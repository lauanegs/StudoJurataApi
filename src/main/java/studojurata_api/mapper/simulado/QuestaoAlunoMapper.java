package studojurata_api.mapper.simulado;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import studojurata_api.dto.simulado.QuestaoAlunoResponse;
import studojurata_api.model.simulado.Alternativa;
import studojurata_api.model.simulado.QuestaoAluno;

import java.util.List;

@Component
@RequiredArgsConstructor
public class QuestaoAlunoMapper {

    public QuestaoAlunoResponse toResponse(QuestaoAluno questaoAluno) {
        if (questaoAluno == null) return null;
        QuestaoAlunoResponse dto = new QuestaoAlunoResponse();
        dto.setId(questaoAluno.getId());
        dto.setSimuladoAlunoId(questaoAluno.getSimuladoAluno() != null ? questaoAluno.getSimuladoAluno().getId() : null);
        dto.setQuestaoId(questaoAluno.getQuestao() != null ? questaoAluno.getQuestao().getId() : null);
        dto.setAlternativaId(questaoAluno.getAlternativa() != null ? questaoAluno.getAlternativa().getId() : null);
        dto.setAlternativasVerdadeirasIds(questaoAluno.getAlternativasVerdadeiras() == null
                ? List.of()
                : questaoAluno.getAlternativasVerdadeiras().stream().map(Alternativa::getId).toList());
        dto.setRespondida(questaoAluno.getRespondida());
        dto.setAcertou(questaoAluno.getAcertou());
        dto.setTempoResposta(questaoAluno.getTempoResposta());
        return dto;
    }
}
