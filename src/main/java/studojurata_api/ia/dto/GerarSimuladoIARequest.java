package studojurata_api.ia.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.ia.model.enums.MotivoRecomendacao;
import studojurata_api.model.enums.NivelDificuldade;

import java.util.Set;

@Getter
@Setter
public class GerarSimuladoIARequest {
    @NotNull(message = "alunoId é obrigatório")
    private Long alunoId;
    @NotNull(message = "conteudoPlanoId é obrigatório")
    private Long conteudoPlanoId;
    /**
     * Sem campo de quantidade: confirmado pelo usuário que todo simulado
     * gerado por IA é padronizado em GeracaoSimuladoIAService.QUANTIDADE_QUESTOES
     * (5) — nada aqui ou no front pedia um valor diferente até então.
     */
    private NivelDificuldade nivelDificuldade;
    /** Opcional — motivo(s) da RecomendacaoDTO que originou esta chamada, quando houver (ver SimuladoGeradoIA). */
    private Set<MotivoRecomendacao> motivos;
}
