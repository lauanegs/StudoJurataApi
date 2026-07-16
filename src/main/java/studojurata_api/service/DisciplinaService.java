package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.model.Disciplina;
import studojurata_api.model.enums.StatusAtivoInativo;
import studojurata_api.repository.DisciplinaRepository;

import java.util.List;

/** Correção 5.1: controller passa a usar este service, não mais o Repository. */
@Service
@RequiredArgsConstructor
public class DisciplinaService {

    private final DisciplinaRepository repository;

    public List<Disciplina> listar() { return repository.findAll(); }

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
