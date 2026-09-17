package studojurata_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.PlanoEnsino;

import java.util.List;

public interface PlanoEnsinoRepository extends JpaRepository<PlanoEnsino, Long> {
    List<PlanoEnsino> findByCurso_Id(Long cursoId);

    /** PlanoEnsino não tem escola própria: herda a do Curso. */
    List<PlanoEnsino> findByCurso_Escola_Id(Long escolaId);

    /** Usado para impedir desvincular uma disciplina da turma enquanto há plano de ensino ativo (ver TurmaDisciplinaService). */
    List<PlanoEnsino> findByTurmaDisciplina_Id(Long turmaDisciplinaId);
}