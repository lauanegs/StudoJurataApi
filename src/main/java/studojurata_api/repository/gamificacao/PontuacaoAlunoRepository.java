package studojurata_api.repository.gamificacao;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.gamificacao.PontuacaoAluno;

import java.util.Optional;

public interface PontuacaoAlunoRepository extends JpaRepository<PontuacaoAluno, Long> {
    Optional<PontuacaoAluno> findByAluno_Id(Long alunoId);
}
