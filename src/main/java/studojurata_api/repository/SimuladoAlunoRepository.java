package studojurata_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.SimuladoAluno;
import studojurata_api.model.enums.StatusSimuladoAluno;

import java.util.Collection;
import java.util.List;

public interface SimuladoAlunoRepository extends JpaRepository<SimuladoAluno, Long> {

    List<SimuladoAluno> findBySimuladoId(Long simuladoId);

    List<SimuladoAluno> findByAlunoId(Long alunoId);

    List<SimuladoAluno> findByStatusAndNotaIsNotNullAndSimulado_Turma_IdIn(
            StatusSimuladoAluno status, Collection<Long> turmaIds);

    boolean existsBySimuladoIdAndAlunoId(Long simuladoId, Long alunoId);

    /** Tentativas dos simulados informados — escopo das listagens pedagogicas. */
    List<SimuladoAluno> findBySimulado_IdIn(Collection<Long> simuladoIds);

    List<SimuladoAluno> findByAluno_IdAndStatusAndSimulado_Disciplina_IdAndSimulado_Turma_Id(
            Long alunoId, StatusSimuladoAluno status, Long disciplinaId, Long turmaId);
}
