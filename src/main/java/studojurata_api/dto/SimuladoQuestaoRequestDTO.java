package studojurata_api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SimuladoQuestaoRequestDTO {
    @NotNull(message = "simuladoId é obrigatório")
    private Long simuladoId;
    @NotNull(message = "questaoId é obrigatório")
    private Long questaoId;
    private Integer ordem;
    private Double pontuacao;
}
