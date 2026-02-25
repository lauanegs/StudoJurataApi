package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import studojurata_api.model.Nota;
import studojurata_api.repository.NotaRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotaService {

    private final NotaRepository repository;

    public List<Nota> listar() { return repository.findAll(); }
    public Nota buscar(Long id) { return repository.findById(id).orElseThrow(); }
    public Nota salvar(Nota obj) { return repository.save(obj); }

    public Nota atualizar(Long id, Nota obj) {
        obj.setId(id);
        return repository.save(obj);
    }

    public void deletar(Long id) { repository.deleteById(id); }
}