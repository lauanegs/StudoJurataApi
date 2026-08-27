package studojurata_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.Nota;

import java.util.List;
import java.util.Optional;

public interface NotaRepository extends JpaRepository<Nota, Long> {

    Optional<Nota> findByAluno_IdAndDisciplina_IdAndTurma_Id(Long alunoId, Long disciplinaId, Long turmaId);

    /** Histórico completo do aluno (todas as disciplinas e turmas), do mais recente ao mais antigo. */
    List<Nota> findByAluno_IdOrderByCreatedAtDesc(Long alunoId);

    List<Nota> findByAluno_IdAndDisciplina_IdOrderByCreatedAtDesc(Long alunoId, Long disciplinaId);
}
