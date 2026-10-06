package studojurata_api.mapper.simulado;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import studojurata_api.dto.simulado.AlternativaRequest;
import studojurata_api.dto.simulado.AlternativaResponse;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.model.simulado.Alternativa;
import studojurata_api.model.simulado.Questao;
import studojurata_api.repository.simulado.QuestaoRepository;

@Component
@RequiredArgsConstructor
public class AlternativaMapper {

    private final QuestaoRepository questaoRepository;

    public Alternativa toEntity(AlternativaRequest dto) {
        if (dto == null) return null;
        Alternativa alternativa = new Alternativa();
        alternativa.setTexto(dto.getTexto());
        alternativa.setCorreta(dto.getCorreta());
        alternativa.setOrdem(dto.getOrdem());
        if (dto.getQuestaoId() != null) {
            Questao questao = questaoRepository.findById(dto.getQuestaoId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Questão " + dto.getQuestaoId() + " não encontrada."));
            alternativa.setQuestao(questao);
        }
        return alternativa;
    }

    public AlternativaResponse toResponse(Alternativa alternativa) {
        if (alternativa == null) return null;
        AlternativaResponse dto = new AlternativaResponse();
        dto.setId(alternativa.getId());
        dto.setQuestaoId(alternativa.getQuestao() != null ? alternativa.getQuestao().getId() : null);
        dto.setTexto(alternativa.getTexto());
        dto.setCorreta(alternativa.getCorreta());
        dto.setOrdem(alternativa.getOrdem());
        return dto;
    }
}
