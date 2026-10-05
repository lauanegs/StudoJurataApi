package studojurata_api.repository.simulado;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.simulado.QuestaoConteudo;

import java.util.Collection;
import java.util.List;

public interface QuestaoConteudoRepository extends JpaRepository<QuestaoConteudo, Long> {


    List<QuestaoConteudo> findByConteudoPlano_Id(Long conteudoPlanoId);

    /** Vínculos dos conteúdos informados — seleção de questões em lote, sem N+1. */
    List<QuestaoConteudo> findByConteudoPlano_IdIn(Collection<Long> conteudoPlanoIds);

    List<QuestaoConteudo> findByQuestao_IdIn(List<Long> questaoIds);

    List<QuestaoConteudo> findByQuestao_Id(Long questaoId);

    boolean existsByQuestao_IdAndConteudoPlano_Id(Long questaoId, Long conteudoPlanoId);

    void deleteByQuestao_IdAndConteudoPlano_Id(Long questaoId, Long conteudoPlanoId);
}
