package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import studojurata_api.model.Responsavel;
import studojurata_api.repository.ResponsavelRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ResponsavelService {

    private final ResponsavelRepository repository;

    public List<Responsavel> listar() { return repository.findAll(); }
    public Responsavel buscar(Long id) { return repository.findById(id).orElseThrow(); }
    public Responsavel salvar(Responsavel obj) { return repository.save(obj); }

    public Responsavel atualizar(Long id, Responsavel obj) {
        obj.setId(id);
        return repository.save(obj);
    }

    public void deletar(Long id) { repository.deleteById(id); }
}