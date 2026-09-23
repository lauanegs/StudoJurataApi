package studojurata_api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * Alternativa como o aluno ve durante a prova: <b>sem</b> o campo {@code correta}.
 * O gabarito nunca trafega dentro da alternativa — quando a tentativa esta
 * concluida, ele vai no mapa separado de {@link QuestaoDaTentativaDTO}.
 */
@Getter
@Setter
@AllArgsConstructor
public class AlternativaDaTentativaDTO {

    private Long id;
    private String texto;
    private Integer ordem;
}
