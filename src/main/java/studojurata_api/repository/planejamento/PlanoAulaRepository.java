package studojurata_api.repository.planejamento;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.planejamento.PlanoAula;

import java.util.Collection;
import java.util.List;

public interface PlanoAulaRepository extends JpaRepository<PlanoAula, Long> {
    List<PlanoAula> findByTurmaDisciplina_Id(Long turmaDisciplinaId);

    List<PlanoAula> findByPlanoEnsino_Id(Long planoEnsinoId);

    List<PlanoAula> findByTurmaDisciplina_IdIn(Collection<Long> turmaDisciplinaIds);
}
