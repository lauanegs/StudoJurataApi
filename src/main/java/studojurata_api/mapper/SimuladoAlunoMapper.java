package studojurata_api.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import studojurata_api.dto.SimuladoAlunoResponseDTO;
import studojurata_api.model.SimuladoAluno;

@Component
@RequiredArgsConstructor
public class SimuladoAlunoMapper {

    public SimuladoAlunoResponseDTO toResponseDTO(SimuladoAluno simuladoAluno) {
        if (simuladoAluno == null) return null;
        SimuladoAlunoResponseDTO dto = new SimuladoAlunoResponseDTO();
        dto.setId(simuladoAluno.getId());
        dto.setSimuladoId(simuladoAluno.getSimulado() != null ? simuladoAluno.getSimulado().getId() : null);
        dto.setAlunoId(simuladoAluno.getAluno() != null ? simuladoAluno.getAluno().getId() : null);
        dto.setQuantidadeAcertos(simuladoAluno.getQuantidadeAcertos());
        dto.setNota(simuladoAluno.getNota());
        dto.setTempoGasto(simuladoAluno.getTempoGasto());
        dto.setFinalizadoPorTempo(simuladoAluno.getFinalizadoPorTempo());
        dto.setStatus(simuladoAluno.getStatus());
        return dto;
    }
}
