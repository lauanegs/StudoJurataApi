package studojurata_api.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import studojurata_api.dto.AlternativaRequestDTO;
import studojurata_api.dto.AlternativaResponseDTO;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.model.Alternativa;
import studojurata_api.model.Questao;
import studojurata_api.repository.QuestaoRepository;

@Component
@RequiredArgsConstructor
public class AlternativaMapper {

    private final QuestaoRepository questaoRepository;

    public Alternativa toEntity(AlternativaRequestDTO dto) {
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

    public AlternativaResponseDTO toResponseDTO(Alternativa alternativa) {
        if (alternativa == null) return null;
        AlternativaResponseDTO dto = new AlternativaResponseDTO();
        dto.setId(alternativa.getId());
        dto.setQuestaoId(alternativa.getQuestao() != null ? alternativa.getQuestao().getId() : null);
        dto.setTexto(alternativa.getTexto());
        dto.setCorreta(alternativa.getCorreta());
        dto.setOrdem(alternativa.getOrdem());
        return dto;
    }
}
