package studojurata_api.repository.planejamento;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.planejamento.ConteudoPlano;

import java.util.Collection;
import java.util.List;

public interface ConteudoPlanoRepository extends JpaRepository<ConteudoPlano, Long> {
    List<ConteudoPlano> findByPlanoEnsino_TurmaDisciplina_IdIn(Collection<Long> turmaDisciplinaIds);

    List<ConteudoPlano> findByPlanoEnsino_TurmaDisciplinaIsNull();

    List<ConteudoPlano> findByPlanoEnsinoIsNull();
}
