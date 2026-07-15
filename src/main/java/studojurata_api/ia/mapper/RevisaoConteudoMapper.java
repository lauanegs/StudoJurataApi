package studojurata_api.ia.mapper;

import org.springframework.stereotype.Component;
import studojurata_api.ia.dto.RevisaoConteudoResponseDTO;
import studojurata_api.ia.model.RevisaoConteudo;

@Component
public class RevisaoConteudoMapper {

    public RevisaoConteudoResponseDTO toResponseDTO(RevisaoConteudo revisao) {
        if (revisao == null) return null;
        RevisaoConteudoResponseDTO dto = new RevisaoConteudoResponseDTO();
        dto.setId(revisao.getId());
        dto.setAlunoId(revisao.getAluno() != null ? revisao.getAluno().getId() : null);
        dto.setConteudoPlanoId(revisao.getConteudoPlano() != null ? revisao.getConteudoPlano().getId() : null);
        dto.setQuantidadeReforcos(revisao.getQuantidadeReforcos());
        dto.setDataUltimoReforco(revisao.getDataUltimoReforco());
        dto.setDataProximoReforco(revisao.getDataProximoReforco());
        dto.setNivelDominio(revisao.getNivelDominio());
        return dto;
    }
}
