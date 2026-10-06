package studojurata_api.dto.simulado;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class LancarSimuladoRequest {
    private List<Long> alunoIds;
}
