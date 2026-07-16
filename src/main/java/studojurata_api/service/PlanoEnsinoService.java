package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.model.Curso;
import studojurata_api.model.PlanoEnsino;
import studojurata_api.model.enums.StatusAtivoInativo;
import studojurata_api.repository.CursoRepository;
import studojurata_api.repository.PlanoEnsinoRepository;

import java.util.List;

/**
 * Correção 5.1: controller passa a usar este service, não mais o Repository.
 * Correção 2.4 da Terceira Análise Crítica: periodoLetivo passa a ser
 * validado (obrigatório) — sem ele, o recálculo automático da Nota do aluno
 * é silenciosamente pulado (ver NotaService/SimuladoAlunoService.finalizar).
 * Vínculo pedido explicitamente: todo Plano de Ensino pertence a um Curso
 * (ver PlanoEnsino.curso) — validado e resolvido da mesma forma que
 * TurmaService faz para Turma.curso.
 */
@Service
@RequiredArgsConstructor
public class PlanoEnsinoService {

    private final PlanoEnsinoRepository repository;
    private final CursoRepository cursoRepository;

    public List<PlanoEnsino> listar() { return repository.findAll(); }

    public PlanoEnsino buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Plano de ensino " + id + " não encontrado."));
    }

    /** Todos os planos de ensino (um por disciplina) vinculados a um curso. */
    public List<PlanoEnsino> listarPorCurso(Long cursoId) {
        return repository.findByCurso_Id(cursoId);
    }

    public PlanoEnsino salvar(PlanoEnsino obj) {
        validarPeriodoLetivo(obj);
        validarCurso(obj);
        if (obj.getStatus() == null) obj.setStatus(StatusAtivoInativo.ATIVO);
        return repository.save(obj);
    }

    public PlanoEnsino atualizar(Long id, PlanoEnsino obj) {
        validarPeriodoLetivo(obj);
        validarCurso(obj);
        obj.setId(id);
        return repository.save(obj);
    }

    private void validarPeriodoLetivo(PlanoEnsino obj) {
        if (obj.getPeriodoLetivo() == null || obj.getPeriodoLetivo().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Período letivo é obrigatório: sem ele, a nota do aluno nesta disciplina não pode ser calculada.");
        }
    }

    private void validarCurso(PlanoEnsino obj) {
        if (obj.getCurso() == null || obj.getCurso().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Curso é obrigatório: todo plano de ensino pertence a um curso.");
        }
        Curso curso = cursoRepository.findById(obj.getCurso().getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Curso " + obj.getCurso().getId() + " não encontrado."));
        obj.setCurso(curso);
    }

    /** Soft-delete (item 4.3/5.1 + caso extremo "Plano de ensino alterado após simulados já realizados"). */
    public void deletar(Long id) {
        PlanoEnsino plano = buscar(id);
        plano.setStatus(StatusAtivoInativo.INATIVO);
        repository.save(plano);
    }
}
