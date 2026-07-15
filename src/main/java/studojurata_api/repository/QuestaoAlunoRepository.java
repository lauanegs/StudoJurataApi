package studojurata_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.QuestaoAluno;

import java.util.List;
import java.util.Optional;

public interface QuestaoAlunoRepository extends JpaRepository<QuestaoAluno, Long> {

    List<QuestaoAluno> findBySimuladoAlunoId(Long simuladoAlunoId);

    Optional<QuestaoAluno> findFirstBySimuladoAlunoIdAndQuestaoId(Long simuladoAlunoId, Long questaoId);

    boolean existsBySimuladoAlunoIdAndQuestaoId(Long simuladoAlunoId, Long questaoId);

    /** Usado pelo módulo de IA (RecomendacaoService, item 7.4) para apurar desempenho por conteúdo. */
    List<QuestaoAluno> findBySimuladoAluno_AlunoId(Long alunoId);
}
