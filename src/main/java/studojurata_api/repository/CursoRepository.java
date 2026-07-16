package studojurata_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.Curso;

import java.util.List;

public interface CursoRepository extends JpaRepository<Curso, Long> {
    /** Isolamento por escola (correção 2.2 da Terceira Análise Crítica). */
    List<Curso> findByEscola_Id(Long escolaId);
}
