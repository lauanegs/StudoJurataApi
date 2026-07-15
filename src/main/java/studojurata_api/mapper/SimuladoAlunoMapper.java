package studojurata_api.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import studojurata_api.dto.SimuladoAlunoRequestDTO;
import studojurata_api.dto.SimuladoAlunoResponseDTO;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.model.Aluno;
import studojurata_api.model.Simulado;
import studojurata_api.model.SimuladoAluno;
import studojurata_api.repository.AlunoRepository;
import studojurata_api.repository.SimuladoRepository;

@Component
@RequiredArgsConstructor
public class SimuladoAlunoMapper {

    private final SimuladoRepository simuladoRepository;
    private final AlunoRepository alunoRepository;

    public SimuladoAluno toEntity(SimuladoAlunoRequestDTO dto) {
        if (dto == null) return null;
        SimuladoAluno simuladoAluno = new SimuladoAluno();
        if (dto.getSimuladoId() != null) {
            Simulado simulado = simuladoRepository.findById(dto.getSimuladoId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Simulado " + dto.getSimuladoId() + " não encontrado."));
            simuladoAluno.setSimulado(simulado);
        }
        if (dto.getAlunoId() != null) {
            Aluno aluno = alunoRepository.findById(dto.getAlunoId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Aluno " + dto.getAlunoId() + " não encontrado."));
            simuladoAluno.setAluno(aluno);
        }
        return simuladoAluno;
    }

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
