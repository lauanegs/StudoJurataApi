package studojurata_api.ia.job;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import studojurata_api.ia.dto.RecomendacaoDTO;
import studojurata_api.ia.model.SimuladoGeradoIA;
import studojurata_api.ia.model.enums.MotivoRecomendacao;
import studojurata_api.ia.repository.SimuladoGeradoIARepository;
import studojurata_api.ia.service.GeracaoSimuladoIAService;
import studojurata_api.ia.service.RecomendacaoService;
import studojurata_api.model.Aluno;
import studojurata_api.model.enums.StatusSimulado;
import studojurata_api.repository.AlunoRepository;

/**
 * Geração automática do simulado de reforço por baixo aproveitamento
 * (RecomendacaoService, item 1.4) — motivo sem uma data própria de vencimento
 * (ao contrário de REPETICAO_ESPACADA, ver GeracaoAutomaticaSimuladoJob), por
 * isso reavaliado por um intervalo fixo em vez de um gatilho de data:
 * confirmado pelo usuário, a cada 7 dias o job recalcula a taxa de acerto de
 * TODOS os alunos e gera um simulado pra quem ainda estiver abaixo do
 * limiar (RecomendacaoService.LIMIAR_BAIXO_APROVEITAMENTO).
 *
 * Sem dado próprio pra "já gerei pra este ciclo" (não há campo de data como
 * RevisaoConteudo.dataProximoReforco), a checagem de duplicidade aqui é:
 * não gerar um novo rascunho pra um aluno+conteúdo enquanto já existir um
 * simulado gerado por IA (de qualquer motivo — inclusive o job de repetição
 * espaçada) ainda em RASCUNHO, ou seja, ainda não revisado/lançado pelo
 * professor. Assim que aquele rascunho for lançado (ou encerrado), a próxima
 * execução do job pode gerar um novo, se o aluno continuar abaixo do limiar.
 */
@Component
@RequiredArgsConstructor
public class GeracaoAutomaticaBaixoAproveitamentoJob {

    private static final Logger log = LoggerFactory.getLogger(GeracaoAutomaticaBaixoAproveitamentoJob.class);

    private final AlunoRepository alunoRepository;
    private final RecomendacaoService recomendacaoService;
    private final SimuladoGeradoIARepository simuladoGeradoIARepository;
    private final GeracaoSimuladoIAService geracaoSimuladoIAService;

    /**
     * Intervalo fixo de verdade (fixedDelay, não cron) — 7 dias contados a
     * partir do fim da execução anterior, sem depender de dia do mês (que
     * não divide igualmente por 7). Configurável via
     * studojurata.ia.geracao-automatica.baixo-aproveitamento.intervalo-ms.
     */
    @Scheduled(fixedDelayString = "${studojurata.ia.geracao-automatica.baixo-aproveitamento.intervalo-ms:604800000}")
    public void gerarParaBaixoAproveitamento() {
        for (Aluno aluno : alunoRepository.findAll()) {
            Long alunoId = aluno.getId();

            for (RecomendacaoDTO recomendacao : recomendacaoService.recomendarParaAluno(alunoId)) {
                if (!recomendacao.getMotivos().contains(MotivoRecomendacao.BAIXO_APROVEITAMENTO)) continue;
                if (existeRascunhoPendente(alunoId, recomendacao.getConteudoPlanoId())) continue;

                try {
                    geracaoSimuladoIAService.gerarParaAluno(
                            alunoId,
                            recomendacao.getConteudoPlanoId(),
                            recomendacao.getNivelPrioritario(),
                            recomendacao.getMotivos());
                } catch (RuntimeException erro) {
                    // Isolado por recomendação — mesma lógica do job de repetição
                    // espaçada: uma falha pontual não pode travar os demais alunos.
                    log.error(
                            "Falha ao gerar simulado automático por baixo aproveitamento (aluno {}, conteúdo {})",
                            alunoId, recomendacao.getConteudoPlanoId(), erro);
                }
            }
        }
    }

    private boolean existeRascunhoPendente(Long alunoId, Long conteudoPlanoId) {
        return simuladoGeradoIARepository.findByAlunoIdAndConteudoPlanoId(alunoId, conteudoPlanoId).stream()
                .map(SimuladoGeradoIA::getSimulado)
                .anyMatch(simulado -> simulado.getStatus() == StatusSimulado.RASCUNHO);
    }
}
