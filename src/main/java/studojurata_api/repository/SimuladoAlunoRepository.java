package studojurata_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.SimuladoAluno;

public interface SimuladoAlunoRepository extends JpaRepository<SimuladoAluno, Long> {
}