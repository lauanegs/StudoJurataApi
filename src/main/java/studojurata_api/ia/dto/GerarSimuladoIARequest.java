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
    private NivelDificuldade nivelDificuldade;
    private Set<MotivoRecomendacao> motivos;
}
