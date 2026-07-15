package studojurata_api.ia.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.ia.model.HistoricoGeracaoIA;

import java.util.List;

public interface HistoricoGeracaoIARepository extends JpaRepository<HistoricoGeracaoIA, Long> {

    List<HistoricoGeracaoIA> findByConteudoPlanoIdOrderByDataGeracaoDesc(Long conteudoPlanoId);

    List<HistoricoGeracaoIA> findBySimuladoIdOrderByDataGeracaoDesc(Long simuladoId);

    List<HistoricoGeracaoIA> findAllByOrderByDataGeracaoDesc();
}
