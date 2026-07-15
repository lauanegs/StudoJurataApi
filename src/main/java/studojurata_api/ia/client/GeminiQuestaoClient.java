package studojurata_api.ia.client;

import studojurata_api.ia.dto.GeminiQuestaoGeradaDTO;
import studojurata_api.model.enums.NivelDificuldade;
import studojurata_api.model.enums.TipoQuestao;

import java.util.List;

/**
 * Integração com o Gemini (item "integração Gemini" desta etapa), isolada em
 * uma interface própria (ver item 3.3 da Análise Crítica: "isolar em um
 * serviço próprio... com fallback para banco de questões existente quando a
 * IA falhar") para que GeracaoQuestaoIAService nunca dependa diretamente de
 * detalhes de transporte HTTP e possa ser testado com um dublê/mock.
 */
public interface GeminiQuestaoClient {

    /**
     * @throws GeminiIndisponivelException quando a geração não pôde ser concluída
     *         (chave ausente, timeout, erro HTTP, resposta inesperada).
     */
    List<GeminiQuestaoGeradaDTO> gerarQuestoes(String conteudoTexto, NivelDificuldade nivel, TipoQuestao tipo, int quantidade);

    /** Identificador do modelo configurado (para auditoria em HistoricoGeracaoIA). */
    String getModelo();
}
