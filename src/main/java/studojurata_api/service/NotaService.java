package studojurata_api.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import studojurata_api.model.enums.TipoUsuario;
import studojurata_api.security.EscopoProfessor;
import studojurata_api.security.PerfilDeGestao;
import studojurata_api.security.UsuarioAutenticado;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.model.AlunoTurma;
import studojurata_api.model.Aluno;
import studojurata_api.model.Disciplina;
import studojurata_api.model.Nota;
import studojurata_api.model.SimuladoAluno;
import studojurata_api.model.Turma;
import studojurata_api.model.enums.AcaoAuditoria;
import studojurata_api.model.enums.StatusSimuladoAluno;
import studojurata_api.repository.AlunoRepository;
import studojurata_api.repository.AlunoTurmaRepository;
import studojurata_api.repository.DisciplinaRepository;
import studojurata_api.repository.NotaRepository;
import studojurata_api.repository.SimuladoAlunoRepository;
import studojurata_api.repository.TurmaRepository;

import java.util.List;

/**
 * A nota é sempre derivada dos simulados concluídos (regra de cálculo em
 * Nota). Toda alteração é registrada em AuditLog.
 */
@Service
@RequiredArgsConstructor
public class NotaService {

    private final NotaRepository repository;
    private final AlunoRepository alunoRepository;
    private final DisciplinaRepository disciplinaRepository;
    private final TurmaRepository turmaRepository;
    private final AlunoTurmaRepository alunoTurmaRepository;
    private final SimuladoAlunoRepository simuladoAlunoRepository;
    private final AuditLogService auditLogService;
    private final UsuarioAutenticado usuarioAutenticado;
    private final EscopoProfessor escopoProfessor;

    /** 403 de perfil — mensagem do domínio de nota, preservada. */
    private static final String MENSAGEM_PERFIL_DE_GESTAO =
            "Apenas professor ou administrador pode listar notas.";

    /**
     * Listagem escopada:
     * <ul>
     *   <li>ADMINISTRADOR: todas as notas (comportamento atual);</li>
     *   <li>PROFESSOR: notas das suas turmas, mais as notas sem turma dos alunos
     *       que estao no escopo dessas turmas;</li>
     *   <li>ALUNO e demais perfis: 403 — o aluno consulta as proprias notas por
     *       {@code /notas/aluno/{id}/historico};</li>
     *   <li>sem autenticacao: 403.</li>
     * </ul>
     *
     * <p>Consulta em lote (4 consultas fixas) e chave por id, para nao repetir
     * nota que apareca nas duas origens.
     */
    public List<Nota> listar() {
        var usuario = usuarioAutenticado.atual();

        if (usuario.getTipoUsuario() == TipoUsuario.ADMINISTRADOR) {
            return repository.findAll();
        }
        PerfilDeGestao.exigir(usuario, MENSAGEM_PERFIL_DE_GESTAO);

        Long professorId = usuario.getProfessor() != null ? usuario.getProfessor().getId() : null;
        Set<Long> turmaIds = escopoProfessor.turmaIdsDoProfessor(professorId);
        if (turmaIds.isEmpty()) {
            return List.of();
        }

        Map<Long, Nota> porId = new LinkedHashMap<>();
        repository.findByTurma_IdIn(turmaIds).forEach(nota -> porId.putIfAbsent(nota.getId(), nota));

        Set<Long> alunoIds = alunoTurmaRepository.findByTurma_IdIn(turmaIds).stream()
                .map(AlunoTurma::getAluno)
                .filter(Objects::nonNull)
                .map(Aluno::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (!alunoIds.isEmpty()) {
            repository.findByTurmaIsNullAndAluno_IdIn(alunoIds)
                    .forEach(nota -> porId.putIfAbsent(nota.getId(), nota));
        }

        return new ArrayList<>(porId.values());
    }

    public List<Nota> historicoPorAluno(Long alunoId) {
        return repository.findByAluno_IdOrderByCreatedAtDesc(alunoId);
    }

    /** Chamado a cada simulado finalizado e, manualmente, para reprocessamento. */
    @Transactional
    public Nota recalcular(Long alunoId, Long disciplinaId, Long turmaId) {
        AlunoTurma matricula = alunoTurmaRepository
                .findFirstByAluno_IdAndTurma_IdOrderByDataInicioDesc(alunoId, turmaId)
                .orElse(null);

        List<SimuladoAluno> concluidos = simuladoAlunoRepository
                .findByAluno_IdAndStatusAndSimulado_Disciplina_IdAndSimulado_Turma_Id(
                        alunoId, StatusSimuladoAluno.CONCLUIDO, disciplinaId, turmaId)
                .stream()
                .filter(sa -> sa.getSimulado().getNotaMaxima() != null && sa.getSimulado().getNotaMaxima() > 0)
                .filter(sa -> elegivelPelaMatricula(sa, matricula))
                .toList();

        double total = concluidos.stream()
                .filter(sa -> sa.getNota() != null)
                .mapToDouble(SimuladoAluno::getNota)
                .sum();

        Nota nota = repository.findByAluno_IdAndDisciplina_IdAndTurma_Id(alunoId, disciplinaId, turmaId)
                .orElseGet(() -> {
                    Nota nova = new Nota();
                    Aluno aluno = alunoRepository.findById(alunoId)
                            .orElseThrow(() -> new RecursoNaoEncontradoException("Aluno não encontrado."));
                    Disciplina disciplina = disciplinaRepository.findById(disciplinaId)
                            .orElseThrow(() -> new RecursoNaoEncontradoException("Disciplina não encontrada."));
                    Turma turma = turmaRepository.findById(turmaId)
                            .orElseThrow(() -> new RecursoNaoEncontradoException("Turma não encontrada."));
                    nova.setAluno(aluno);
                    nova.setDisciplina(disciplina);
                    nova.setTurma(turma);
                    return nova;
                });

        Double totalAnterior = nota.getTotal();
        nota.setTotal(concluidos.isEmpty() ? null : total);
        nota.setQuantidadeSimuladosConsiderados(concluidos.size());
        Nota salva = repository.save(nota);

        auditLogService.registrar("Nota", salva.getId(),
                totalAnterior == null ? AcaoAuditoria.CRIACAO : AcaoAuditoria.ATUALIZACAO,
                "total: " + totalAnterior + " -> " + salva.getTotal()
                        + " (turma " + turmaId + ", " + concluidos.size() + " simulado(s) concluído(s))");

        return salva;
    }

    /**
     * Sem matrícula encontrada, conta o simulado: um dado de matrícula
     * inconsistente não deve zerar a nota do aluno.
     */
    private boolean elegivelPelaMatricula(SimuladoAluno simuladoAluno, AlunoTurma matricula) {
        if (matricula == null || matricula.getDataInicio() == null) return true;
        var dataAplicacao = simuladoAluno.getSimulado().getDataInicio();
        if (dataAplicacao == null) return true;
        return !dataAplicacao.toLocalDate().isBefore(matricula.getDataInicio());
    }
}
