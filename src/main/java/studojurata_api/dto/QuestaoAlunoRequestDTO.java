package studojurata_api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class QuestaoAlunoRequestDTO {
    @NotNull(message = "simuladoAlunoId é obrigatório")
    private Long simuladoAlunoId;
    @NotNull(message = "questaoId é obrigatório")
    private Long questaoId;
    /** Usado quando a questão é do tipo ALTERNATIVAS. */
    private Long alternativaId;
    /** Usado quando a questão é do tipo VERDADEIRO_FALSO: ids das afirmações marcadas Verdadeiras. */
    private List<Long> alternativasVerdadeirasIds;
    private Boolean respondida;
    private Boolean acertou;
    private Integer tempoResposta;
}
