package studojurata_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.Alternativa;

import java.util.Collection;
import java.util.List;

public interface AlternativaRepository extends JpaRepository<Alternativa, Long> {

    List<Alternativa> findByQuestaoIdOrderByOrdem(Long questaoId);

    List<Alternativa> findByQuestaoIdAndCorretaTrue(Long questaoId);

    /** Alternativas das questoes informadas — carga em lote, sem N+1. */
    List<Alternativa> findByQuestao_IdIn(Collection<Long> questaoIds);

    /** Alternativas das questoes das disciplinas informadas — escopo do professor. */
    List<Alternativa> findByQuestao_Disciplina_IdIn(Collection<Long> disciplinaIds);
}
