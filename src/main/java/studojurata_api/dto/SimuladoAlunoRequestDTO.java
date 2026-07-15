package studojurata_api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SimuladoAlunoRequestDTO {
    @NotNull(message = "simuladoId é obrigatório")
    private Long simuladoId;
    @NotNull(message = "alunoId é obrigatório")
    private Long alunoId;
}
