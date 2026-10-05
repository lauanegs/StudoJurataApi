package studojurata_api.mapper.simulado;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import studojurata_api.dto.simulado.SimuladoQuestaoRequestDTO;
import studojurata_api.dto.simulado.SimuladoQuestaoResponseDTO;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.model.simulado.Questao;
import studojurata_api.model.simulado.Simulado;
import studojurata_api.model.simulado.SimuladoQuestao;
import studojurata_api.repository.simulado.QuestaoRepository;
import studojurata_api.repository.simulado.SimuladoRepository;

@Component
@RequiredArgsConstructor
public class SimuladoQuestaoMapper {

    private final SimuladoRepository simuladoRepository;
    private final QuestaoRepository questaoRepository;

    public SimuladoQuestao toEntity(SimuladoQuestaoRequestDTO dto) {
        if (dto == null) return null;
        SimuladoQuestao simuladoQuestao = new SimuladoQuestao();
        simuladoQuestao.setOrdem(dto.getOrdem());
        simuladoQuestao.setPontuacao(dto.getPontuacao());
        if (dto.getSimuladoId() != null) {
            Simulado simulado = simuladoRepository.findById(dto.getSimuladoId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Simulado " + dto.getSimuladoId() + " não encontrado."));
            simuladoQuestao.setSimulado(simulado);
        }
        if (dto.getQuestaoId() != null) {
            Questao questao = questaoRepository.findById(dto.getQuestaoId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Questão " + dto.getQuestaoId() + " não encontrada."));
            simuladoQuestao.setQuestao(questao);
        }
        return simuladoQuestao;
    }

    public SimuladoQuestaoResponseDTO toResponseDTO(SimuladoQuestao simuladoQuestao) {
        if (simuladoQuestao == null) return null;
        SimuladoQuestaoResponseDTO dto = new SimuladoQuestaoResponseDTO();
        dto.setId(simuladoQuestao.getId());
        dto.setSimuladoId(simuladoQuestao.getSimulado() != null ? simuladoQuestao.getSimulado().getId() : null);
        dto.setQuestaoId(simuladoQuestao.getQuestao() != null ? simuladoQuestao.getQuestao().getId() : null);
        dto.setOrdem(simuladoQuestao.getOrdem());
        dto.setPontuacao(simuladoQuestao.getPontuacao());
        dto.setStatus(simuladoQuestao.getStatus());
        return dto;
    }
}
