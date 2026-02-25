package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import studojurata_api.model.Alternativa;
import studojurata_api.repository.AlternativaRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AlternativaService {

    private final AlternativaRepository repository;

    public List<Alternativa> listar() { return repository.findAll(); }

    public Alternativa buscar(Long id) { return repository.findById(id).orElseThrow(); }

    public Alternativa salvar(Alternativa obj) { return repository.save(obj); }

    public Alternativa atualizar(Long id, Alternativa obj) {
        obj.setId(id);
        return repository.save(obj);
    }

    public void deletar(Long id) { repository.deleteById(id); }
}