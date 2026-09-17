package studojurata_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.SimuladoAluno;
import studojurata_api.model.enums.StatusSimuladoAluno;

import java.util.List;

public interface SimuladoAlunoRepository extends JpaRepository<SimuladoAluno, Long> {

    List<SimuladoAluno> findBySimuladoId(Long simuladoId);

    List<SimuladoAluno> findByAlunoId(Long alunoId);

    boolean existsBySimuladoIdAndAlunoId(Long simuladoId, Long alunoId);

    List<SimuladoAluno> findByAluno_IdAndStatusAndSimulado_Disciplina_IdAndSimulado_Turma_Id(
            Long alunoId, StatusSimuladoAluno status, Long disciplinaId, Long turmaId);
}
