package studojurata_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.Aluno;

public interface AlunoRepository extends JpaRepository<Aluno, Long> {
}