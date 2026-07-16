package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.model.Disciplina;
import studojurata_api.model.enums.StatusAtivoInativo;
import studojurata_api.repository.DisciplinaRepository;
import studojurata_api.security.EscolaContext;

import java.util.List;

/**
 * Correção 5.1: controller passa a usar este service, não mais o Repository.
 * Correção 2.2 da Terceira Análise Crítica (isolamento multi-tenant):
 * listar() filtra pela escola do usuário autenticado.
 */
@Service
@RequiredArgsConstructor
public class DisciplinaService {

    private final DisciplinaRepository repository;
    private final EscolaContext escolaContext;

    /** Filtra pela escola do usuário autenticado; se não houver escola resolvível, devolve tudo (bootstrapping). */
    public List<Disciplina> listar() {
        Long escolaId = escolaContext.escolaAtualId();
        return escolaId != null ? repository.findByEscola_Id(escolaId) : repository.findAll();
    }

    public Disciplina buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Disciplina " + id + " não encontrada."));
    }

    public Disciplina salvar(Disciplina obj) {
        if (obj.getStatus() == null) obj.setStatus(StatusAtivoInativo.ATIVO);
        return repository.save(obj);
    }

    public Disciplina atualizar(Long id, Disciplina obj) {
        obj.setId(id);
        return repository.save(obj);
    }

    /** Soft-delete (item 4.3/5.1): Disciplina pode ter Questao/Nota/PlanoEnsino vinculados. */
    public void deletar(Long id) {
        Disciplina disciplina = buscar(id);
        disciplina.setStatus(StatusAtivoInativo.INATIVO);
        repository.save(disciplina);
    }
}
