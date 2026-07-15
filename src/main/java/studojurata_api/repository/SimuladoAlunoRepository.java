package studojurata_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.SimuladoAluno;
import studojurata_api.model.enums.StatusSimuladoAluno;

import java.util.List;
import java.util.Optional;

public interface SimuladoAlunoRepository extends JpaRepository<SimuladoAluno, Long> {

    List<SimuladoAluno> findBySimuladoId(Long simuladoId);

    List<SimuladoAluno> findByAlunoId(Long alunoId);

    List<SimuladoAluno> findByAlunoIdAndStatus(Long alunoId, StatusSimuladoAluno status);

    Optional<SimuladoAluno> findFirstBySimuladoIdAndAlunoId(Long simuladoId, Long alunoId);

    boolean existsBySimuladoIdAndAlunoId(Long simuladoId, Long alunoId);
}
