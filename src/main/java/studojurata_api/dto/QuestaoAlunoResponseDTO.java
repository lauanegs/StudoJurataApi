package studojurata_api.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuestaoAlunoResponseDTO {
    private Long id;
    private Long simuladoAlunoId;
    private Long questaoId;
    private Long alternativaId;
    private Boolean acertou;
    private Integer tempoResposta;
}
