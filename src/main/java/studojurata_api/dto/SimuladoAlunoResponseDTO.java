package studojurata_api.dto;

import lombok.Getter;
import lombok.Setter;
import studojurata_api.model.enums.StatusSimuladoAluno;

@Getter
@Setter
public class SimuladoAlunoResponseDTO {
    private Long id;
    private Long simuladoId;
    private Long alunoId;
    private Integer quantidadeAcertos;
    private Double nota;
    private Integer tempoGasto;
    private Boolean finalizadoPorTempo;
    private StatusSimuladoAluno status;
}
