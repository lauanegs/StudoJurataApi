package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import studojurata_api.model.AulaConteudo;
import studojurata_api.repository.AulaConteudoRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AulaConteudoService {

    private final AulaConteudoRepository repository;

    public List<AulaConteudo> listar() { return repository.findAll(); }
    public AulaConteudo buscar(Long id) { return repository.findById(id).orElseThrow(); }
    public AulaConteudo salvar(AulaConteudo obj) { return repository.save(obj); }

    public AulaConteudo atualizar(Long id, AulaConteudo obj) {
        obj.setId(id);
        return repository.save(obj);
    }

    public void deletar(Long id) { repository.deleteById(id); }
}