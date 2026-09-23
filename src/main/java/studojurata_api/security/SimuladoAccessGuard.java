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
import studojurata_api.model.PlanoEnsino;
import studojurata_api.model.Questao;
import studojurata_api.model.Simulado;
import studojurata_api.model.TurmaDisciplina;
import studojurata_api.model.enums.StatusAtivoInativo;
import studojurata_api.model.enums.TipoUsuario;

/**
 * Guarda de escrita de simulado: quem pode criar, editar, lançar, encerrar,
 * estender a disponibilidade e vincular questão em um simulado.
 *
 * <p>O simulado é lido por vários perfis (o aluno enxerga os que têm tentativa,
 * o professor enxerga os das suas turmas e D-C1 mantém os órfãos visíveis), mas a
 * escrita tem dono. Para o professor o escopo precisa ser <b>verificável</b>:
 * turma e disciplina nos vínculos dele e, quando houver plano de ensino, um
 * plano de vínculo dele. Simulado sem turma, sem disciplina ou com plano
 * genérico fica restrito ao administrador — melhor recusar a escrita do que
 * atribuir posse por suposição.
 *
 * <p>Existe para que {@link studojurata_api.service.SimuladoService} e
 * {@link studojurata_api.service.SimuladoQuestaoService} compartilhem a mesma
 * regra em vez de manter cada um a sua cópia.
 *
 * <p>O par turma × disciplina do simulado precisa ser uma oferta do professor —
 * não basta cada ponta isolada. Essa garantia vem de
 * {@link #garantirPlanoCompativel(Simulado)}: o plano de ensino exigido no
 * simulado de professor pertence a um vínculo turma-disciplina dele e precisa
 * apontar a mesma turma e a mesma disciplina do simulado, então não existe
 * simulado apontando par fora dos vínculos de quem cria.
 *
 * <p>Responsabilidade: decidir a escrita e falhar fechado com 403. O papel é
 * declarado no {@code SecurityConfig}; aqui a decisão é de escopo.
 */
@Component
@RequiredArgsConstructor
public class SimuladoAccessGuard {

    private final UsuarioAutenticado usuarioAutenticado;
    private final EscopoProfessor escopoProfessor;

    /**
     * 403 para quem não é professor nem administrador. Separado do escopo porque
     * quem carrega o simulado por id pode chamar isto antes da consulta e negar o
     * perfil sem tocar no banco.
     */
    public void garantirPerfilDeEscrita() {
        var usuario = usuarioAutenticado.atual();
        if (usuario.getTipoUsuario() != TipoUsuario.PROFESSOR
                && usuario.getTipoUsuario() != TipoUsuario.ADMINISTRADOR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Apenas professor ou administrador pode alterar simulados.");
        }
    }

    /** 403 quando o simulado não está no escopo do professor logado. */
    public void garantirEscrita(Simulado alvo) {
        garantirEscrita(alvo, null);
    }

    /**
     * Leitura de simulado <b>sem turma</b> (órfão): sem vínculo verificável não
     * há como atribuir posse, então só o administrador consulta — a mesma
     * cautela aplicada à escrita. Simulado com turma passa direto: quem decide
     * ali é o escopo da turma.
     *
     * <p>É o par de leitura de {@code SimuladoService.simuladoIdsVisiveis()},
     * onde órfão também fica fora do alcance de professor e aluno.
     */
    public void garantirLeituraDeOrfao(Simulado alvo) {
        if (alvo == null || alvo.getTurma() != null) {
            return;
        }
        if (!usuarioAutenticado.ehAdministrador()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Simulado sem turma só pode ser consultado pelo administrador.");
        }
    }

    /**
     * Mesma validação para o simulado atual e para os valores de uma edição,
     * resolvendo o escopo uma única vez: a edição precisa dos dois, e repetir a
     * resolução só duplicaria as consultas.
     */
    public void garantirEscrita(Simulado alvo, Simulado novosValores) {
        if (deixarPassarAdministrador()) {
            return;
        }

        Long professorId = usuarioAutenticado.professorId();
        List<Simulado> alvos = alvosDe(alvo, novosValores);

        // Uma fase por dimensão, e cada consulta de escopo só acontece depois do
        // teste de presença da dimensão — simulado órfão e simulado sem disciplina
        // são recusados sem consultar nada. Nenhuma consulta se repete por causa
        // do segundo alvo (o caso da edição).
        garantirTurmas(alvos, professorId);
        garantirDisciplinas(alvos, professorId);
        garantirPlanos(alvos, professorId);
    }

    /**
     * Simulado de professor exige plano de ensino <b>compatível</b> com a turma e
     * a disciplina escolhidas: o plano é o que liga o simulado ao conteúdo da
     * turma (e à geração por IA). Plano genérico ou de outro vínculo não serve.
     *
     * <p>Vale só na criação e na edição — lançar, encerrar e estender
     * disponibilidade não revisitam esta regra, para não travar simulado que já
     * existe. O administrador mantém a liberdade do fluxo administrativo
     * (simulado órfão e sem plano continuam possíveis para ele).
     */
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

    /**
     * Disciplina inativada não entra em simulado novo nem em edição: ela sai dos
     * vínculos e da grade, e o simulado ficaria sem oferta. Vale só na criação e
     * na edição — lançar, encerrar e estender disponibilidade não revisitam a
     * regra, para não travar simulado que já existe. O ADMIN mantém a liberdade
     * do fluxo administrativo.
     */
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

    /**
     * Vínculo simulado × questão: o simulado passa pelas mesmas dimensões
     * (turma, disciplina e plano) e a questão precisa ter disciplina que o
     * professor lecione. As duas pontas compartilham a resolução de escopo. Sem
     * simulado não há escopo de simulado a validar — a obrigatoriedade é regra
     * estrutural do service.
     */
    public void garantirEscritaDoVinculo(Simulado simulado, Questao questao) {
        if (deixarPassarAdministrador() || simulado == null) {
            return;
        }

        Long professorId = usuarioAutenticado.professorId();
        List<Simulado> alvos = alvosDe(simulado, null);

        garantirTurmas(alvos, professorId);
        // Com um alvo, a fase de disciplina ou lança 403 ou devolve o conjunto resolvido.
        Set<Long> disciplinas = garantirDisciplinas(alvos, professorId);
        garantirPlanos(alvos, professorId);
        garantirDisciplinaDaQuestao(questao, disciplinas);
    }

    /** ADMIN passa sem consultar escopo; demais perfis já foram barrados antes. */
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

    /** Devolve o conjunto resolvido para quem também precisa validar a questão. */
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

    /**
     * Plano de ensino do simulado precisa ser um dos vínculos do professor.
     * Plano genérico (sem turma/disciplina) não tem escopo verificável, então ele
     * recusa — a consistência entre o par turma/disciplina do plano e o do
     * simulado é responsabilidade de quem cadastra (ver relatório do C2.7d).
     */
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
