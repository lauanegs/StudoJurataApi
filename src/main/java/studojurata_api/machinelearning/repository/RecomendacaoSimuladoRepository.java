package studojurata_api.machinelearning.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import studojurata_api.machinelearning.model.RecomendacaoSimulado;

public interface RecomendacaoSimuladoRepository extends JpaRepository<RecomendacaoSimulado, Long> {

    /** Recomendações com desfecho observado — é o dataset de treino do Weka. */
    List<RecomendacaoSimulado> findByPercentualAcertoPosteriorIsNotNull();

    /**
     * Recomendações do aluno ainda sem desfecho, da mais recente para a mais
     * antiga — onde o próximo resultado é registrado (uma tentativa rotula só a
     * recomendação que a explica).
     */
    List<RecomendacaoSimulado> findByAluno_IdAndPercentualAcertoPosteriorIsNullOrderByCreatedAtDesc(Long alunoId);

    List<RecomendacaoSimulado> findByAluno_IdOrderByCreatedAtDesc(Long alunoId);
}
