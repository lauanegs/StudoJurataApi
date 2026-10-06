package studojurata_api.service.nota;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import studojurata_api.model.simulado.Simulado;
import studojurata_api.model.simulado.SimuladoAluno;
import studojurata_api.model.turma.TurmaDisciplina;
import studojurata_api.model.enums.StatusSimuladoAluno;
import studojurata_api.model.enums.TipoDestinacaoSimulado;
import studojurata_api.repository.simulado.SimuladoAlunoRepository;
import studojurata_api.repository.turma.TurmaDisciplinaRepository;
import studojurata_api.security.ProfessorAccessGuard;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** Base das telas de Desempenho do professor */
@Service
@RequiredArgsConstructor
public class DesempenhoService {
    /** Simulado sem nota máxima definida é tratado como nota de 0 a 10 */
    private static final double NOTA_MAXIMA_PADRAO = 10;

    private final SimuladoAlunoRepository simuladoAlunoRepository;
    private final TurmaDisciplinaRepository turmaDisciplinaRepository;
    private final ProfessorAccessGuard professorAccessGuard;

    public record TentativaDesempenho(
            Long id,
            Long alunoId,
            Long simuladoId,
            String simuladoTitulo,
            Long turmaId,
            String turma,
            Long disciplinaId,
            String disciplina,
            double nota,
            double notaMaxima,
            /** 0 a 100 */
            double percentual,
            /** dataInicio do simulado, com createdAt como reserva */
            LocalDateTime data,
            TipoDestinacaoSimulado tipoDestinacao) {}

    @Transactional(readOnly = true)
    public List<TentativaDesempenho> tentativasDoProfessor(Long professorId) {
        // Antes de qualquer consulta: quem não é o próprio professor
        professorAccessGuard.garantir(professorId);

        Set<Long> turmaIds = turmaDisciplinaRepository.findByProfessorId(professorId).stream()
                .map(TurmaDisciplina::getTurma)
                .filter(Objects::nonNull)
                .map(turma -> turma.getId())
                .collect(Collectors.toSet());

        if (turmaIds.isEmpty()) return List.of();

        return simuladoAlunoRepository
                .findByStatusAndNotaIsNotNullAndSimulado_Turma_IdIn(StatusSimuladoAluno.CONCLUIDO, turmaIds)
                .stream()
                .map(this::paraTentativaDesempenho)
                .toList();
    }

    private TentativaDesempenho paraTentativaDesempenho(SimuladoAluno tentativa) {
        Simulado simulado = tentativa.getSimulado();
        double notaMaxima = simulado.getNotaMaxima() == null || simulado.getNotaMaxima() == 0
                ? NOTA_MAXIMA_PADRAO
                : simulado.getNotaMaxima();

        return new TentativaDesempenho(
                tentativa.getId(),
                tentativa.getAluno().getId(),
                simulado.getId(),
                simulado.getTitulo(),
                simulado.getTurma() != null ? simulado.getTurma().getId() : null,
                simulado.getTurma() != null ? simulado.getTurma().getTitulo() : null,
                simulado.getDisciplina() != null ? simulado.getDisciplina().getId() : null,
                simulado.getDisciplina() != null ? simulado.getDisciplina().getTitulo() : null,
                tentativa.getNota(),
                notaMaxima,
                Math.min(100, tentativa.getNota() / notaMaxima * 100),
                simulado.getDataInicio() != null ? simulado.getDataInicio() : simulado.getCreatedAt(),
                simulado.getTipoDestinacao());
    }
}
