package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import studojurata_api.dto.ChamadaRequest;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.exception.RegraNegocioException;
import studojurata_api.exception.RequisicaoInvalidaException;
import studojurata_api.model.Aluno;
import studojurata_api.model.Aula;
import studojurata_api.model.Frequencia;
import studojurata_api.model.Turma;
import studojurata_api.model.enums.StatusAtivoInativo;
import studojurata_api.model.enums.StatusMatricula;
import studojurata_api.repository.AlunoRepository;
import studojurata_api.repository.AlunoTurmaRepository;
import studojurata_api.repository.AulaRepository;
import studojurata_api.repository.FrequenciaRepository;
import studojurata_api.repository.TurmaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FrequenciaService {

    private final FrequenciaRepository repository;
    private final AulaRepository aulaRepository;
    private final AlunoRepository alunoRepository;
    private final AlunoTurmaRepository alunoTurmaRepository;
    private final TurmaRepository turmaRepository;
    private final AlunoTurmaService alunoTurmaService;
    private final studojurata_api.security.PlanejamentoAccessGuard planejamentoAccessGuard;
    private final studojurata_api.security.AlunoAccessGuard alunoAccessGuard;

    public List<Frequencia> listarPorAula(Long aulaId) {
        Aula aula = buscarAula(aulaId);
        planejamentoAccessGuard.garantirLeitura(vinculoId(aula),
                "Você só pode acessar frequência das aulas das suas turmas.");
        return repository.findByAula_Id(aulaId);
    }

    public List<Frequencia> listarPorAluno(Long alunoId) { return repository.findByAluno_IdOrderByAula_DataPrevistaDesc(alunoId); }

    public record ResumoFrequenciaAluno(Long alunoId, double cargaHoraria, long faltas) {}

    /**
     * Carga horária cursada (aulas com presença) e faltas de cada aluno com
     * matrícula ativa, considerando só as disciplinas não inativas da turma —
     * uma disciplina removida da turma não conta mais para o aluno.
     */
    public List<ResumoFrequenciaAluno> resumoPorTurma(Long turmaId) {
        alunoAccessGuard.garantirAcessoATurma(turmaId);
        Map<Long, List<Frequencia>> frequenciasPorAluno = repository.findByAula_PlanoAula_TurmaDisciplina_Turma_Id(turmaId)
                .stream()
                .filter(frequencia -> frequencia.getAula().getPlanoAula().getTurmaDisciplina().getStatus() != StatusAtivoInativo.INATIVO)
                .collect(Collectors.groupingBy(frequencia -> frequencia.getAluno().getId()));

        return alunoTurmaService.ativosPorTurma(turmaId).stream()
                .map(matricula -> {
                    Long alunoId = matricula.getAluno().getId();
                    List<Frequencia> frequencias = frequenciasPorAluno.getOrDefault(alunoId, List.of());
                    double cargaHoraria = frequencias.stream()
                            .filter(Frequencia::getPresente)
                            .mapToDouble(frequencia -> frequencia.getAula().getCargaHoraria() != null
                                    ? frequencia.getAula().getCargaHoraria()
                                    : 0)
                            .sum();
                    long faltas = frequencias.stream().filter(frequencia -> !frequencia.getPresente()).count();
                    return new ResumoFrequenciaAluno(alunoId, cargaHoraria, faltas);
                })
                .toList();
    }

    /** Só aceita alunos com matrícula ATIVA na turma da aula. */
    @Transactional
    public List<Frequencia> registrarChamada(Long aulaId, ChamadaRequest request) {
        if (request == null || request.getAlunos() == null || request.getAlunos().isEmpty()) {
            throw new RequisicaoInvalidaException("Informe ao menos um aluno para realizar a chamada.");
        }

        Aula aula = buscarAula(aulaId);
        planejamentoAccessGuard.garantirEscrita(vinculoId(aula),
                "Você só pode registrar chamada nas aulas das suas turmas.");
        Long turmaId = aula.getPlanoAula().getTurmaDisciplina().getTurma().getId();

        return request.getAlunos().stream()
                .map(item -> registrarPresenca(aula, turmaId, item))
                .toList();
    }

    private Frequencia registrarPresenca(Aula aula, Long turmaId, ChamadaRequest.Item item) {
        if (item.getAlunoId() == null) {
            throw new RequisicaoInvalidaException("alunoId é obrigatório para cada item da chamada.");
        }
        if (item.getPresente() == null) {
            throw new RequisicaoInvalidaException("presente é obrigatório para cada item da chamada.");
        }

        Aluno aluno = alunoRepository.findById(item.getAlunoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aluno não encontrado: " + item.getAlunoId()));

        boolean matriculaAtiva = alunoTurmaRepository.existsByAluno_IdAndTurma_IdAndStatus(
                aluno.getId(), turmaId, StatusMatricula.ATIVA);
        if (!matriculaAtiva) {
            throw new RegraNegocioException(
                    "Aluno " + aluno.getId() + " não possui matrícula ativa na turma desta aula.");
        }

        Frequencia frequencia = repository.findByAluno_IdAndAula_Id(aluno.getId(), aula.getId())
                .orElseGet(Frequencia::new);
        frequencia.setAluno(aluno);
        frequencia.setAula(aula);
        frequencia.setPresente(item.getPresente());
        frequencia.setJustificativa(item.getJustificativa());
        Frequencia salva = repository.save(frequencia);

        if (Boolean.TRUE.equals(item.getPresente())) {
            concluirSeAtingiuCargaHoraria(aluno.getId(), turmaId);
        }

        return salva;
    }

    /**
     * Conclui a matrícula quando a carga horária cursada atinge a do curso.
     * A escola pode reativá-la se decidir por carga horária adicional.
     */
    private void concluirSeAtingiuCargaHoraria(Long alunoId, Long turmaId) {
        Turma turma = turmaRepository.findById(turmaId).orElse(null);
        Integer cargaHorariaTotal = turma != null && turma.getCurso() != null
                ? turma.getCurso().getCargaHorariaTotal()
                : null;
        if (cargaHorariaTotal == null) return;

        double cargaHorariaCursada = repository
                .findByAluno_IdAndAula_PlanoAula_TurmaDisciplina_Turma_IdAndPresenteTrue(alunoId, turmaId)
                .stream()
                .mapToDouble(frequencia -> frequencia.getAula().getCargaHoraria() != null
                        ? frequencia.getAula().getCargaHoraria()
                        : 0)
                .sum();

        if (cargaHorariaCursada < cargaHorariaTotal) return;

        alunoTurmaRepository
                .findFirstByAluno_IdAndTurma_IdAndStatus(alunoId, turmaId, StatusMatricula.ATIVA)
                .ifPresent(matricula -> alunoTurmaService.concluir(matricula.getId(), LocalDate.now()));
    }

    private Aula buscarAula(Long aulaId) {
        return aulaRepository.findById(aulaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aula " + aulaId + " não encontrada."));
    }

    private static Long vinculoId(Aula aula) {
        return aula.getPlanoAula() != null && aula.getPlanoAula().getTurmaDisciplina() != null
                ? aula.getPlanoAula().getTurmaDisciplina().getId()
                : null;
    }
}
