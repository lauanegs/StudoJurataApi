package studojurata_api.repository.simulado;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.simulado.Questao;
import studojurata_api.model.enums.StatusQuestao;

import java.util.Collection;
import java.util.List;

public interface QuestaoRepository extends JpaRepository<Questao, Long> {
    List<Questao> findByStatus(StatusQuestao status);

    List<Questao> findByDisciplina_IdIn(Collection<Long> disciplinaIds);

    List<Questao> findByStatusAndDisciplina_IdIn(StatusQuestao status, Collection<Long> disciplinaIds);
}
