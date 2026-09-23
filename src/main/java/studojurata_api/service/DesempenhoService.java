package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import studojurata_api.model.Simulado;
import studojurata_api.model.SimuladoAluno;
import studojurata_api.model.TurmaDisciplina;
import studojurata_api.model.enums.StatusSimuladoAluno;
import studojurata_api.model.enums.TipoDestinacaoSimulado;
import studojurata_api.repository.SimuladoAlunoRepository;
import studojurata_api.repository.TurmaDisciplinaRepository;
import studojurata_api.security.ProfessorAccessGuard;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Base das telas de Desempenho do professor: as tentativas concluídas dos
 * simulados das turmas em que ele leciona, já com os dados do simulado. As
 * agregações (por simulado, disciplina e ao longo do tempo) seguem no front
 * porque dependem dos filtros aplicados na tela.
 *
 * <p>A consulta é escopada pelo {@link ProfessorAccessGuard}: o id do professor
 * chega pela URL, então sem o guarda um professor leria o desempenho das turmas
 * de outro (o administrador continua passando por qualquer id).
 */
@Service
@RequiredArgsConstructor
public class DesempenhoService {

    /** Simulado sem nota máxima definida é tratado como nota de 0 a 10. */
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
            /** 0 a 100. */
            double percentual,
            /** dataInicio do simulado, com createdAt como reserva. */
            LocalDateTime data,
            TipoDestinacaoSimulado tipoDestinacao) {}

    @Transactional(readOnly = true)
    public List<TentativaDesempenho> tentativasDoProfessor(Long professorId) {
        // Antes de qualquer consulta: quem não é o próprio professor (nem
        // administrador) recebe 403 sem tocar nos repositórios.
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
