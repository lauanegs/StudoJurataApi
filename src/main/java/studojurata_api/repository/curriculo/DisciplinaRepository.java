package studojurata_api.repository.curriculo;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.curriculo.Disciplina;

import java.util.List;

public interface DisciplinaRepository extends JpaRepository<Disciplina, Long> {
    List<Disciplina> findByEscola_Id(Long escolaId);
}