package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.exception.RegraNegocioException;
import studojurata_api.exception.RequisicaoInvalidaException;
import studojurata_api.model.AlunoTurma;
import studojurata_api.model.Curso;
import studojurata_api.model.Turma;
import studojurata_api.model.Usuario;
import studojurata_api.model.enums.StatusAtivoInativo;
import studojurata_api.model.enums.StatusTurma;
import studojurata_api.model.enums.TipoUsuario;
import studojurata_api.repository.AlunoTurmaRepository;
import studojurata_api.repository.CursoRepository;
import studojurata_api.repository.TurmaRepository;
import studojurata_api.security.EscolaContext;
import studojurata_api.security.EscopoProfessor;
import studojurata_api.security.AlunoAccessGuard;
import studojurata_api.security.PlanejamentoAccessGuard;
import studojurata_api.security.UsuarioAutenticado;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * validarCurso resolve o curso pelo id e recusa curso de outra escola (403)
 * ou inativo (409).
 *
 * <p>Escrita: o escopo de um professor vem do vinculo {@code TurmaDisciplina}
 * (a mesma fonte do {@code EscopoProfessor}), entao atualizar, inativar e ativar
 * exigem a turma nesse escopo — validado antes de ler ou gravar qualquer dado da
 * turma. Criar turma e ato do administrador: a turma nova ainda nao tem vinculo
 * de onde tirar a posse.
 */
@Service
@RequiredArgsConstructor
public class TurmaService {

    private final TurmaRepository repository;
    private final AlunoTurmaRepository alunoTurmaRepository;
    private final AlunoTurmaService alunoTurmaService;
    private final CursoRepository cursoRepository;
    private final EscolaContext escolaContext;
    private final EscopoProfessor escopoProfessor;
    private final UsuarioAutenticado usuarioAutenticado;
    private final AlunoAccessGuard alunoAccessGuard;
    private final PlanejamentoAccessGuard planejamentoAccessGuard;

    /**
     * Listagem escopada: ADMINISTRADOR mantém a visão da escola (sem escola
     * resolvível, antes do cadastro inicial, não filtra); PROFESSOR vê as turmas
     * em que leciona; ALUNO vê as turmas em que está matriculado.
     */
    public List<Turma> listar() {
        Usuario usuario = usuarioAutenticado.atual();

        if (usuario.getTipoUsuario() == TipoUsuario.ADMINISTRADOR) {
            Long escolaId = escolaContext.escolaAtualId();
            return escolaId != null ? repository.findByEscola_Id(escolaId) : repository.findAll();
        }

        if (usuario.getTipoUsuario() == TipoUsuario.PROFESSOR) {
            return usuario.getProfessor() == null ? List.of()
                    : turmasPorIds(escopoProfessor.turmaIdsDoProfessor(usuario.getProfessor().getId()));
        }

        return turmasPorIds(turmasDoAluno(usuario));
    }

    private Set<Long> turmasDoAluno(Usuario usuario) {
        if (usuario.getAluno() == null || usuario.getAluno().getId() == null) {
            return Set.of();
        }
        return alunoTurmaRepository.findByAluno_Id(usuario.getAluno().getId()).stream()
                .map(AlunoTurma::getTurma)
                .filter(Objects::nonNull)
                .map(Turma::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /** Escopo vazio não consulta o repositório. */
    private List<Turma> turmasPorIds(Set<Long> turmaIds) {
        return turmaIds.isEmpty() ? List.of() : repository.findAllById(turmaIds);
    }

    public Turma buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Turma " + id + " não encontrada."));
    }

    /**
     * Leitura individual (GET /turmas/{id}) com a <b>mesma visibilidade da
     * listagem</b>: ADMIN vê todas (dentro da escola já resolvida), PROFESSOR as
     * turmas em que leciona e ALUNO as turmas em que está matriculado. Sem isso,
     * qualquer autenticado leria por id os dados de uma turma alheia.
     *
     * <p>O uso interno continua em {@link #buscar}: as operações de escrita têm
     * seu próprio caminho e não dependem deste recorte.
     */
    public Turma buscarParaLeitura(Long id) {
        Turma turma = buscar(id);

        Usuario usuario = usuarioAutenticado.atual();

        if (usuario.getTipoUsuario() == TipoUsuario.ADMINISTRADOR) {
            return turma;
        }

        if (usuario.getTipoUsuario() == TipoUsuario.PROFESSOR) {
            Long professorId = usuario.getProfessor() != null ? usuario.getProfessor().getId() : null;
            if (professorId != null && escopoProfessor.turmaIdsDoProfessor(professorId).contains(turma.getId())) {
                return turma;
            }
        } else if (usuario.getTipoUsuario() == TipoUsuario.ALUNO && turmasDoAluno(usuario).contains(turma.getId())) {
            return turma;
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você só pode acessar as suas turmas.");
    }

    public Turma salvar(Turma obj) {
        exigirAdministradorParaCriarTurma();
        validarCapacidadeMaxima(obj);
        validarPeriodo(obj);
        validarCurso(obj);
        if (obj.getStatus() == null) obj.setStatus(StatusTurma.ATIVA);
        return repository.save(obj);
    }

    public Turma atualizar(Long id, Turma obj) {
        // Autorizacao antes de qualquer leitura/alteracao: o professor so altera as
        // turmas em que leciona (o vinculo turma-disciplina e a fonte do escopo).
        planejamentoAccessGuard.garantirEscritaNaTurma(id,
                "Você só pode alterar as suas turmas.");
        Turma existente = buscar(id);
        validarCapacidadeMaxima(obj);
        validarPeriodo(obj);
        validarCurso(obj);
        obj.setId(id);
        // A escola da turma nao muda pelo corpo da requisicao (mesma regra do
        // CursoService atualizar): o corpo nao escolhe o tenant.
        obj.setEscola(existente.getEscola());

        if (obj.getCapacidadeMaxima() != null) {
            long ativos = alunoTurmaService.contarAtivosPorTurma(id);
            if (ativos > obj.getCapacidadeMaxima()) {
                throw new RegraNegocioException(
                        "Não é possível reduzir a capacidade máxima para " + obj.getCapacidadeMaxima()
                                + ": a turma já possui " + ativos + " aluno(s) com matrícula ativa.");
            }
        }

        return repository.save(obj);
    }

    public long contarAlunosAtivos(Long turmaId) {
        // Contagem de alunos da turma é dado de gestão: dono da turma ou admin.
        alunoAccessGuard.garantirAcessoATurma(turmaId);
        return alunoTurmaService.contarAtivosPorTurma(turmaId);
    }

    /**
     * Recusa turma com qualquer matrícula: a exclusão serve só para turma
     * criada por engano; para encerrar, a turma é inativada.
     */
    public void deletar(Long id) {
        // Escopo antes da consulta: turma de outro professor/escola nem revela se existe.
        planejamentoAccessGuard.garantirEscritaNaTurma(id,
                "Você só pode inativar as suas turmas.");
        Turma turma = buscar(id);
        if (alunoTurmaRepository.existsByTurma_Id(id)) {
            throw new RegraNegocioException(
                    "Esta turma já teve aluno(s) matriculado(s) e não pode ser inativada por aqui — "
                            + "use a Situação em \"Dados da turma\" para inativar preservando o histórico.");
        }
        turma.setStatus(StatusTurma.INATIVA);
        repository.save(turma);
    }

    public Turma ativar(Long id) {
        planejamentoAccessGuard.garantirEscritaNaTurma(id,
                "Você só pode ativar as suas turmas.");
        Turma turma = buscar(id);
        turma.setStatus(StatusTurma.ATIVA);
        return repository.save(turma);
    }

    /**
     * Criar turma e ato de gestao: o escopo do professor nasce do vinculo
     * turma-disciplina, e a turma nova ainda nao tem vinculo nenhum. Sem vinculo
     * nao ha como provar posse — nem o proprio professor conseguiria criar o
     * vinculo depois, porque {@code garantirEscritaNaTurma} ja exigiria a posse da
     * turma. As demais operacoes seguem permitidas ao professor no proprio escopo.
     */
    private void exigirAdministradorParaCriarTurma() {
        if (!usuarioAutenticado.ehAdministrador()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Apenas administrador pode criar turmas.");
        }
    }

    /** Capacidade é obrigatória desde a criação: é ela que avisa quando a turma lota. */
    private void validarCapacidadeMaxima(Turma obj) {
        if (obj.getCapacidadeMaxima() == null) {
            throw new RequisicaoInvalidaException("Capacidade máxima é obrigatória.");
        }
        if (obj.getCapacidadeMaxima() <= 0) {
            throw new RequisicaoInvalidaException("Capacidade máxima deve ser maior que zero.");
        }
    }

    /** Data de início obrigatória, e a de término (quando houver) nunca anterior a ela. */
    private void validarPeriodo(Turma obj) {
        if (obj.getDataInicio() == null) {
            throw new RequisicaoInvalidaException("Data de início é obrigatória.");
        }
        if (obj.getDataFim() != null && obj.getDataFim().isBefore(obj.getDataInicio())) {
            throw new RequisicaoInvalidaException("A data de término não pode ser anterior à data de início.");
        }
    }

    private void validarCurso(Turma obj) {
        if (obj.getCurso() == null || obj.getCurso().getId() == null) {
            throw new RequisicaoInvalidaException(
                    "Curso é obrigatório: o aluno matriculado na turma sempre segue o curso vinculado a ela.");
        }
        Curso curso = cursoRepository.findById(obj.getCurso().getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Curso " + obj.getCurso().getId() + " não encontrado."));

        if (curso.getStatus() == StatusAtivoInativo.INATIVO) {
            throw new RegraNegocioException(
                    "O curso \"" + curso.getNome() + "\" está inativo e não pode receber novas turmas.");
        }

        // Sem isso, qualquer curso de outra escola poderia ser vinculado só pelo id.
        Long escolaId = escolaContext.escolaAtualId();
        if (escolaId != null && curso.getEscola() != null && !escolaId.equals(curso.getEscola().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Este curso pertence a outra escola e não pode ser vinculado a esta turma.");
        }

        obj.setCurso(curso);
    }
}
