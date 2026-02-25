package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import studojurata_api.model.ConteudoPlano;
import studojurata_api.repository.ConteudoPlanoRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConteudoPlanoService {

    private final ConteudoPlanoRepository repository;

    public List<ConteudoPlano> listar() { return repository.findAll(); }
    public ConteudoPlano buscar(Long id) { return repository.findById(id).orElseThrow(); }
    public ConteudoPlano salvar(ConteudoPlano obj) { return repository.save(obj); }

    public ConteudoPlano atualizar(Long id, ConteudoPlano obj) {
        obj.setId(id);
        return repository.save(obj);
    }

    public void deletar(Long id) { repository.deleteById(id); }
}