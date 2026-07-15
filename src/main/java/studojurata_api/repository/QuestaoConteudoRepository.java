package studojurata_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.QuestaoConteudo;

import java.util.List;

public interface QuestaoConteudoRepository extends JpaRepository<QuestaoConteudo, Long> {

    boolean existsByQuestaoId(Long questaoId);

    /** Usado pelo módulo de IA (cache/fallback — ver GeracaoQuestaoIAService) para localizar
     *  questões já vinculadas a um conteúdo. */
    List<QuestaoConteudo> findByConteudoPlano_Id(Long conteudoPlanoId);

    List<QuestaoConteudo> findByQuestao_IdIn(List<Long> questaoIds);
}