package studojurata_api.ia.job;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import studojurata_api.ia.model.RevisaoConteudo;
import studojurata_api.ia.model.enums.MotivoRecomendacao;
import studojurata_api.ia.repository.RevisaoConteudoRepository;
import studojurata_api.ia.repository.SimuladoGeradoIARepository;
import studojurata_api.ia.service.GeracaoSimuladoIAService;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Gera o simulado de reforço assim que a repetição espaçada de um
 * aluno+conteúdo vence, sem ação do professor (ele só revisa e lança).
 * Baixo aproveitamento fica em GeracaoAutomaticaBaixoAproveitamentoJob.
 *
 * Cada revisão devida roda em transação e captura de erro próprias, para que
 * uma falha isolada não derrube as demais.
 */
@Component
@RequiredArgsConstructor
public class GeracaoAutomaticaSimuladoJob {

    private static final Logger log = LoggerFactory.getLogger(GeracaoAutomaticaSimuladoJob.class);

    private final RevisaoConteudoRepository revisaoConteudoRepository;
    private final SimuladoGeradoIARepository simuladoGeradoIARepository;
    private final GeracaoSimuladoIAService geracaoSimuladoIAService;

    @Scheduled(cron = "${studojurata.ia.geracao-automatica.cron:0 0 5 * * *}")
    public void gerarSimuladosDevidos() {
        List<RevisaoConteudo> devidos = revisaoConteudoRepository.findByDataProximoReforcoLessThanEqual(LocalDate.now());

        for (RevisaoConteudo revisao : devidos) {
            Long alunoId = revisao.getAluno().getId();
            Long conteudoPlanoId = revisao.getConteudoPlano().getId();
            LocalDate prazo = revisao.getDataProximoReforco();

            // dataProximoReforco só avança quando o professor revisa; sem isto o
            // job geraria um simulado novo a cada execução enquanto o rascunho espera.
            boolean jaGerado = simuladoGeradoIARepository
                    .existsByAlunoIdAndConteudoPlanoIdAndPrazoLancamento(alunoId, conteudoPlanoId, prazo);
            if (jaGerado) continue;

            try {
                geracaoSimuladoIAService.gerarParaAluno(
                        alunoId, conteudoPlanoId, null, Set.of(MotivoRecomendacao.REPETICAO_ESPACADA));
            } catch (RuntimeException erro) {
                // Uma falha pontual não pode interromper a geração das demais revisões.
                log.error(
                        "Falha ao gerar simulado automático (aluno {}, conteúdo {}, prazo {})",
                        alunoId, conteudoPlanoId, prazo, erro);
            }
        }
    }
}
