package studojurata_api.repository.simulado;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.simulado.Simulado;
import studojurata_api.model.enums.StatusSimulado;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface SimuladoRepository extends JpaRepository<Simulado, Long> {
    List<Simulado> findByTurma_IdIn(Collection<Long> turmaIds);

    List<Simulado> findByStatusAndDataInicioLessThanEqual(StatusSimulado status, LocalDateTime limite);

    List<Simulado> findByStatusAndDataFimLessThanEqual(StatusSimulado status, LocalDateTime limite);
}
