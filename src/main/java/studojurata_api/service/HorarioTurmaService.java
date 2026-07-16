package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.model.HorarioTurma;
import studojurata_api.model.Turma;
import studojurata_api.repository.HorarioTurmaRepository;
import studojurata_api.repository.TurmaRepository;

import java.util.List;

/**
 * Correção solicitada após a Terceira Análise Crítica: horário semanal da
 * turma — 1 Turma para N HorarioTurma, já que uma turma costuma ter aula em
 * mais de um dia da semana, cada um com seu próprio intervalo de horário.
 */
@Service
@RequiredArgsConstructor
public class HorarioTurmaService {

    private final HorarioTurmaRepository repository;
    private final TurmaRepository turmaRepository;

    public List<HorarioTurma> listarPorTurma(Long turmaId) {
        return repository.findByTurma_Id(turmaId);
    }

    public HorarioTurma adicionar(Long turmaId, HorarioTurma obj) {
        Turma turma = turmaRepository.findById(turmaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Turma " + turmaId + " não encontrada."));
        validar(obj);
        obj.setId(null);
        obj.setTurma(turma);
        validarSemSobreposicao(turmaId, obj);
        return repository.save(obj);
    }

    public void remover(Long id) {
        if (!repository.existsById(id)) {
            throw new RecursoNaoEncontradoException("Horário " + id + " não encontrado.");
        }
        repository.deleteById(id);
    }

    private void validar(HorarioTurma obj) {
        if (obj.getDiaSemana() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dia da semana é obrigatório.");
        }
        if (obj.getHoraInicio() == null || obj.getHoraFim() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hora de início e hora de fim são obrigatórias.");
        }
        if (!obj.getHoraInicio().isBefore(obj.getHoraFim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hora de início deve ser antes da hora de fim.");
        }
    }

    /** Impede cadastrar, para a mesma turma e o mesmo dia, dois horários que se sobrepõem. */
    private void validarSemSobreposicao(Long turmaId, HorarioTurma novo) {
        List<HorarioTurma> doMesmoDia = repository.findByTurma_IdAndDiaSemana(turmaId, novo.getDiaSemana());
        boolean sobrepoe = doMesmoDia.stream().anyMatch(existente ->
                novo.getHoraInicio().isBefore(existente.getHoraFim())
                        && novo.getHoraFim().isAfter(existente.getHoraInicio()));
        if (sobrepoe) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Já existe um horário cadastrado para esta turma neste dia que se sobrepõe ao informado.");
        }
    }
}
