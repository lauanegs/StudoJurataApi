package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.model.Escola;
import studojurata_api.model.enums.StatusAtivoInativo;
import studojurata_api.repository.EscolaRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EscolaService {

    private final EscolaRepository repository;

    public List<Escola> listar() { return repository.findAll(); }

    public Escola buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Escola " + id + " não encontrada."));
    }

    public Escola salvar(Escola obj) {
        if (obj.getStatus() == null) obj.setStatus(StatusAtivoInativo.ATIVO);
        return repository.save(obj);
    }

    public Escola atualizar(Long id, Escola obj) {
        obj.setId(id);
        return repository.save(obj);
    }

    /** Soft-delete (item 4.3/5.1): nunca exclusão física de uma escola com turmas/usuários vinculados. */
    public void deletar(Long id) {
        Escola escola = buscar(id);
        escola.setStatus(StatusAtivoInativo.INATIVO);
        repository.save(escola);
    }
}
