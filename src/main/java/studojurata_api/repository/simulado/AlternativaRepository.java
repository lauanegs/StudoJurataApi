package studojurata_api.repository.simulado;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.simulado.Alternativa;

import java.util.Collection;
import java.util.List;

public interface AlternativaRepository extends JpaRepository<Alternativa, Long> {
    List<Alternativa> findByQuestaoIdOrderByOrdem(Long questaoId);

    List<Alternativa> findByQuestaoIdAndCorretaTrue(Long questaoId);

    List<Alternativa> findByQuestao_IdIn(Collection<Long> questaoIds);

    List<Alternativa> findByQuestao_Disciplina_IdIn(Collection<Long> disciplinaIds);
}
