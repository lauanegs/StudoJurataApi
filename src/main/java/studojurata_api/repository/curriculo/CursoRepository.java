package studojurata_api.repository.curriculo;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.curriculo.Curso;

import java.util.List;

public interface CursoRepository extends JpaRepository<Curso, Long> {
    List<Curso> findByEscola_Id(Long escolaId);
}
