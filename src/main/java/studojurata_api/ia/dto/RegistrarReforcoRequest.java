package studojurata_api.ia.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.ia.model.enums.NivelDominio;

@Getter
@Setter
public class RegistrarReforcoRequest {
    @NotNull(message = "alunoId é obrigatório")
    private Long alunoId;
    @NotNull(message = "conteudoPlanoId é obrigatório")
    private Long conteudoPlanoId;
    private NivelDominio nivelDominio;
}
