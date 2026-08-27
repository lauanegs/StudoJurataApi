package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.exception.RegraNegocioException;
import studojurata_api.exception.RequisicaoInvalidaException;
import studojurata_api.model.Curso;
import studojurata_api.model.PlanoEnsino;
import studojurata_api.model.enums.StatusAtivoInativo;
import studojurata_api.model.enums.StatusPlano;
import studojurata_api.repository.CursoRepository;
import studojurata_api.repository.PlanoEnsinoRepository;
import studojurata_api.security.EscolaContext;

import java.util.List;

/**
 * Correção 5.1: controller passa a usar este service, não mais o Repository.
 * Vínculo pedido explicitamente: todo Plano de Ensino pertence a um Curso
 * (ver PlanoEnsino.curso) — validado e resolvido da mesma forma que
 * TurmaService faz para Turma.curso.
 *
 * Correção 3.1 da Quarta Análise Crítica (isolamento por escola na
 * escrita): validarCurso recusa (403) um Curso que não pertence à escola
 * do usuário autenticado. Correção 3.3: recusa (409) vincular um plano de
 * ensino a um Curso já INATIVO.
 *
 * Correção "matrícula cíclica": periodoLetivo deixou de existir/ser
 * validado (ver PlanoEnsino.java) — a nota do aluno é escopada por turma,
 * não por calendário.
 */
@Service
@RequiredArgsConstructor
public class PlanoEnsinoService {

    private final PlanoEnsinoRepository repository;
    private final CursoRepository cursoRepository;
    private final EscolaContext escolaContext;

    /**
     * Filtra pela escola do usuário autenticado (via Curso.escola); se não
     * houver escola resolvível, devolve tudo (bootstrapping). Correção de
     * auditoria: antes listar() ignorava EscolaContext (que já era usado em
     * validarCurso, no caminho de escrita), devolvendo planos de ensino de
     * todas as escolas para qualquer usuário autenticado.
     */
    public List<PlanoEnsino> listar() {
        Long escolaId = escolaContext.escolaAtualId();
        return escolaId != null ? repository.findByCurso_Escola_Id(escolaId) : repository.findAll();
    }

    public PlanoEnsino buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Plano de ensino " + id + " não encontrado."));
    }

    /** Todos os planos de ensino (um por disciplina) vinculados a um curso. */
    public List<PlanoEnsino> listarPorCurso(Long cursoId) {
        return repository.findByCurso_Id(cursoId);
    }

    public PlanoEnsino salvar(PlanoEnsino obj) {
        validarCurso(obj);
        if (obj.getStatus() == null) obj.setStatus(StatusPlano.ATIVO);
        return repository.save(obj);
    }

    public PlanoEnsino atualizar(Long id, PlanoEnsino obj) {
        validarCurso(obj);
        obj.setId(id);
        return repository.save(obj);
    }

    private void validarCurso(PlanoEnsino obj) {
        if (obj.getCurso() == null || obj.getCurso().getId() == null) {
            throw new RequisicaoInvalidaException("Curso é obrigatório: todo plano de ensino pertence a um curso.");
        }
        Curso curso = cursoRepository.findById(obj.getCurso().getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Curso " + obj.getCurso().getId() + " não encontrado."));

        if (curso.getStatus() == StatusAtivoInativo.INATIVO) {
            throw new RegraNegocioException(
                    "O curso \"" + curso.getNome() + "\" está inativo e não pode receber novos planos de ensino.");
        }

        Long escolaId = escolaContext.escolaAtualId();
        if (escolaId != null && curso.getEscola() != null && !escolaId.equals(curso.getEscola().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Este curso pertence a outra escola e não pode ser vinculado a este plano de ensino.");
        }

        obj.setCurso(curso);
    }

    /**
     * Soft-delete (item 4.3/5.1 + caso extremo "Plano de ensino alterado
     * após simulados já realizados") — vira CONCLUIDO, não "excluído" de
     * fato: histórico (conteúdos, planos de aula, simulados) é preservado.
     */
    public void deletar(Long id) {
        PlanoEnsino plano = buscar(id);
        plano.setStatus(StatusPlano.CONCLUIDO);
        repository.save(plano);
    }
}
