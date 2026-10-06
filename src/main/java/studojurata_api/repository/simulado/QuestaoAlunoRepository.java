package studojurata_api.repository.simulado;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.simulado.QuestaoAluno;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface QuestaoAlunoRepository extends JpaRepository<QuestaoAluno, Long> {
    List<QuestaoAluno> findBySimuladoAlunoId(Long simuladoAlunoId);

    Optional<QuestaoAluno> findFirstBySimuladoAlunoIdAndQuestaoId(Long simuladoAlunoId, Long questaoId);

    List<QuestaoAluno> findBySimuladoAluno_AlunoId(Long alunoId);

    List<QuestaoAluno> findByQuestao_IdIn(Collection<Long> questaoIds);

    List<QuestaoAluno> findBySimuladoAluno_IdIn(Collection<Long> simuladoAlunoIds);
}
