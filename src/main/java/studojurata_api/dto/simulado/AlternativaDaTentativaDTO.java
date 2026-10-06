package studojurata_api.dto.simulado;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AlternativaDaTentativaDTO {
    private Long id;
    private String texto;
    private Integer ordem;
}
