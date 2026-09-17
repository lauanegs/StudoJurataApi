package studojurata_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.QuestaoConteudo;

import java.util.List;

public interface QuestaoConteudoRepository extends JpaRepository<QuestaoConteudo, Long> {

    boolean existsByQuestaoId(Long questaoId);

    List<QuestaoConteudo> findByConteudoPlano_Id(Long conteudoPlanoId);

    List<QuestaoConteudo> findByQuestao_IdIn(List<Long> questaoIds);

    List<QuestaoConteudo> findByQuestao_Id(Long questaoId);

    boolean existsByQuestao_IdAndConteudoPlano_Id(Long questaoId, Long conteudoPlanoId);

    void deleteByQuestao_IdAndConteudoPlano_Id(Long questaoId, Long conteudoPlanoId);
}