package studojurata_api.ia.dto;

import lombok.Getter;
import lombok.Setter;
import studojurata_api.ia.model.enums.MotivoRecomendacao;
import studojurata_api.model.enums.NivelDificuldade;

import java.time.LocalDate;
import java.util.Set;

@Getter
@Setter
public class RecomendacaoDTO {
    private Long alunoId;
    private Long conteudoPlanoId;
    private String conteudoTitulo;
    private Set<MotivoRecomendacao> motivos;
    private Double taxaAcerto;
    private NivelDificuldade nivelPrioritario;
    private LocalDate dataProximoReforco;
}
