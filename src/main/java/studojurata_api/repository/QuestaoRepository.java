package studojurata_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.Questao;
import studojurata_api.model.enums.StatusQuestao;

import java.util.Collection;
import java.util.List;

public interface QuestaoRepository extends JpaRepository<Questao, Long> {

    List<Questao> findByStatus(StatusQuestao status);

    /** Questoes das disciplinas informadas — escopo do professor. */
    List<Questao> findByDisciplina_IdIn(Collection<Long> disciplinaIds);

    /** Questoes de um status nas disciplinas informadas (ex.: pendentes do professor). */
    List<Questao> findByStatusAndDisciplina_IdIn(StatusQuestao status, Collection<Long> disciplinaIds);
}
