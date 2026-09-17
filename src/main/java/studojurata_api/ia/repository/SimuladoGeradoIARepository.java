package studojurata_api.ia.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.ia.model.SimuladoGeradoIA;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SimuladoGeradoIARepository extends JpaRepository<SimuladoGeradoIA, Long> {

    Optional<SimuladoGeradoIA> findBySimuladoId(Long simuladoId);

    boolean existsByAlunoIdAndConteudoPlanoIdAndPrazoLancamento(Long alunoId, Long conteudoPlanoId, LocalDate prazoLancamento);

    List<SimuladoGeradoIA> findByAlunoIdAndConteudoPlanoId(Long alunoId, Long conteudoPlanoId);
}
