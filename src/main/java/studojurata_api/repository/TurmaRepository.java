package studojurata_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.Turma;

import java.util.List;

public interface TurmaRepository extends JpaRepository<Turma, Long> {
    /** Correção 2.2 da Terceira Análise Crítica (isolamento multi-tenant): filtra por escola. */
    List<Turma> findByEscola_Id(Long escolaId);
}