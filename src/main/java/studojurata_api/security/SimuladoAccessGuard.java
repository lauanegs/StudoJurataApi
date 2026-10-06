package studojurata_api.security;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;

import studojurata_api.exception.RegraNegocioException;
import studojurata_api.exception.RequisicaoInvalidaException;
import studojurata_api.model.planejamento.PlanoEnsino;
import studojurata_api.model.simulado.Questao;
import studojurata_api.model.simulado.Simulado;
import studojurata_api.model.turma.TurmaDisciplina;
import studojurata_api.model.enums.StatusAtivoInativo;
import studojurata_api.model.enums.TipoUsuario;

/** Guarda de escrita de simulado: quem pode criar, editar, lançar, encerrar, estender a disponibilidade */
@Component
@RequiredArgsConstructor
public class SimuladoAccessGuard {
    private final UsuarioAutenticado usuarioAutenticado;
    private final EscopoProfessor escopoProfessor;

    /** 403 para quem não é professor nem administrador */
    public void garantirPerfilDeEscrita() {
        var usuario = usuarioAutenticado.atual();
        if (usuario.getTipoUsuario() != TipoUsuario.PROFESSOR
                && usuario.getTipoUsuario() != TipoUsuario.ADMINISTRADOR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Apenas professor ou administrador pode alterar simulados.");
        }
    }

    /** 403 quando o simulado não está no escopo do professor logado */
    public void garantirEscrita(Simulado alvo) {
        garantirEscrita(alvo, null);
    }

    /** Leitura de simulado sem turma (órfão) */
    public void garantirLeituraDeOrfao(Simulado alvo) {
        if (alvo == null || alvo.getTurma() != null) {
            return;
        }
        if (!usuarioAutenticado.ehAdministrador()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Simulado sem turma só pode ser consultado pelo administrador.");
        }
    }

    /** Mesma validação para o simulado atual e para os valores de uma edição, resolvendo o escopo uma única vez */
    public void garantirEscrita(Simulado alvo, Simulado novosValores) {
        if (deixarPassarAdministrador()) {
            return;
        }

        Long professorId = usuarioAutenticado.professorId();
        List<Simulado> alvos = alvosDe(alvo, novosValores);

        // Uma fase por dimensão, e cada consulta de escopo só acontece depois do teste de presença da dimensão
        garantirTurmas(alvos, professorId);
        garantirDisciplinas(alvos, professorId);
        garantirPlanos(alvos, professorId);
    }

    /** Simulado de professor exige plano de ensino compatível com a turma e a disciplina escolhidas */
    public void garantirPlanoCompativel(Simulado alvo) {
        if (usuarioAutenticado.ehAdministrador()) {
            return;
        }
        garantirPerfilDeEscrita();

        if (alvo == null || alvo.getPlanoEnsino() == null || alvo.getPlanoEnsino().getId() == null) {
            throw new RequisicaoInvalidaException(
                    "Plano de ensino é obrigatório: é ele que liga o simulado ao conteúdo da turma.");
        }

        TurmaDisciplina vinculo = alvo.getPlanoEnsino().getTurmaDisciplina();
        if (vinculo == null) {
            throw new RegraNegocioException(
                    "Plano de ensino genérico não pode ser usado em simulado de professor.");
        }

        Long turmaDoPlano = vinculo.getTurma() != null ? vinculo.getTurma().getId() : null;
        Long disciplinaDoPlano = vinculo.getDisciplina() != null ? vinculo.getDisciplina().getId() : null;
        Long turmaDoSimulado = alvo.getTurma() != null ? alvo.getTurma().getId() : null;
        Long disciplinaDoSimulado = alvo.getDisciplina() != null ? alvo.getDisciplina().getId() : null;

        if (turmaDoSimulado == null || disciplinaDoSimulado == null
                || !turmaDoSimulado.equals(turmaDoPlano) || !disciplinaDoSimulado.equals(disciplinaDoPlano)) {
            throw new RegraNegocioException(
                    "O plano de ensino selecionado não pertence à turma e à disciplina deste simulado.");
        }
    }

    /** Disciplina inativada não entra em simulado novo nem em edição */
    public void garantirDisciplinaAtiva(Simulado alvo) {
        if (usuarioAutenticado.ehAdministrador()) {
            return;
        }
        garantirPerfilDeEscrita();
        if (alvo == null || alvo.getDisciplina() == null) {
            return; // ausência de disciplina é tratada em garantirDisciplinas
        }
        if (alvo.getDisciplina().getStatus() == StatusAtivoInativo.INATIVO) {
            throw new RegraNegocioException(
                    "A disciplina do simulado está inativa e não pode receber novos simulados.");
        }
    }

    /** Vínculo simulado × questão: o simulado passa pelas mesmas dimensões (turma, disciplina e plano) */
    public void garantirEscritaDoVinculo(Simulado simulado, Questao questao) {
        if (deixarPassarAdministrador() || simulado == null) {
            return;
        }

        Long professorId = usuarioAutenticado.professorId();
        List<Simulado> alvos = alvosDe(simulado, null);

        garantirTurmas(alvos, professorId);
        // Com um alvo, a fase de disciplina ou lança 403 ou devolve o conjunto resolvido
        Set<Long> disciplinas = garantirDisciplinas(alvos, professorId);
        garantirPlanos(alvos, professorId);
        garantirDisciplinaDaQuestao(questao, disciplinas);
    }

    /** ADMIN passa sem consultar escopo; demais perfis já foram barrados antes */
    private boolean deixarPassarAdministrador() {
        garantirPerfilDeEscrita();
        return usuarioAutenticado.ehAdministrador();
    }

    private void garantirTurmas(List<Simulado> alvos, Long professorId) {
        Set<Long> turmas = null;

        for (Simulado alvo : alvos) {
            Long turmaId = alvo.getTurma() != null ? alvo.getTurma().getId() : null;
            if (turmaId == null) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Você só pode alterar simulados das turmas que leciona.");
            }
            if (turmas == null) {
                turmas = escopoProfessor.turmaIdsDoProfessor(professorId);
            }
            if (!turmas.contains(turmaId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Você só pode alterar simulados das turmas que leciona.");
            }
        }
    }

    /** Devolve o conjunto resolvido para quem também precisa validar a questão */
    private Set<Long> garantirDisciplinas(List<Simulado> alvos, Long professorId) {
        Set<Long> disciplinas = null;

        for (Simulado alvo : alvos) {
            Long disciplinaId = alvo.getDisciplina() != null ? alvo.getDisciplina().getId() : null;
            if (disciplinaId == null) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Você só pode alterar simulados das disciplinas que leciona.");
            }
            if (disciplinas == null) {
                disciplinas = escopoProfessor.disciplinaIdsDoProfessor(professorId);
            }
            if (!disciplinas.contains(disciplinaId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Você só pode alterar simulados das disciplinas que leciona.");
            }
        }

        return disciplinas;
    }

    private void garantirDisciplinaDaQuestao(Questao questao, Set<Long> disciplinas) {
        Long disciplinaId = questao != null && questao.getDisciplina() != null
                ? questao.getDisciplina().getId()
                : null;
        if (disciplinaId == null || !disciplinas.contains(disciplinaId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Você só pode vincular questões das disciplinas que leciona.");
        }
    }

    /** Plano de ensino do simulado precisa ser um dos vínculos do professor */
    private void garantirPlanos(List<Simulado> alvos, Long professorId) {
        Set<Long> vinculos = null;

        for (Simulado alvo : alvos) {
            PlanoEnsino plano = alvo.getPlanoEnsino();
            if (plano == null) {
                continue;
            }
            Long vinculoId = plano.getTurmaDisciplina() != null ? plano.getTurmaDisciplina().getId() : null;
            if (vinculoId == null) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "O plano de ensino deste simulado não pertence às turmas/disciplinas que você leciona.");
            }
            if (vinculos == null) {
                vinculos = escopoProfessor.turmaDisciplinaIdsDoProfessor(professorId);
            }
            if (!vinculos.contains(vinculoId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "O plano de ensino deste simulado não pertence às turmas/disciplinas que você leciona.");
            }
        }
    }

    private static List<Simulado> alvosDe(Simulado alvo, Simulado novosValores) {
        List<Simulado> alvos = new ArrayList<>(2);
        if (alvo != null) {
            alvos.add(alvo);
        }
        if (novosValores != null) {
            alvos.add(novosValores);
        }
        return alvos;
    }
}
