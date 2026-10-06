package studojurata_api.repository.nota;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.nota.Nota;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface NotaRepository extends JpaRepository<Nota, Long> {
    Optional<Nota> findByAluno_IdAndDisciplina_IdAndTurma_Id(Long alunoId, Long disciplinaId, Long turmaId);

    List<Nota> findByAluno_IdOrderByCreatedAtDesc(Long alunoId);

    List<Nota> findByTurma_IdIn(Collection<Long> turmaIds);

    List<Nota> findByTurmaIsNullAndAluno_IdIn(Collection<Long> alunoIds);
}
