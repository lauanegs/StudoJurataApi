package studojurata_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.QuestaoAluno;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface QuestaoAlunoRepository extends JpaRepository<QuestaoAluno, Long> {

    List<QuestaoAluno> findBySimuladoAlunoId(Long simuladoAlunoId);

    Optional<QuestaoAluno> findFirstBySimuladoAlunoIdAndQuestaoId(Long simuladoAlunoId, Long questaoId);

    List<QuestaoAluno> findBySimuladoAluno_AlunoId(Long alunoId);

    /** Respostas das questões informadas — agregação em lote (machine learning), sem N+1. */
    List<QuestaoAluno> findByQuestao_IdIn(Collection<Long> questaoIds);

    /** Respostas das tentativas informadas — recorte das listagens pedagogicas. */
    List<QuestaoAluno> findBySimuladoAluno_IdIn(Collection<Long> simuladoAlunoIds);
}
