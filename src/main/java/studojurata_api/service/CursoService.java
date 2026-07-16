package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.model.Curso;
import studojurata_api.model.enums.StatusAtivoInativo;
import studojurata_api.repository.CursoRepository;
import studojurata_api.security.EscolaContext;

import java.util.List;

/**
 * Curso: entidade própria (ver Curso.java) — cada Turma passa a apontar para
 * um Curso cadastrado, em vez de repetir o nome do curso como texto livre.
 */
@Service
@RequiredArgsConstructor
public class CursoService {

    private final CursoRepository repository;
    private final EscolaContext escolaContext;

    /** Filtra pela escola do usuário autenticado; se não houver escola resolvível, devolve tudo (bootstrapping). */
    public List<Curso> listar() {
        Long escolaId = escolaContext.escolaAtualId();
        return escolaId != null ? repository.findByEscola_Id(escolaId) : repository.findAll();
    }

    public Curso buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Curso " + id + " não encontrado."));
    }

    public Curso salvar(Curso obj) {
        if (obj.getStatus() == null) obj.setStatus(StatusAtivoInativo.ATIVO);
        return repository.save(obj);
    }

    public Curso atualizar(Long id, Curso obj) {
        obj.setId(id);
        return repository.save(obj);
    }

    /** Soft-delete: turmas já vinculadas a este curso preservam a referência histórica. */
    public void deletar(Long id) {
        Curso curso = buscar(id);
        curso.setStatus(StatusAtivoInativo.INATIVO);
        repository.save(curso);
    }
}
