package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import studojurata_api.model.PlanoAula;
import studojurata_api.repository.PlanoAulaRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlanoAulaService {

    private final PlanoAulaRepository repository;

    public List<PlanoAula> listar() { return repository.findAll(); }
    public PlanoAula buscar(Long id) { return repository.findById(id).orElseThrow(); }
    public PlanoAula salvar(PlanoAula obj) { return repository.save(obj); }

    public PlanoAula atualizar(Long id, PlanoAula obj) {
        obj.setId(id);
        return repository.save(obj);
    }

    public void deletar(Long id) { repository.deleteById(id); }
}