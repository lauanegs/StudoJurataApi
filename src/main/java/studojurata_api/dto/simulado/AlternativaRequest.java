package studojurata_api.dto.simulado;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AlternativaRequest {
    @NotNull(message = "questaoId é obrigatório")
    private Long questaoId;
    @NotBlank(message = "texto é obrigatório")
    private String texto;
    private Boolean correta;
    private Integer ordem;
}
