package studojurata_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.Nota;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface NotaRepository extends JpaRepository<Nota, Long> {

    Optional<Nota> findByAluno_IdAndDisciplina_IdAndTurma_Id(Long alunoId, Long disciplinaId, Long turmaId);

    List<Nota> findByAluno_IdOrderByCreatedAtDesc(Long alunoId);

    /** Notas vinculadas as turmas informadas — escopo do professor. */
    List<Nota> findByTurma_IdIn(Collection<Long> turmaIds);

    /**
     * Notas sem turma (historicas) dos alunos informados — preservadas ao
     * professor quando o aluno esta no escopo dele (decisao do bloco C2.6).
     */
    List<Nota> findByTurmaIsNullAndAluno_IdIn(Collection<Long> alunoIds);
}
