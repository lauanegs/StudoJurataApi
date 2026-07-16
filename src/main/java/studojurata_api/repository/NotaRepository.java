package studojurata_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.Nota;

import java.util.List;
import java.util.Optional;

public interface NotaRepository extends JpaRepository<Nota, Long> {

    Optional<Nota> findByAluno_IdAndDisciplina_IdAndPeriodoLetivo(Long alunoId, Long disciplinaId, String periodoLetivo);

    /** Histórico completo do aluno (todas as disciplinas e períodos letivos), do mais recente ao mais antigo. */
    List<Nota> findByAluno_IdOrderByPeriodoLetivoDesc(Long alunoId);

    List<Nota> findByAluno_IdAndDisciplina_IdOrderByPeriodoLetivoDesc(Long alunoId, Long disciplinaId);
}
