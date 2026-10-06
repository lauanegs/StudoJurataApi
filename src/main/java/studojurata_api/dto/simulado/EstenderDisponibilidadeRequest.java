package studojurata_api.dto.simulado;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class EstenderDisponibilidadeRequest {
    private LocalDateTime dataFim;
}
