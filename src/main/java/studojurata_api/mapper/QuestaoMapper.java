package studojurata_api.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import studojurata_api.dto.QuestaoRequestDTO;
import studojurata_api.dto.QuestaoResponseDTO;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.model.Disciplina;
import studojurata_api.model.Questao;
import studojurata_api.repository.DisciplinaRepository;

@Component
@RequiredArgsConstructor
public class QuestaoMapper {

    private final DisciplinaRepository disciplinaRepository;

    /**
     * Nao copia {@code origem}: quem decide origem e status de moderacao e o
     * servidor ({@code QuestaoService.salvar}/{@code atualizar}). O campo
     * continua aceito no corpo para nao quebrar o cliente, mas e ignorado.
     */
    public Questao toEntity(QuestaoRequestDTO dto) {
        if (dto == null) return null;
        Questao questao = new Questao();
        questao.setEnunciado(dto.getEnunciado());
        questao.setTipo(dto.getTipo());
        questao.setNivelDificuldade(dto.getNivelDificuldade());
        if (dto.getDisciplinaId() != null) {
            Disciplina disciplina = disciplinaRepository.findById(dto.getDisciplinaId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Disciplina " + dto.getDisciplinaId() + " não encontrada."));
            questao.setDisciplina(disciplina);
        }
        return questao;
    }

    public QuestaoResponseDTO toResponseDTO(Questao questao) {
        if (questao == null) return null;
        QuestaoResponseDTO dto = new QuestaoResponseDTO();
        dto.setId(questao.getId());
        dto.setEnunciado(questao.getEnunciado());
        dto.setTipo(questao.getTipo());
        dto.setDisciplinaId(questao.getDisciplina() != null ? questao.getDisciplina().getId() : null);
        dto.setNivelDificuldade(questao.getNivelDificuldade());
        dto.setOrigem(questao.getOrigem());
        dto.setStatus(questao.getStatus());
        return dto;
    }
}
