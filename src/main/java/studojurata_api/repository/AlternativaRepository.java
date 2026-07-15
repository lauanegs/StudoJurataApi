package studojurata_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.Alternativa;

import java.util.List;

public interface AlternativaRepository extends JpaRepository<Alternativa, Long> {

    List<Alternativa> findByQuestaoIdOrderByOrdem(Long questaoId);

    List<Alternativa> findByQuestaoIdAndCorretaTrue(Long questaoId);

    long countByQuestaoIdAndCorretaTrue(Long questaoId);
}
