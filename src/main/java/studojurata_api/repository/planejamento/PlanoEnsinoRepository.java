package studojurata_api.repository.planejamento;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.planejamento.PlanoEnsino;

import java.util.Collection;
import java.util.List;

public interface PlanoEnsinoRepository extends JpaRepository<PlanoEnsino, Long> {
    List<PlanoEnsino> findByCurso_Escola_Id(Long escolaId);

    List<PlanoEnsino> findByTurmaDisciplina_Id(Long turmaDisciplinaId);

    List<PlanoEnsino> findByTurmaDisciplina_IdIn(Collection<Long> turmaDisciplinaIds);

    List<PlanoEnsino> findByTurmaDisciplinaIsNull();
}
