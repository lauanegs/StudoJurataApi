package studojurata_api.ia.mapper;

import org.springframework.stereotype.Component;
import studojurata_api.ia.dto.HistoricoGeracaoIAResponseDTO;
import studojurata_api.ia.model.HistoricoGeracaoIA;

@Component
public class HistoricoGeracaoIAMapper {

    public HistoricoGeracaoIAResponseDTO toResponseDTO(HistoricoGeracaoIA historico) {
        if (historico == null) return null;
        HistoricoGeracaoIAResponseDTO dto = new HistoricoGeracaoIAResponseDTO();
        dto.setId(historico.getId());
        dto.setConteudoPlanoId(historico.getConteudoPlano() != null ? historico.getConteudoPlano().getId() : null);
        dto.setDisciplinaId(historico.getDisciplina() != null ? historico.getDisciplina().getId() : null);
        dto.setSimuladoId(historico.getSimulado() != null ? historico.getSimulado().getId() : null);
        dto.setNivelDificuldade(historico.getNivelDificuldade());
        dto.setTipoQuestao(historico.getTipoQuestao());
        dto.setQuantidadeSolicitada(historico.getQuantidadeSolicitada());
        dto.setQuantidadeReaproveitadaCache(historico.getQuantidadeReaproveitadaCache());
        dto.setQuantidadeGeradaGemini(historico.getQuantidadeGeradaGemini());
        dto.setQuantidadeFallbackBanco(historico.getQuantidadeFallbackBanco());
        dto.setOrigemResultado(historico.getOrigemResultado());
        dto.setModeloUtilizado(historico.getModeloUtilizado());
        dto.setTempoRespostaMs(historico.getTempoRespostaMs());
        dto.setMensagemErro(historico.getMensagemErro());
        dto.setDataGeracao(historico.getDataGeracao());
        return dto;
    }
}
