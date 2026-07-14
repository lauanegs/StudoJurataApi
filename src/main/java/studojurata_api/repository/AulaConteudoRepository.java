package studojurata_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.AulaConteudo;

import java.util.List;

public interface AulaConteudoRepository extends JpaRepository<AulaConteudo, Long> {

    List<AulaConteudo> findByAula_Id(Long aulaId);

    List<AulaConteudo> findByConteudoPlano_Id(Long conteudoPlanoId);

    boolean existsByAula_IdAndConteudoPlano_Id(Long aulaId, Long conteudoPlanoId);

    void deleteByAula_IdAndConteudoPlano_Id(Long aulaId, Long conteudoPlanoId);
}
