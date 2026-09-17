package studojurata_api.ia.dto;

import lombok.Getter;
import lombok.Setter;
import studojurata_api.ia.model.enums.MotivoRecomendacao;

import java.time.LocalDate;
import java.util.Set;

@Getter
@Setter
public class SimuladoGeradoIAResponseDTO {
    private Long simuladoId;
    private Long alunoId;
    private Long conteudoPlanoId;
    private String conteudoTitulo;
    private Set<MotivoRecomendacao> motivos;
    /** Prazo pra lançar este simulado — ver SimuladoGeradoIA.prazoLancamento. */
    private LocalDate prazoLancamento;
}
