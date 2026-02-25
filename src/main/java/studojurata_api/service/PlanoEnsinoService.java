package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import studojurata_api.model.PlanoEnsino;
import studojurata_api.repository.PlanoEnsinoRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlanoEnsinoService {

    private final PlanoEnsinoRepository repository;

    public List<PlanoEnsino> listar() { return repository.findAll(); }
    public PlanoEnsino buscar(Long id) { return repository.findById(id).orElseThrow(); }
    public PlanoEnsino salvar(PlanoEnsino obj) { return repository.save(obj); }

    public PlanoEnsino atualizar(Long id, PlanoEnsino obj) {
        obj.setId(id);
        return repository.save(obj);
    }

    public void deletar(Long id) { repository.deleteById(id); }
}