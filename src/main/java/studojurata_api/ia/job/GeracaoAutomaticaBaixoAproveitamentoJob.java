package studojurata_api.ia.job;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import studojurata_api.exception.RegraNegocioException;
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
 * Baixo aproveitamento não tem data de vencimento própria (ao contrário da
 * repetição espaçada), então é reavaliado por intervalo fixo para todos os
 * alunos.
 *
 * Para não empilhar rascunhos, não gera um novo para aluno+conteúdo enquanto
 * existir um simulado gerado por IA, de qualquer motivo, ainda em RASCUNHO.
 */
@Component
@RequiredArgsConstructor
public class GeracaoAutomaticaBaixoAproveitamentoJob {

    private static final Logger log = LoggerFactory.getLogger(GeracaoAutomaticaBaixoAproveitamentoJob.class);

    private final AlunoRepository alunoRepository;
    private final RecomendacaoService recomendacaoService;
    private final SimuladoGeradoIARepository simuladoGeradoIARepository;
    private final GeracaoSimuladoIAService geracaoSimuladoIAService;

    /** fixedDelay em vez de cron: cron não expressa "a cada 7 dias" de forma exata. */
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
                } catch (RegraNegocioException regra) {
                    // Regra de negócio (ex.: baixo aproveitamento coletivo pede revisão em
                    // sala) não é falha do job; fica no log informativo.
                    log.info("Geração por baixo aproveitamento ignorada (aluno {}, conteúdo {}): {}",
                            alunoId, recomendacao.getConteudoPlanoId(), regra.getMessage());
                } catch (RuntimeException erro) {
                    // Uma falha pontual não pode interromper a geração dos demais alunos.
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
