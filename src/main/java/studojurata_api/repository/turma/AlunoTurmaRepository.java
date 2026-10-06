package studojurata_api.repository.turma;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.turma.AlunoTurma;
import studojurata_api.model.enums.StatusMatricula;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AlunoTurmaRepository extends JpaRepository<AlunoTurma, Long> {
    long countByTurmaIdAndStatus(Long turmaId, StatusMatricula status);

    boolean existsByAluno_IdAndTurma_IdAndStatus(Long alunoId, Long turmaId, StatusMatricula status);

    Optional<AlunoTurma> findFirstByAluno_IdAndTurma_IdAndStatus(Long alunoId, Long turmaId, StatusMatricula status);

    List<AlunoTurma> findByTurmaIdAndStatus(Long turmaId, StatusMatricula status);

    List<AlunoTurma> findByTurmaIdOrderByDataInicioDesc(Long turmaId);

    boolean existsByTurma_Id(Long turmaId);

    boolean existsByAluno_Id(Long alunoId);

    Optional<AlunoTurma> findFirstByAluno_IdAndTurma_IdOrderByDataInicioDesc(Long alunoId, Long turmaId);

    boolean existsByAluno_IdAndTurma_IdIn(Long alunoId, Collection<Long> turmaIds);

    List<AlunoTurma> findByAluno_Id(Long alunoId);

    List<AlunoTurma> findByTurma_IdIn(Collection<Long> turmaIds);
}
