package studojurata_api.ia.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;
import studojurata_api.ia.model.enums.OrigemResultadoGeracao;
import studojurata_api.model.enums.NivelDificuldade;
import studojurata_api.model.enums.TipoQuestao;

@Getter
@Setter
public class HistoricoGeracaoIAResponseDTO {
    private Long id;
    private Long conteudoPlanoId;
    private Long disciplinaId;
    private Long simuladoId;
    private NivelDificuldade nivelDificuldade;
    private TipoQuestao tipoQuestao;
    private Integer quantidadeSolicitada;
    private Integer quantidadeReaproveitadaCache;
    private Integer quantidadeGeradaGemini;
    private Integer quantidadeFallbackBanco;
    private OrigemResultadoGeracao origemResultado;
    private String modeloUtilizado;
    private Long tempoRespostaMs;
    private String mensagemErro;
    private LocalDateTime dataGeracao;
}
