package studojurata_api.ia.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import studojurata_api.ia.dto.AlternativaGeradaDTO;
import studojurata_api.ia.dto.GeminiQuestaoGeradaDTO;
import studojurata_api.model.enums.NivelDificuldade;
import studojurata_api.model.enums.TipoQuestao;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementação real da integração com o Gemini (Google Generative Language
 * API), usando java.net.http.HttpClient (nativo do JDK, sem depender de
 * nenhuma biblioteca HTTP adicional no classpath) e o ObjectMapper padrão do
 * Spring Boot para montar/parsear JSON.
 *
 * Configuração (application.properties — ver README do módulo de IA):
 *   studojurata.ia.gemini.api-key=...
 *   studojurata.ia.gemini.model=gemini-1.5-flash
 *   studojurata.ia.gemini.base-url=https://generativelanguage.googleapis.com/v1beta/models
 *   studojurata.ia.gemini.timeout-ms=8000
 *
 * Sem api-key configurada, gerarQuestoes falha imediatamente com
 * GeminiIndisponivelException — o que já aciona o fallback em
 * GeracaoQuestaoIAService, permitindo rodar o backend em ambiente de
 * desenvolvimento/demonstração sem uma chave real (ver item 9.3: o projeto é
 * um MVP, a integração de fato com custos/tokens de produção fica para uma
 * etapa futura).
 */
@Component
@RequiredArgsConstructor
public class GeminiApiClient implements GeminiQuestaoClient {

    private final ObjectMapper objectMapper;

    @Value("${studojurata.ia.gemini.api-key:}")
    private String apiKey;

    @Value("${studojurata.ia.gemini.model:gemini-1.5-flash}")
    private String modelo;

    @Value("${studojurata.ia.gemini.base-url:https://generativelanguage.googleapis.com/v1beta/models}")
    private String baseUrl;

    @Value("${studojurata.ia.gemini.timeout-ms:8000}")
    private long timeoutMs;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Override
    public String getModelo() {
        return modelo;
    }

    @Override
    public List<GeminiQuestaoGeradaDTO> gerarQuestoes(String conteudoTexto, NivelDificuldade nivel, TipoQuestao tipo, int quantidade) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new GeminiIndisponivelException("Chave de API do Gemini não configurada (studojurata.ia.gemini.api-key).");
        }
        if (conteudoTexto == null || conteudoTexto.isBlank()) {
            throw new GeminiIndisponivelException("Conteúdo vazio: não é possível gerar questões sem um texto-base.");
        }

        try {
            String prompt = montarPrompt(conteudoTexto, nivel, tipo, quantidade);
            String corpoRequisicao = montarCorpoRequisicao(prompt);

            URI uri = URI.create(baseUrl + "/" + modelo + ":generateContent?key=" + apiKey);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(Duration.ofMillis(timeoutMs))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(corpoRequisicao))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new GeminiIndisponivelException(
                        "Gemini retornou status " + response.statusCode() + ": " + response.body());
            }

            return extrairQuestoes(response.body());
        } catch (GeminiIndisponivelException e) {
            throw e;
        } catch (Exception e) {
            throw new GeminiIndisponivelException("Falha ao chamar a API do Gemini: " + e.getMessage(), e);
        }
    }

    private String montarPrompt(String conteudoTexto, NivelDificuldade nivel, TipoQuestao tipo, int quantidade) {
        String instrucaoTipo = tipo == TipoQuestao.VERDADEIRO_FALSO
                ? "Cada questão deve ter exatamente 2 alternativas, com os textos \"Verdadeiro\" e \"Falso\", e apenas uma marcada como correta."
                : "Cada questão deve ter exatamente 3 alternativas (curtas, plausíveis e mutuamente exclusivas), com apenas uma marcada como correta.";
        String nivelTexto = nivel != null ? nivel.name() : "MEDIA";

        return "Você é um assistente pedagógico especializado em elaborar questões de avaliação para o ensino médio. "
                + "Gere " + quantidade + " questões de múltipla escolha em português do Brasil, nível de dificuldade "
                + nivelTexto + ", estritamente baseadas no conteúdo abaixo, sem inventar informações fora dele. "
                + instrucaoTipo + " "
                + "Responda APENAS com um array JSON válido, sem markdown, sem comentários e sem texto adicional, "
                + "no formato exato: "
                + "[{\"enunciado\": \"...\", \"alternativas\": [{\"texto\": \"...\", \"correta\": true}, {\"texto\": \"...\", \"correta\": false}]}]"
                + "\n\nConteúdo:\n" + conteudoTexto;
    }

    private String montarCorpoRequisicao(String prompt) throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode contents = root.putArray("contents");
        ObjectNode content = contents.addObject();
        ArrayNode parts = content.putArray("parts");
        parts.addObject().put("text", prompt);

        ObjectNode generationConfig = root.putObject("generationConfig");
        generationConfig.put("responseMimeType", "application/json");

        return objectMapper.writeValueAsString(root);
    }

    private List<GeminiQuestaoGeradaDTO> extrairQuestoes(String corpoResposta) throws Exception {
        JsonNode raiz = objectMapper.readTree(corpoResposta);
        JsonNode candidatos = raiz.path("candidates");
        if (!candidatos.isArray() || candidatos.isEmpty()) {
            throw new GeminiIndisponivelException("Resposta do Gemini sem candidatos: " + corpoResposta);
        }
        JsonNode texto = candidatos.get(0).path("content").path("parts").get(0).path("text");
        if (texto.isMissingNode() || texto.asText().isBlank()) {
            throw new GeminiIndisponivelException("Resposta do Gemini sem conteúdo textual: " + corpoResposta);
        }

        JsonNode arrayQuestoes = objectMapper.readTree(texto.asText());
        List<GeminiQuestaoGeradaDTO> resultado = new ArrayList<>();
        if (!arrayQuestoes.isArray()) {
            throw new GeminiIndisponivelException("Formato inesperado no texto retornado pelo Gemini (esperava um array JSON).");
        }
        for (JsonNode nodeQuestao : arrayQuestoes) {
            GeminiQuestaoGeradaDTO dto = new GeminiQuestaoGeradaDTO();
            dto.setEnunciado(nodeQuestao.path("enunciado").asText());
            List<AlternativaGeradaDTO> alternativas = new ArrayList<>();
            for (JsonNode nodeAlternativa : nodeQuestao.path("alternativas")) {
                AlternativaGeradaDTO alt = new AlternativaGeradaDTO();
                alt.setTexto(nodeAlternativa.path("texto").asText());
                alt.setCorreta(nodeAlternativa.path("correta").asBoolean(false));
                alternativas.add(alt);
            }
            dto.setAlternativas(alternativas);
            resultado.add(dto);
        }
        if (resultado.isEmpty()) {
            throw new GeminiIndisponivelException("Gemini não retornou nenhuma questão.");
        }
        return resultado;
    }
}
