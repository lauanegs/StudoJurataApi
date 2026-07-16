package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import studojurata_api.model.Turma;
import studojurata_api.model.enums.StatusMatricula;
import studojurata_api.model.enums.StatusTurma;
import studojurata_api.repository.AlunoTurmaRepository;
import studojurata_api.repository.TurmaRepository;
import studojurata_api.security.EscolaContext;

import java.util.List;

/**
 * Correção 2.2 da Terceira Análise Crítica (isolamento multi-tenant):
 * listar() passa a filtrar pela escola do usuário autenticado (via
 * EscolaContext), em vez de devolver as turmas de todas as escolas para
 * qualquer usuário. Correção 2.3: deletar() passa a ser soft-delete
 * (Turma.status = INATIVA) — o campo já existia mas nunca era usado, e uma
 * Turma pode ter AlunoTurma/TurmaDisciplina/histórico de simulados
 * vinculados, o mesmo risco que motivou soft-delete nas demais entidades.
 * Correção 2.4: curso passa a ser obrigatório (ver Turma.curso).
 */
@Service
@RequiredArgsConstructor
public class TurmaService {

    private final TurmaRepository repository;
    private final AlunoTurmaRepository alunoTurmaRepository;
    private final EscolaContext escolaContext;

    /** Filtra pela escola do usuário autenticado; se não houver escola resolvível, devolve tudo (bootstrapping). */
    public List<Turma> listar() {
        Long escolaId = escolaContext.escolaAtualId();
        return escolaId != null ? repository.findByEscola_Id(escolaId) : repository.findAll();
    }

    public Turma buscar(Long id) { return repository.findById(id).orElseThrow(); }

    public Turma salvar(Turma obj) {
        validarCapacidadeMaxima(obj);
        validarCurso(obj);
        if (obj.getStatus() == null) obj.setStatus(StatusTurma.ATIVA);
        return repository.save(obj);
    }

    public Turma atualizar(Long id, Turma obj) {
        validarCapacidadeMaxima(obj);
        validarCurso(obj);
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

    /** Soft-delete (correção 2.3): preserva o histórico de matrículas/disciplinas vinculadas à turma. */
    public void deletar(Long id) {
        Turma turma = buscar(id);
        turma.setStatus(StatusTurma.INATIVA);
        repository.save(turma);
    }

    private void validarCapacidadeMaxima(Turma obj) {
        if (obj.getCapacidadeMaxima() != null && obj.getCapacidadeMaxima() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Capacidade máxima deve ser maior que zero.");
        }
    }

    private void validarCurso(Turma obj) {
        if (obj.getCurso() == null || obj.getCurso().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Curso é obrigatório: o aluno matriculado na turma sempre segue o curso vinculado a ela.");
        }
    }
}