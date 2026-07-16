package studojurata_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.HorarioTurma;
import studojurata_api.model.enums.DiaSemana;

import java.util.List;

public interface HorarioTurmaRepository extends JpaRepository<HorarioTurma, Long> {
    List<HorarioTurma> findByTurma_Id(Long turmaId);
    List<HorarioTurma> findByTurma_IdAndDiaSemana(Long turmaId, DiaSemana diaSemana);
}
