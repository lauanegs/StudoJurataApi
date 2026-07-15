package studojurata_api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuestaoAlunoRequestDTO {
    @NotNull(message = "simuladoAlunoId é obrigatório")
    private Long simuladoAlunoId;
    @NotNull(message = "questaoId é obrigatório")
    private Long questaoId;
    private Long alternativaId;
    private Boolean acertou;
    private Integer tempoResposta;
}
