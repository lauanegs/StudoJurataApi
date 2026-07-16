package studojurata_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.Disciplina;

import java.util.List;

public interface DisciplinaRepository extends JpaRepository<Disciplina, Long> {
    /** Correção 2.2 da Terceira Análise Crítica (isolamento multi-tenant): filtra por escola. */
    List<Disciplina> findByEscola_Id(Long escolaId);
}