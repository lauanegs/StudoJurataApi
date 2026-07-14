package studojurata_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.PlanoAula;
import studojurata_api.model.enums.StatusAtivoInativo;

import java.util.List;

public interface PlanoAulaRepository extends JpaRepository<PlanoAula, Long> {

    List<PlanoAula> findByTurmaDisciplina_Id(Long turmaDisciplinaId);

    List<PlanoAula> findByPlanoEnsino_Id(Long planoEnsinoId);

    List<PlanoAula> findByStatus(StatusAtivoInativo status);
}
