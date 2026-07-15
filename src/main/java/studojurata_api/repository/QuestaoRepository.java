package studojurata_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.Questao;
import studojurata_api.model.enums.StatusQuestao;

import java.util.List;

public interface QuestaoRepository extends JpaRepository<Questao, Long> {

    List<Questao> findByStatus(StatusQuestao status);

    List<Questao> findByDisciplinaIdAndStatus(Long disciplinaId, StatusQuestao status);
}
