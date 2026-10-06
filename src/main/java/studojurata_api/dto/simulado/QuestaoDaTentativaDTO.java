package studojurata_api.dto.simulado;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

import studojurata_api.model.enums.StatusSimuladoAluno;
import studojurata_api.model.enums.TipoQuestao;

@Getter
@Setter
public class QuestaoDaTentativaDTO {
    private Long simuladoAlunoId;
    private StatusSimuladoAluno status;
    private List<Item> questoes = new ArrayList<>();

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<Long, Boolean> gabarito;

    @Getter
    @Setter
    public static class Item {
        private Long questaoId;
        private String enunciado;
        private TipoQuestao tipo;
        private List<AlternativaDaTentativaDTO> alternativas = new ArrayList<>();

        public Item(Long questaoId, String enunciado, TipoQuestao tipo,
                    List<AlternativaDaTentativaDTO> alternativas) {
            this.questaoId = questaoId;
            this.enunciado = enunciado;
            this.tipo = tipo;
            this.alternativas = alternativas;
        }
    }
}
