package studojurata_api.security;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import studojurata_api.model.AlunoTurma;
import studojurata_api.model.TurmaDisciplina;
import studojurata_api.model.enums.StatusMatricula;
import studojurata_api.repository.AlunoTurmaRepository;
import studojurata_api.repository.TurmaDisciplinaRepository;

/**
 * Resolve o escopo de leitura de um professor: <b>quais ids ele pode
 * enxergar</b>. O escopo de um professor nasce do vínculo
 * {@code TurmaDisciplina} em que ele é o titular — a mesma origem que o
 * {@code ProfessorService.turmasLecionadas} usa.
 *
 * <p>Responsabilidade única, e por isso este componente <b>não</b>:
 * <ul>
 *   <li>decide autorização — quem decide é o {@link ProfessorAccessGuard};</li>
 *   <li>monta consultas JPA novas — usa apenas métodos que já existem nos
 *       repositórios;</li>
 *   <li>devolve sentinela: nenhum método significa "tudo". Um conjunto
 *       <b>vazio</b> quer dizer "sem escopo", e o caso irrestrito do
 *       administrador é decidido por quem chama, via
 *       {@link ProfessorAccessGuard#ehAdministrador()}.</li>
 * </ul>
 *
 * <p>Os vínculos <b>não</b> são filtrados por status: manter os inativos é o
 * que preserva o acesso do professor ao histórico das turmas em que lecionou.
 * Coincide com o comportamento atual do frontend, que monta o recorte a partir
 * de {@code turmasLecionadas} (sem filtro de status). Restringir a leitura nova
 * a vínculos ATIVOS é decisão de cada caso de uso, no bloco em que os guardas
 * forem aplicados.
 */
@Component
@RequiredArgsConstructor
public class EscopoProfessor {

    private final TurmaDisciplinaRepository turmaDisciplinaRepository;
    private final AlunoTurmaRepository alunoTurmaRepository;

    /** Ids das turmas em que o professor é titular. */
    public Set<Long> turmaIdsDoProfessor(Long professorId) {
        return turmaIdsDosVinculos(vinculosDoProfessor(professorId));
    }

    /** Ids dos vínculos turma+disciplina do professor. */
    public Set<Long> turmaDisciplinaIdsDoProfessor(Long professorId) {
        Set<Long> vinculoIds = new LinkedHashSet<>();
        for (TurmaDisciplina vinculo : vinculosDoProfessor(professorId)) {
            if (vinculo.getId() != null) {
                vinculoIds.add(vinculo.getId());
            }
        }
        return vinculoIds;
    }

    /**
     * O professor leciona para este aluno?
     *
     * <p>Custo <b>fixo de duas consultas</b> — as turmas do professor e uma
     * verificação de pertencimento com {@code IN} — em vez de materializar a
     * lista de todos os alunos das turmas dele ({@link #alunoIdsDoProfessor}),
     * que seria 1+N em endpoint individual.
     *
     * <p>Não filtra o status da matrícula: o professor consulta notas de alunos
     * com matrícula concluída no modo histórico da tela de Notas, e essa leitura
     * segue legítima.
     */
    public boolean lecionaPara(Long professorId, Long alunoId) {
        if (professorId == null || alunoId == null) {
            return false;
        }

        Set<Long> turmaIds = turmaIdsDoProfessor(professorId);
        if (turmaIds.isEmpty()) {
            return false;
        }

        return alunoTurmaRepository.existsByAluno_IdAndTurma_IdIn(alunoId, turmaIds);
    }

    /**
     * Ids dos alunos com matrícula <b>ATIVA</b> em alguma turma do professor.
     * Só matrículas ativas porque é o mesmo critério de
     * {@code AlunoTurmaService.ativosPorTurma}, usado pelas telas hoje; alunos
     * com matrícula encerrada em uma turma continuam visíveis por outra turma,
     * caso tenham uma.
     */
    public Set<Long> alunoIdsDoProfessor(Long professorId) {
        Set<Long> alunoIds = new LinkedHashSet<>();

        for (Long turmaId : turmaIdsDosVinculos(vinculosDoProfessor(professorId))) {
            for (AlunoTurma matricula : alunoTurmaRepository.findByTurmaIdAndStatus(turmaId, StatusMatricula.ATIVA)) {
                if (matricula.getAluno() != null && matricula.getAluno().getId() != null) {
                    alunoIds.add(matricula.getAluno().getId());
                }
            }
        }

        return alunoIds;
    }

    /** Vínculos em que o professor é titular; lista vazia quando não há id. */
    private List<TurmaDisciplina> vinculosDoProfessor(Long professorId) {
        if (professorId == null) {
            return List.of();
        }
        List<TurmaDisciplina> vinculos = turmaDisciplinaRepository.findByProfessorId(professorId);
        return vinculos != null ? vinculos : List.of();
    }

    /** Turmas distintas dos vínculos, ignorando vínculo sem turma ou turma sem id. */
    private Set<Long> turmaIdsDosVinculos(List<TurmaDisciplina> vinculos) {
        Set<Long> turmaIds = new LinkedHashSet<>();
        for (TurmaDisciplina vinculo : vinculos) {
            if (vinculo.getTurma() != null && vinculo.getTurma().getId() != null) {
                turmaIds.add(vinculo.getTurma().getId());
            }
        }
        return turmaIds;
    }
}
