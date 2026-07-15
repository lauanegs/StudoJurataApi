package studojurata_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.SimuladoQuestao;
import studojurata_api.model.enums.StatusSimuladoQuestao;

import java.util.List;

public interface SimuladoQuestaoRepository extends JpaRepository<SimuladoQuestao, Long> {

    List<SimuladoQuestao> findBySimuladoIdOrderByOrdem(Long simuladoId);

    List<SimuladoQuestao> findBySimuladoIdAndStatusOrderByOrdem(Long simuladoId, StatusSimuladoQuestao status);

    long countBySimuladoIdAndStatus(Long simuladoId, StatusSimuladoQuestao status);
}
