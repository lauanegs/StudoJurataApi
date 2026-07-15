package studojurata_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.QuestaoConteudo;

public interface QuestaoConteudoRepository extends JpaRepository<QuestaoConteudo, Long> {

    boolean existsByQuestaoId(Long questaoId);
}