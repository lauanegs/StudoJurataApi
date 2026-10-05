package studojurata_api.repository.curriculo;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.curriculo.CursoDisciplina;

import java.util.List;

public interface CursoDisciplinaRepository extends JpaRepository<CursoDisciplina, Long> {
    List<CursoDisciplina> findByCurso_Id(Long cursoId);
}
