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
 * Geração automática do simulado de reforço por repetição espaçada — a peça
 * "job futuro" citada no Javadoc de RecomendacaoService (item 1.4 da Análise
 * Crítica: "os simulados gerados pela plataforma a partir da IA não
 * precisarão de autorização do professor para serem gerados"). Confirmado
 * pelo usuário: nada de botão/tela manual — assim que a data de repetição
 * espaçada de um aluno+conteúdo vence, o próprio sistema chama
 * GeracaoSimuladoIAService, sem esperar ninguém abrir uma tela.
 *
 * Só cobre o motivo REPETICAO_ESPACADA (tem data — RevisaoConteudo.
 * dataProximoReforco). BAIXO_APROVEITAMENTO (RecomendacaoService) não tem
 * agenda própria — é um limiar avaliado em tempo real quando alguém consulta
 * as recomendações do aluno — então fica fora do escopo deste job por
 * enquanto (não há "data em que venceu" pra disparar sozinho).
 *
 * A cada execução, cada revisão devida ganha sua própria transação (dentro
 * de GeracaoSimuladoIAService.gerarParaAluno) e sua própria captura de erro —
 * uma falha isolada (aluno/conteúdo excluído no meio do caminho, etc.) não
 * pode derrubar a geração dos demais.
 */
@Component
@RequiredArgsConstructor
public class GeracaoAutomaticaSimuladoJob {

    private static final Logger log = LoggerFactory.getLogger(GeracaoAutomaticaSimuladoJob.class);

    private final RevisaoConteudoRepository revisaoConteudoRepository;
    private final SimuladoGeradoIARepository simuladoGeradoIARepository;
    private final GeracaoSimuladoIAService geracaoSimuladoIAService;

    /**
     * Uma vez por dia, de madrugada — a geração em si (chamada real ao
     * Gemini) não precisa de tempo real, só rodar antes do professor entrar
     * pela manhã. Configurável via studojurata.ia.geracao-automatica.cron
     * (application.properties) pra quem precisar de outra janela.
     */
    @Scheduled(cron = "${studojurata.ia.geracao-automatica.cron:0 0 5 * * *}")
    public void gerarSimuladosDevidos() {
        List<RevisaoConteudo> devidos = revisaoConteudoRepository.findByDataProximoReforcoLessThanEqual(LocalDate.now());

        for (RevisaoConteudo revisao : devidos) {
            Long alunoId = revisao.getAluno().getId();
            Long conteudoPlanoId = revisao.getConteudoPlano().getId();
            LocalDate prazo = revisao.getDataProximoReforco();

            // Já gerado pra este mesmo ciclo — dataProximoReforco só avança
            // quando o professor revisa (RevisaoConteudoService.registrarReforco),
            // então sem isto o job geraria um simulado novo por execução
            // enquanto o rascunho anterior ficar esperando revisão.
            boolean jaGerado = simuladoGeradoIARepository
                    .existsByAlunoIdAndConteudoPlanoIdAndPrazoLancamento(alunoId, conteudoPlanoId, prazo);
            if (jaGerado) continue;

            try {
                geracaoSimuladoIAService.gerarParaAluno(
                        alunoId, conteudoPlanoId, null, Set.of(MotivoRecomendacao.REPETICAO_ESPACADA));
            } catch (RuntimeException erro) {
                // Isolado por revisão: aluno/conteúdo removido nesse meio-tempo,
                // ou qualquer outra falha inesperada — não deve impedir a
                // geração das demais revisões devidas nesta execução.
                log.error(
                        "Falha ao gerar simulado automático (aluno {}, conteúdo {}, prazo {})",
                        alunoId, conteudoPlanoId, prazo, erro);
            }
        }
    }
}
