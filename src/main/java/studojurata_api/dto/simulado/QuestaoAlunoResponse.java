package studojurata_api.dto.simulado;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class QuestaoAlunoResponse {
    private Long id;
    private Long simuladoAlunoId;
    private Long questaoId;
    private Long alternativaId;
    private List<Long> alternativasVerdadeirasIds;
    private Boolean respondida;
    private Boolean acertou;
    private Integer tempoResposta;
}
