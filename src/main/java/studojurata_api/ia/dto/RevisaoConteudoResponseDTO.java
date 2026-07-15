package studojurata_api.ia.dto;

import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;
import studojurata_api.ia.model.enums.NivelDominio;

@Getter
@Setter
public class RevisaoConteudoResponseDTO {
    private Long id;
    private Long alunoId;
    private Long conteudoPlanoId;
    private Integer quantidadeReforcos;
    private LocalDate dataUltimoReforco;
    private LocalDate dataProximoReforco;
    private NivelDominio nivelDominio;
}
