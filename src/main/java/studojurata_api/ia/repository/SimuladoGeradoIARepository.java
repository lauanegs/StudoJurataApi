package studojurata_api.ia.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.ia.model.SimuladoGeradoIA;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SimuladoGeradoIARepository extends JpaRepository<SimuladoGeradoIA, Long> {

    Optional<SimuladoGeradoIA> findBySimuladoId(Long simuladoId);

    /**
     * Usado por GeracaoAutomaticaSimuladoJob pra não gerar de novo o mesmo
     * ciclo de repetição espaçada — dataProximoReforco só muda quando o
     * professor efetivamente revisa (RevisaoConteudoService.registrarReforco),
     * então sem essa checagem o job re-geraria um simulado por execução
     * enquanto o rascunho ficar parado esperando revisão.
     */
    boolean existsByAlunoIdAndConteudoPlanoIdAndPrazoLancamento(Long alunoId, Long conteudoPlanoId, LocalDate prazoLancamento);

    /**
     * Usado por GeracaoAutomaticaBaixoAproveitamentoJob pra não empilhar um
     * novo rascunho enquanto já existir um (de qualquer motivo/job) ainda não
     * revisado pelo professor pra este mesmo aluno+conteúdo.
     */
    List<SimuladoGeradoIA> findByAlunoIdAndConteudoPlanoId(Long alunoId, Long conteudoPlanoId);
}
