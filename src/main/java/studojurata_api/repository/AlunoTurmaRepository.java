package studojurata_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.AlunoTurma;
import studojurata_api.model.enums.StatusMatricula;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AlunoTurmaRepository extends JpaRepository<AlunoTurma, Long> {

    long countByTurmaIdAndStatus(Long turmaId, StatusMatricula status);

    boolean existsByAluno_IdAndTurma_IdAndStatus(Long alunoId, Long turmaId, StatusMatricula status);

    Optional<AlunoTurma> findFirstByAluno_IdAndTurma_IdAndStatus(Long alunoId, Long turmaId, StatusMatricula status);

    List<AlunoTurma> findByTurmaIdAndStatus(Long turmaId, StatusMatricula status);

    List<AlunoTurma> findByTurmaIdOrderByDataInicioDesc(Long turmaId);


    /** Usado para impedir excluir turma/aluno com qualquer matrícula vinculada (ativa ou histórico). */
    boolean existsByTurma_Id(Long turmaId);

    boolean existsByAluno_Id(Long alunoId);

    /**
     * Qualquer status: a nota continua histórica mesmo depois de a matrícula
     * ser concluída ou cancelada.
     */
    Optional<AlunoTurma> findFirstByAluno_IdAndTurma_IdOrderByDataInicioDesc(Long alunoId, Long turmaId);

    /**
     * Pertencimento do aluno a alguma das turmas informadas — usado pelo escopo
     * do professor em endpoints individuais: uma consulta, em vez de carregar
     * todos os alunos das turmas dele.
     *
     * <p>Qualquer status de matrícula: o histórico continua visível ao professor.
     */
    boolean existsByAluno_IdAndTurma_IdIn(Long alunoId, Collection<Long> turmaIds);
}
