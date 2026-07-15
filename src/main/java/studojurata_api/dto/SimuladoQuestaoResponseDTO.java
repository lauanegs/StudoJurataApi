package studojurata_api.dto;

import lombok.Getter;
import lombok.Setter;
import studojurata_api.model.enums.StatusSimuladoQuestao;

@Getter
@Setter
public class SimuladoQuestaoResponseDTO {
    private Long id;
    private Long simuladoId;
    private Long questaoId;
    private Integer ordem;
    private Double pontuacao;
    private StatusSimuladoQuestao status;
}
