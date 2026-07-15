package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import studojurata_api.model.AlunoTurma;
import studojurata_api.model.Turma;
import studojurata_api.model.enums.StatusMatricula;
import studojurata_api.repository.AlunoTurmaRepository;
import studojurata_api.repository.TurmaRepository;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AlunoTurmaService {

    private final AlunoTurmaRepository repository;
    private final TurmaRepository turmaRepository;

    public List<AlunoTurma> listar() { return repository.findAll(); }

    public AlunoTurma buscar(Long id) { return repository.findById(id).orElseThrow(); }

    /** Lista o histórico completo de matrículas de uma turma (ativas ou não). */
    public List<AlunoTurma> historicoPorTurma(Long turmaId) {
        return repository.findByTurmaIdOrderByDataInicioDesc(turmaId);
    }

    /** Lista apenas os alunos com matrícula ativa numa turma. */
    public List<AlunoTurma> ativosPorTurma(Long turmaId) {
        return repository.findByTurmaIdAndStatus(turmaId, StatusMatricula.ATIVA);
    }

    /** Lista o histórico completo de matrículas de um aluno (em todas as turmas). */
    public List<AlunoTurma> historicoPorAluno(Long alunoId) {
        return repository.findByAlunoIdOrderByDataInicioDesc(alunoId);
    }

    /** Quantidade de matrículas ativas em uma turma (fonte de verdade para "alunos ativos"). */
    public long contarAtivosPorTurma(Long turmaId) {
        return repository.countByTurmaIdAndStatus(turmaId, StatusMatricula.ATIVA);
    }

    /**
     * Cria uma nova matrícula, aplicando as regras de negócio:
     * - não permite duas matrículas ATIVAS para o mesmo par (aluno, turma);
     * - não permite matricular além da capacidadeMaxima da turma;
     * - status default é ATIVA quando não informado.
     */
    @Transactional
    public AlunoTurma matricular(AlunoTurma obj) {
        if (obj.getAluno() == null || obj.getAluno().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aluno é obrigatório para a matrícula.");
        }
        if (obj.getTurma() == null || obj.getTurma().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Turma é obrigatória para a matrícula.");
        }
        if (obj.getStatus() == null) {
            obj.setStatus(StatusMatricula.ATIVA);
        }
        if (obj.getDataInicio() == null) {
            obj.setDataInicio(LocalDate.now());
        }

        if (obj.getStatus() == StatusMatricula.ATIVA) {
            validarMatriculaAtivaUnica(obj.getAluno().getId(), obj.getTurma().getId(), null);
            validarCapacidade(obj.getTurma().getId());
        }

        return repository.save(obj);
    }

    /**
     * Atualiza uma matrícula existente. Reaplica as mesmas validações de
     * unicidade/capacidade caso o status resultante seja ATIVA.
     */
    @Transactional
    public AlunoTurma atualizar(Long id, AlunoTurma obj) {
        AlunoTurma existente = buscar(id);
        obj.setId(id);

        if (obj.getStatus() == StatusMatricula.ATIVA) {
            Long alunoId = obj.getAluno() != null ? obj.getAluno().getId() : existente.getAluno().getId();
            Long turmaId = obj.getTurma() != null ? obj.getTurma().getId() : existente.getTurma().getId();
            validarMatriculaAtivaUnica(alunoId, turmaId, id);
            // só valida capacidade se a matrícula não já estava ativa nessa mesma turma
            boolean jaEstavaAtivaNaMesmaTurma = existente.getStatus() == StatusMatricula.ATIVA
                    && existente.getTurma().getId().equals(turmaId);
            if (!jaEstavaAtivaNaMesmaTurma) {
                validarCapacidade(turmaId);
            }
        }

        return repository.save(obj);
    }

    /**
     * Cancela uma matrícula (soft delete): mantém o registro histórico,
     * apenas altera status para CANCELADA e preenche dataFim.
     */
    @Transactional
    public AlunoTurma cancelar(Long id, LocalDate dataFim) {
        AlunoTurma matricula = buscar(id);
        matricula.setStatus(StatusMatricula.CANCELADA);
        matricula.setDataFim(dataFim != null ? dataFim : LocalDate.now());
        return repository.save(matricula);
    }

    /**
     * Conclui uma matrícula (ex.: encerramento natural do ciclo na turma),
     * preservando o histórico.
     */
    @Transactional
    public AlunoTurma concluir(Long id, LocalDate dataFim) {
        AlunoTurma matricula = buscar(id);
        matricula.setStatus(StatusMatricula.CONCLUIDA);
        matricula.setDataFim(dataFim != null ? dataFim : LocalDate.now());
        return repository.save(matricula);
    }

    /**
     * Transfere um aluno de uma turma para outra: encerra a matrícula de
     * origem como TRANSFERIDA (não CANCELADA, para não confundir com
     * desistência) e cria uma nova matrícula ATIVA na turma de destino,
     * respeitando a capacidade máxima do destino.
     *
     * Importante: isso NÃO impede que o aluno mantenha outra matrícula ATIVA
     * em uma turma diferente — um aluno pode estar matriculado em duas ou
     * mais turmas ao mesmo tempo normalmente. A regra de unicidade é sempre
     * por par (aluno, turma), nunca "1 turma ativa por aluno no sistema
     * todo". Para o caso de transferência real (o aluno sai definitivamente
     * da turma de origem), chame este método passando a matrícula de origem;
     * se o objetivo for apenas adicionar o aluno a uma turma extra sem
     * encerrar a original, use matricular() diretamente.
     */
    @Transactional
    public AlunoTurma transferir(Long matriculaOrigemId, Long turmaDestinoId, LocalDate dataTransferencia) {
        AlunoTurma origem = buscar(matriculaOrigemId);

        if (origem.getStatus() != StatusMatricula.ATIVA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Só é possível transferir uma matrícula que esteja ATIVA.");
        }
        if (origem.getTurma().getId().equals(turmaDestinoId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Turma de destino deve ser diferente da turma de origem.");
        }

        Turma destino = turmaRepository.findById(turmaDestinoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turma de destino não encontrada."));

        LocalDate data = dataTransferencia != null ? dataTransferencia : LocalDate.now();

        validarMatriculaAtivaUnica(origem.getAluno().getId(), turmaDestinoId, null);
        validarCapacidade(turmaDestinoId);

        AlunoTurma novaMatricula = new AlunoTurma();
        novaMatricula.setAluno(origem.getAluno());
        novaMatricula.setTurma(destino);
        novaMatricula.setDataInicio(data);
        novaMatricula.setStatus(StatusMatricula.ATIVA);
        AlunoTurma salva = repository.save(novaMatricula);

        origem.setStatus(StatusMatricula.TRANSFERIDA);
        origem.setDataFim(data);
        origem.setMatriculaDestinoTransferencia(salva);
        repository.save(origem);

        return salva;
    }

    /**
     * Exclusão física: mantida apenas para compatibilidade/uso administrativo
     * pontual. Preferir sempre cancelar()/concluir() para preservar
     * histórico pedagógico (ver 4.3 da análise crítica).
     */
    public void deletar(Long id) { repository.deleteById(id); }

    private void validarMatriculaAtivaUnica(Long alunoId, Long turmaId, Long ignorarMatriculaId) {
        boolean jaAtiva = repository.findFirstByAluno_IdAndTurma_IdAndStatus(alunoId, turmaId, StatusMatricula.ATIVA)
                .filter(m -> ignorarMatriculaId == null || !m.getId().equals(ignorarMatriculaId))
                .isPresent();
        if (jaAtiva) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Este aluno já possui uma matrícula ativa nesta turma.");
        }
    }

    private void validarCapacidade(Long turmaId) {
        Turma turma = turmaRepository.findById(turmaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turma não encontrada."));
        if (turma.getCapacidadeMaxima() != null) {
            long ativos = repository.countByTurmaIdAndStatus(turmaId, StatusMatricula.ATIVA);
            if (ativos >= turma.getCapacidadeMaxima()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Capacidade máxima da turma atingida (" + turma.getCapacidadeMaxima() + " alunos).");
            }
        }
    }
}