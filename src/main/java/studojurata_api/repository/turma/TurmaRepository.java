package studojurata_api.repository.turma;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.turma.Turma;

import java.util.List;

public interface TurmaRepository extends JpaRepository<Turma, Long> {
    List<Turma> findByEscola_Id(Long escolaId);
}