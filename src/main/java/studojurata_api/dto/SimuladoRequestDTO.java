package studojurata_api.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.model.enums.TipoDestinacaoSimulado;

@Getter
@Setter
public class SimuladoRequestDTO {
    @NotBlank(message = "titulo é obrigatório")
    private String titulo;
    private Long disciplinaId;
    private Long planoEnsinoId;
    private Long turmaId;
    @NotNull(message = "tipoDestinacao é obrigatório")
    private TipoDestinacaoSimulado tipoDestinacao;
    private LocalDateTime dataInicio;
    private LocalDateTime dataFim;
    private Integer tempoLimite;
    private Double notaMaxima;
    private Integer quantidadeQuestoes;
}
