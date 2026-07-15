package studojurata_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.Simulado;
import studojurata_api.model.enums.StatusSimulado;

import java.util.List;

public interface SimuladoRepository extends JpaRepository<Simulado, Long> {

    List<Simulado> findByTurmaId(Long turmaId);

    List<Simulado> findByTurmaIdAndStatus(Long turmaId, StatusSimulado status);

    List<Simulado> findByDisciplinaId(Long disciplinaId);
}
