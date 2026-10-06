package studojurata_api.repository.simulado;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.simulado.SimuladoQuestao;
import studojurata_api.model.enums.StatusSimuladoQuestao;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SimuladoQuestaoRepository extends JpaRepository<SimuladoQuestao, Long> {
    List<SimuladoQuestao> findBySimuladoIdAndStatusOrderByOrdem(Long simuladoId, StatusSimuladoQuestao status);

    long countBySimuladoIdAndStatus(Long simuladoId, StatusSimuladoQuestao status);

    List<SimuladoQuestao> findBySimulado_IdIn(Collection<Long> simuladoIds);

    Optional<SimuladoQuestao> findFirstBySimuladoIdAndQuestaoId(Long simuladoId, Long questaoId);

    boolean existsBySimuladoIdAndQuestaoIdAndStatus(Long simuladoId, Long questaoId, StatusSimuladoQuestao status);
}
