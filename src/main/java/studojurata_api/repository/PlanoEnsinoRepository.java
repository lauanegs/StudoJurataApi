package studojurata_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.PlanoEnsino;

import java.util.List;

public interface PlanoEnsinoRepository extends JpaRepository<PlanoEnsino, Long> {
    /** Todos os planos de ensino (um por disciplina) vinculados a um curso. */
    List<PlanoEnsino> findByCurso_Id(Long cursoId);

    /**
     * Isolamento por escola (correção de auditoria: PlanoEnsino não tem
     * campo de escola direto, mas herda a escola do Curso vinculado — mesmo
     * padrão de CursoService/DisciplinaService/TurmaService/UsuarioService).
     */
    List<PlanoEnsino> findByCurso_Escola_Id(Long escolaId);

    /** Usado para impedir desvincular uma disciplina da turma enquanto há plano de ensino ativo (ver TurmaDisciplinaService). */
    List<PlanoEnsino> findByTurmaDisciplina_Id(Long turmaDisciplinaId);
}