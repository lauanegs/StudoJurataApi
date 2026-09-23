package studojurata_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.SimuladoQuestao;
import studojurata_api.model.enums.StatusSimuladoQuestao;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SimuladoQuestaoRepository extends JpaRepository<SimuladoQuestao, Long> {

    List<SimuladoQuestao> findBySimuladoIdAndStatusOrderByOrdem(Long simuladoId, StatusSimuladoQuestao status);

    long countBySimuladoIdAndStatus(Long simuladoId, StatusSimuladoQuestao status);

    /** Vinculos questao-simulado dos simulados informados. */
    List<SimuladoQuestao> findBySimulado_IdIn(Collection<Long> simuladoIds);

    /** Vínculo de uma questão com um simulado — usado no desvínculo. */
    Optional<SimuladoQuestao> findFirstBySimuladoIdAndQuestaoId(Long simuladoId, Long questaoId);

    /** Já existe vínculo com este status para o par simulado × questão? */
    boolean existsBySimuladoIdAndQuestaoIdAndStatus(Long simuladoId, Long questaoId, StatusSimuladoQuestao status);
}
