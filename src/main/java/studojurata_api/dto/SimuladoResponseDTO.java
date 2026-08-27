package studojurata_api.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;
import studojurata_api.model.enums.StatusSimulado;
import studojurata_api.model.enums.TipoDestinacaoSimulado;

@Getter
@Setter
public class SimuladoResponseDTO {
    private Long id;
    private String titulo;
    private Long disciplinaId;
    private Long planoEnsinoId;
    private Long turmaId;
    private TipoDestinacaoSimulado tipoDestinacao;
    private LocalDateTime dataInicio;
    private LocalDateTime dataFim;
    private Integer tempoLimite;
    private Double notaMaxima;
    private Integer quantidadeQuestoes;
    private StatusSimulado status;
}
