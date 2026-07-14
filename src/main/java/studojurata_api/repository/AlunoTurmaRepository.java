package studojurata_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.AlunoTurma;
import studojurata_api.model.enums.StatusMatricula;

import java.util.List;
import java.util.Optional;

public interface AlunoTurmaRepository extends JpaRepository<AlunoTurma, Long> {

    long countByTurmaIdAndStatus(Long turmaId, StatusMatricula status);

    boolean existsByAluno_IdAndTurma_IdAndStatus(Long alunoId, Long turmaId, StatusMatricula status);

    Optional<AlunoTurma> findFirstByAluno_IdAndTurma_IdAndStatus(Long alunoId, Long turmaId, StatusMatricula status);

    List<AlunoTurma> findByTurmaIdAndStatus(Long turmaId, StatusMatricula status);

    List<AlunoTurma> findByTurmaIdOrderByDataInicioDesc(Long turmaId);

    List<AlunoTurma> findByAlunoIdOrderByDataInicioDesc(Long alunoId);
}