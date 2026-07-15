package studojurata_api.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import studojurata_api.dto.QuestaoAlunoRequestDTO;
import studojurata_api.dto.QuestaoAlunoResponseDTO;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.model.Alternativa;
import studojurata_api.model.Questao;
import studojurata_api.model.QuestaoAluno;
import studojurata_api.model.SimuladoAluno;
import studojurata_api.repository.AlternativaRepository;
import studojurata_api.repository.QuestaoRepository;
import studojurata_api.repository.SimuladoAlunoRepository;

@Component
@RequiredArgsConstructor
public class QuestaoAlunoMapper {

    private final SimuladoAlunoRepository simuladoAlunoRepository;
    private final QuestaoRepository questaoRepository;
    private final AlternativaRepository alternativaRepository;

    public QuestaoAluno toEntity(QuestaoAlunoRequestDTO dto) {
        if (dto == null) return null;
        QuestaoAluno questaoAluno = new QuestaoAluno();
        questaoAluno.setAcertou(dto.getAcertou());
        questaoAluno.setTempoResposta(dto.getTempoResposta());
        if (dto.getSimuladoAlunoId() != null) {
            SimuladoAluno simuladoAluno = simuladoAlunoRepository.findById(dto.getSimuladoAlunoId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("SimuladoAluno " + dto.getSimuladoAlunoId() + " não encontrado."));
            questaoAluno.setSimuladoAluno(simuladoAluno);
        }
        if (dto.getQuestaoId() != null) {
            Questao questao = questaoRepository.findById(dto.getQuestaoId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Questão " + dto.getQuestaoId() + " não encontrada."));
            questaoAluno.setQuestao(questao);
        }
        if (dto.getAlternativaId() != null) {
            Alternativa alternativa = alternativaRepository.findById(dto.getAlternativaId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Alternativa " + dto.getAlternativaId() + " não encontrada."));
            questaoAluno.setAlternativa(alternativa);
        }
        return questaoAluno;
    }

    public QuestaoAlunoResponseDTO toResponseDTO(QuestaoAluno questaoAluno) {
        if (questaoAluno == null) return null;
        QuestaoAlunoResponseDTO dto = new QuestaoAlunoResponseDTO();
        dto.setId(questaoAluno.getId());
        dto.setSimuladoAlunoId(questaoAluno.getSimuladoAluno() != null ? questaoAluno.getSimuladoAluno().getId() : null);
        dto.setQuestaoId(questaoAluno.getQuestao() != null ? questaoAluno.getQuestao().getId() : null);
        dto.setAlternativaId(questaoAluno.getAlternativa() != null ? questaoAluno.getAlternativa().getId() : null);
        dto.setAcertou(questaoAluno.getAcertou());
        dto.setTempoResposta(questaoAluno.getTempoResposta());
        return dto;
    }
}
