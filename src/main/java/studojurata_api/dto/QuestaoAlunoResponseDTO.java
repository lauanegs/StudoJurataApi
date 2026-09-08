package studojurata_api.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class QuestaoAlunoResponseDTO {
    private Long id;
    private Long simuladoAlunoId;
    private Long questaoId;
    private Long alternativaId;
    /** Ids das afirmações que o aluno marcou como Verdadeiras (só questão VERDADEIRO_FALSO). */
    private List<Long> alternativasVerdadeirasIds;
    /** false = questão deixada em branco. Distingue de "respondeu e errou" (ambíguo em V/F com lista vazia). */
    private Boolean respondida;
    private Boolean acertou;
    private Integer tempoResposta;
}
