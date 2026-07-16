package studojurata_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.PlanoEnsino;

import java.util.List;

public interface PlanoEnsinoRepository extends JpaRepository<PlanoEnsino, Long> {
    /** Todos os planos de ensino (um por disciplina) vinculados a um curso. */
    List<PlanoEnsino> findByCurso_Id(Long cursoId);
}