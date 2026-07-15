package studojurata_api.dto;

import lombok.Getter;
import lombok.Setter;
import studojurata_api.model.enums.NivelDificuldade;
import studojurata_api.model.enums.OrigemQuestao;
import studojurata_api.model.enums.StatusQuestao;
import studojurata_api.model.enums.TipoQuestao;

@Getter
@Setter
public class QuestaoResponseDTO {
    private Long id;
    private String enunciado;
    private TipoQuestao tipo;
    private Long disciplinaId;
    private NivelDificuldade nivelDificuldade;
    private OrigemQuestao origem;
    private StatusQuestao status;
}
