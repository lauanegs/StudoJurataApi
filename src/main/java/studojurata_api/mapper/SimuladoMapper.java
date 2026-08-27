package studojurata_api.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import studojurata_api.dto.SimuladoRequestDTO;
import studojurata_api.dto.SimuladoResponseDTO;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.model.Disciplina;
import studojurata_api.model.PlanoEnsino;
import studojurata_api.model.Simulado;
import studojurata_api.model.Turma;
import studojurata_api.repository.DisciplinaRepository;
import studojurata_api.repository.PlanoEnsinoRepository;
import studojurata_api.repository.TurmaRepository;

@Component
@RequiredArgsConstructor
public class SimuladoMapper {

    private final DisciplinaRepository disciplinaRepository;
    private final PlanoEnsinoRepository planoEnsinoRepository;
    private final TurmaRepository turmaRepository;

    public Simulado toEntity(SimuladoRequestDTO dto) {
        if (dto == null) return null;
        Simulado simulado = new Simulado();
        simulado.setTitulo(dto.getTitulo());
        simulado.setTipoDestinacao(dto.getTipoDestinacao());
        simulado.setDataInicio(dto.getDataInicio());
        simulado.setDataFim(dto.getDataFim());
        simulado.setTempoLimite(dto.getTempoLimite());
        simulado.setNotaMaxima(dto.getNotaMaxima());
        simulado.setQuantidadeQuestoes(dto.getQuantidadeQuestoes());

        if (dto.getDisciplinaId() != null) {
            Disciplina disciplina = disciplinaRepository.findById(dto.getDisciplinaId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Disciplina " + dto.getDisciplinaId() + " não encontrada."));
            simulado.setDisciplina(disciplina);
        }
        if (dto.getPlanoEnsinoId() != null) {
            PlanoEnsino planoEnsino = planoEnsinoRepository.findById(dto.getPlanoEnsinoId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Plano de ensino " + dto.getPlanoEnsinoId() + " não encontrado."));
            simulado.setPlanoEnsino(planoEnsino);
        }
        if (dto.getTurmaId() != null) {
            Turma turma = turmaRepository.findById(dto.getTurmaId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Turma " + dto.getTurmaId() + " não encontrada."));
            simulado.setTurma(turma);
        }
        return simulado;
    }

    public SimuladoResponseDTO toResponseDTO(Simulado simulado) {
        if (simulado == null) return null;
        SimuladoResponseDTO dto = new SimuladoResponseDTO();
        dto.setId(simulado.getId());
        dto.setTitulo(simulado.getTitulo());
        dto.setDisciplinaId(simulado.getDisciplina() != null ? simulado.getDisciplina().getId() : null);
        dto.setPlanoEnsinoId(simulado.getPlanoEnsino() != null ? simulado.getPlanoEnsino().getId() : null);
        dto.setTurmaId(simulado.getTurma() != null ? simulado.getTurma().getId() : null);
        dto.setTipoDestinacao(simulado.getTipoDestinacao());
        dto.setDataInicio(simulado.getDataInicio());
        dto.setDataFim(simulado.getDataFim());
        dto.setTempoLimite(simulado.getTempoLimite());
        dto.setNotaMaxima(simulado.getNotaMaxima());
        dto.setQuantidadeQuestoes(simulado.getQuantidadeQuestoes());
        dto.setStatus(simulado.getStatus());
        return dto;
    }
}
