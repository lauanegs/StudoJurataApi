package studojurata_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.model.enums.NivelDificuldade;
import studojurata_api.model.enums.OrigemQuestao;
import studojurata_api.model.enums.TipoQuestao;

@Getter
@Setter
public class QuestaoRequestDTO {
    @NotBlank(message = "enunciado é obrigatório")
    private String enunciado;
    @NotNull(message = "tipo é obrigatório")
    private TipoQuestao tipo;
    private Long disciplinaId;
    private NivelDificuldade nivelDificuldade;
    private OrigemQuestao origem;
}
