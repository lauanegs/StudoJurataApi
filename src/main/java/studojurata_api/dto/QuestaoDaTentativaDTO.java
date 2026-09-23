package studojurata_api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

import studojurata_api.model.enums.StatusSimuladoAluno;
import studojurata_api.model.enums.TipoQuestao;

/**
 * Conteudo da prova de uma tentativa, para o proprio aluno.
 *
 * <p>Durante {@code PENDENTE} o payload traz apenas o necessario para responder:
 * id, enunciado, tipo e alternativas (id, texto, ordem) — <b>sem</b> {@code correta}
 * e com {@code gabarito} nulo. Em {@code CONCLUIDO} o gabarito aparece separado,
 * como mapa {@code alternativaId -> correta}, para a tela de resultado colorir a
 * correcao sem reabrir o campo na alternativa.
 */
@Getter
@Setter
public class QuestaoDaTentativaDTO {

    private Long simuladoAlunoId;
    private StatusSimuladoAluno status;
    private List<Item> questoes = new ArrayList<>();

    /** Preenchido somente quando a tentativa esta concluida; nulo caso contrario. */
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
