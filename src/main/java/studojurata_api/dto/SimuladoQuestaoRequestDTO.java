package studojurata_api.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SimuladoQuestaoRequestDTO {
    private Long simuladoId;
    private Long questaoId;
    private Integer ordem;
    private Double pontuacao;
}
