package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import studojurata_api.model.Turma;
import studojurata_api.model.enums.StatusMatricula;
import studojurata_api.repository.AlunoTurmaRepository;
import studojurata_api.repository.TurmaRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TurmaService {

    private final TurmaRepository repository;
    private final AlunoTurmaRepository alunoTurmaRepository;

    public List<Turma> listar() { return repository.findAll(); }

    public Turma buscar(Long id) { return repository.findById(id).orElseThrow(); }

    public Turma salvar(Turma obj) {
        validarCapacidadeMaxima(obj);
        return repository.save(obj);
    }

    public Turma atualizar(Long id, Turma obj) {
        validarCapacidadeMaxima(obj);
        obj.setId(id);

        // Se a capacidade máxima está sendo reduzida abaixo da quantidade de
        // alunos já ativos, alertamos em vez de permitir uma turma "estourada"
        // silenciosamente.
        if (obj.getCapacidadeMaxima() != null) {
            long ativos = alunoTurmaRepository.countByTurmaIdAndStatus(id, StatusMatricula.ATIVA);
            if (ativos > obj.getCapacidadeMaxima()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Não é possível reduzir a capacidade máxima para " + obj.getCapacidadeMaxima()
                                + ": a turma já possui " + ativos + " aluno(s) com matrícula ativa.");
            }
        }

        return repository.save(obj);
    }

    /** Quantidade de alunos com matrícula ativa na turma, sempre derivada das matrículas (nunca persistida em Turma). */
    public long contarAlunosAtivos(Long turmaId) {
        return alunoTurmaRepository.countByTurmaIdAndStatus(turmaId, StatusMatricula.ATIVA);
    }

    public void deletar(Long id) { repository.deleteById(id); }

    private void validarCapacidadeMaxima(Turma obj) {
        if (obj.getCapacidadeMaxima() != null && obj.getCapacidadeMaxima() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Capacidade máxima deve ser maior que zero.");
        }
    }
}