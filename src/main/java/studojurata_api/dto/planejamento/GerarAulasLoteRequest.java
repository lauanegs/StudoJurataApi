package studojurata_api.dto.planejamento;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class GerarAulasLoteRequest {
    private Integer quantidade;
    private LocalDate dataInicio;
    private String tituloBase;
}
