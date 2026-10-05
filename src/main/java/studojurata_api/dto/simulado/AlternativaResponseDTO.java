package studojurata_api.dto.simulado;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AlternativaResponseDTO {
    private Long id;
    private Long questaoId;
    private String texto;
    private Boolean correta;
    private Integer ordem;
}
