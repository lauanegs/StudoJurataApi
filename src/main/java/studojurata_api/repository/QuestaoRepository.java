package studojurata_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.Questao;

public interface QuestaoRepository extends JpaRepository<Questao, Long> {
}