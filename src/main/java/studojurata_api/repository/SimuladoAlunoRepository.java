package studojurata_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.SimuladoAluno;
import studojurata_api.model.enums.StatusSimuladoAluno;

import java.util.List;
import java.util.Optional;

public interface SimuladoAlunoRepository extends JpaRepository<SimuladoAluno, Long> {

    List<SimuladoAluno> findBySimuladoId(Long simuladoId);

    List<SimuladoAluno> findByAlunoId(Long alunoId);

    List<SimuladoAluno> findByAlunoIdAndStatus(Long alunoId, StatusSimuladoAluno status);

    Optional<SimuladoAluno> findFirstBySimuladoIdAndAlunoId(Long simuladoId, Long alunoId);

    boolean existsBySimuladoIdAndAlunoId(Long simuladoId, Long alunoId);

    /**
     * Usada por NotaService.recalcular (item 1.2/2.13 da Segunda Análise
     * Crítica) para obter todos os simulados concluídos de um aluno, numa
     * disciplina e período letivo específicos. Correção do bug "nota não
     * recalcula sem Plano de Ensino": passa a percorrer Simulado.periodoLetivo
     * diretamente (campo próprio do Simulado), não mais via
     * Simulado.planoEnsino.periodoLetivo, que é opcional.
     */
    List<SimuladoAluno> findByAluno_IdAndStatusAndSimulado_Disciplina_IdAndSimulado_PeriodoLetivo(
            Long alunoId, StatusSimuladoAluno status, Long disciplinaId, String periodoLetivo);
}
