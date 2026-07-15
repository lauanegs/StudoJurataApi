package studojurata_api.ia.dto;

import lombok.Getter;
import lombok.Setter;
import studojurata_api.ia.model.enums.NivelDominio;

@Getter
@Setter
public class RegistrarReforcoRequest {
    private Long alunoId;
    private Long conteudoPlanoId;
    private NivelDominio nivelDominio;
}
