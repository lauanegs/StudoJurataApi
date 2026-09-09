package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.exception.RegraNegocioException;
import studojurata_api.exception.RequisicaoInvalidaException;
import studojurata_api.model.Curso;
import studojurata_api.model.CursoDisciplina;
import studojurata_api.model.Disciplina;
import studojurata_api.model.enums.StatusAtivoInativo;
import studojurata_api.repository.CursoDisciplinaRepository;
import studojurata_api.repository.CursoRepository;
import studojurata_api.repository.DisciplinaRepository;

import java.util.List;

/**
 * Grade curricular (pedido explícito): quais disciplinas compõem cada Curso
 * e a carga horária de cada uma — reaproveitada pela tela de Turma
 * (restringe o seletor de disciplina às da grade do curso, ver
 * TurmaFormulario no front) e pelo Plano de Ensino (pré-preenche a carga
 * horária, ver PlanoEnsinoFormulario).
 */
@Service
@RequiredArgsConstructor
public class CursoDisciplinaService {

    private final CursoDisciplinaRepository repository;
    private final CursoRepository cursoRepository;
    private final DisciplinaRepository disciplinaRepository;

    public List<CursoDisciplina> listar() {
        return repository.findAll();
    }

    public List<CursoDisciplina> listarPorCurso(Long cursoId) {
        return repository.findByCurso_Id(cursoId);
    }

    public CursoDisciplina buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Vínculo curso-disciplina " + id + " não encontrado."));
    }

    public CursoDisciplina salvar(CursoDisciplina obj) {
        resolverVinculos(obj);
        if (obj.getStatus() == null) obj.setStatus(StatusAtivoInativo.ATIVO);
        validarDuplicidade(obj);
        CursoDisciplina salvo = repository.save(obj);
        recalcularCargaHorariaTotal(salvo.getCurso());
        return salvo;
    }

    public CursoDisciplina atualizar(Long id, CursoDisciplina obj) {
        obj.setId(id);
        resolverVinculos(obj);
        validarDuplicidade(obj);
        CursoDisciplina salvo = repository.save(obj);
        recalcularCargaHorariaTotal(salvo.getCurso());
        return salvo;
    }

    /** Soft-delete: turmas/planos de ensino que já usam esta disciplina no curso preservam a referência histórica. */
    public void deletar(Long id) {
        CursoDisciplina obj = buscar(id);
        obj.setStatus(StatusAtivoInativo.INATIVO);
        repository.save(obj);
        recalcularCargaHorariaTotal(obj.getCurso());
    }

    private void resolverVinculos(CursoDisciplina obj) {
        if (obj.getCurso() == null || obj.getCurso().getId() == null) {
            throw new RequisicaoInvalidaException("Curso é obrigatório.");
        }
        if (obj.getDisciplina() == null || obj.getDisciplina().getId() == null) {
            throw new RequisicaoInvalidaException("Disciplina é obrigatória.");
        }
        Curso curso = cursoRepository.findById(obj.getCurso().getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Curso " + obj.getCurso().getId() + " não encontrado."));
        Disciplina disciplina = disciplinaRepository.findById(obj.getDisciplina().getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Disciplina " + obj.getDisciplina().getId() + " não encontrada."));
        obj.setCurso(curso);
        obj.setDisciplina(disciplina);
    }

    private void validarDuplicidade(CursoDisciplina obj) {
        boolean duplicado = repository.findByCurso_Id(obj.getCurso().getId()).stream()
                .filter(item -> item.getStatus() == StatusAtivoInativo.ATIVO)
                .filter(item -> !item.getId().equals(obj.getId()))
                .anyMatch(item -> item.getDisciplina().getId().equals(obj.getDisciplina().getId()));
        if (duplicado) {
            throw new RegraNegocioException("Esta disciplina já está vinculada a este curso.");
        }
    }

    /** Curso.cargaHorariaTotal passa a ser sempre a soma das cargas horárias ativas desta grade. */
    private void recalcularCargaHorariaTotal(Curso curso) {
        int total = repository.findByCurso_Id(curso.getId()).stream()
                .filter(item -> item.getStatus() == StatusAtivoInativo.ATIVO)
                .mapToInt(item -> item.getCargaHoraria() != null ? item.getCargaHoraria() : 0)
                .sum();
        curso.setCargaHorariaTotal(total);
        cursoRepository.save(curso);
    }
}
