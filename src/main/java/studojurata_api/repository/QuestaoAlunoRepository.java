package studojurata_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.QuestaoAluno;

public interface QuestaoAlunoRepository extends JpaRepository<QuestaoAluno, Long> {
}