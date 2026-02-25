package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import studojurata_api.model.Aula;
import studojurata_api.repository.AulaRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AulaService {

    private final AulaRepository repository;

    public List<Aula> listar() { return repository.findAll(); }
    public Aula buscar(Long id) { return repository.findById(id).orElseThrow(); }
    public Aula salvar(Aula obj) { return repository.save(obj); }

    public Aula atualizar(Long id, Aula obj) {
        obj.setId(id);
        return repository.save(obj);
    }

    public void deletar(Long id) { repository.deleteById(id); }
}