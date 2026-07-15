package studojurata_api.ia.dto;

import lombok.Getter;
import lombok.Setter;
import studojurata_api.model.enums.NivelDificuldade;

@Getter
@Setter
public class GerarSimuladoIARequest {
    private Long alunoId;
    private Long conteudoPlanoId;
    private Integer quantidadeQuestoes;
    private NivelDificuldade nivelDificuldade;
}
