package studojurata_api.machinelearning.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import studojurata_api.machinelearning.model.RecomendacaoSimulado;

public interface RecomendacaoSimuladoRepository extends JpaRepository<RecomendacaoSimulado, Long> {
    List<RecomendacaoSimulado> findByAluno_IdAndPercentualAcertoPosteriorIsNullOrderByCreatedAtDesc(Long alunoId);

    List<RecomendacaoSimulado> findByAluno_IdOrderByCreatedAtDesc(Long alunoId);
}
