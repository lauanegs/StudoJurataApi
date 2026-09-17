package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.exception.RegraNegocioException;
import studojurata_api.exception.RequisicaoInvalidaException;
import studojurata_api.model.Curso;
import studojurata_api.model.Turma;
import studojurata_api.model.enums.StatusAtivoInativo;
import studojurata_api.model.enums.StatusTurma;
import studojurata_api.repository.AlunoTurmaRepository;
import studojurata_api.repository.CursoRepository;
import studojurata_api.repository.TurmaRepository;
import studojurata_api.security.EscolaContext;

import java.util.List;

/**
 * validarCurso resolve o curso pelo id e recusa curso de outra escola (403)
 * ou inativo (409).
 */
@Service
@RequiredArgsConstructor
public class TurmaService {

    private final TurmaRepository repository;
    private final AlunoTurmaRepository alunoTurmaRepository;
    private final AlunoTurmaService alunoTurmaService;
    private final CursoRepository cursoRepository;
    private final EscolaContext escolaContext;

    /** Sem escola resolvível (antes do cadastro inicial da escola), não filtra. */
    public List<Turma> listar() {
        Long escolaId = escolaContext.escolaAtualId();
        return escolaId != null ? repository.findByEscola_Id(escolaId) : repository.findAll();
    }

    public Turma buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Turma " + id + " não encontrada."));
    }

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

        if (obj.getCapacidadeMaxima() != null) {
            long ativos = alunoTurmaService.contarAtivosPorTurma(id);
            if (ativos > obj.getCapacidadeMaxima()) {
                throw new RegraNegocioException(
                        "Não é possível reduzir a capacidade máxima para " + obj.getCapacidadeMaxima()
                                + ": a turma já possui " + ativos + " aluno(s) com matrícula ativa.");
            }
        }

        return repository.save(obj);
    }

    public long contarAlunosAtivos(Long turmaId) {
        return alunoTurmaService.contarAtivosPorTurma(turmaId);
    }

    /**
     * Recusa turma com qualquer matrícula: a exclusão serve só para turma
     * criada por engano; para encerrar, a turma é inativada.
     */
    public void deletar(Long id) {
        Turma turma = buscar(id);
        if (alunoTurmaRepository.existsByTurma_Id(id)) {
            throw new RegraNegocioException(
                    "Esta turma já teve aluno(s) matriculado(s) e não pode ser inativada por aqui — "
                            + "use a Situação em \"Dados da turma\" para inativar preservando o histórico.");
        }
        turma.setStatus(StatusTurma.INATIVA);
        repository.save(turma);
    }

    public Turma ativar(Long id) {
        Turma turma = buscar(id);
        turma.setStatus(StatusTurma.ATIVA);
        return repository.save(turma);
    }

    private void validarCapacidadeMaxima(Turma obj) {
        if (obj.getCapacidadeMaxima() != null && obj.getCapacidadeMaxima() <= 0) {
            throw new RequisicaoInvalidaException("Capacidade máxima deve ser maior que zero.");
        }
    }

    private void validarCurso(Turma obj) {
        if (obj.getCurso() == null || obj.getCurso().getId() == null) {
            throw new RequisicaoInvalidaException(
                    "Curso é obrigatório: o aluno matriculado na turma sempre segue o curso vinculado a ela.");
        }
        Curso curso = cursoRepository.findById(obj.getCurso().getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Curso " + obj.getCurso().getId() + " não encontrado."));

        if (curso.getStatus() == StatusAtivoInativo.INATIVO) {
            throw new RegraNegocioException(
                    "O curso \"" + curso.getNome() + "\" está inativo e não pode receber novas turmas.");
        }

        // Sem isso, qualquer curso de outra escola poderia ser vinculado só pelo id.
        Long escolaId = escolaContext.escolaAtualId();
        if (escolaId != null && curso.getEscola() != null && !escolaId.equals(curso.getEscola().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Este curso pertence a outra escola e não pode ser vinculado a esta turma.");
        }

        obj.setCurso(curso);
    }
}