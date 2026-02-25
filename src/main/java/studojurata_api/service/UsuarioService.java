package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import studojurata_api.model.Usuario;
import studojurata_api.repository.UsuarioRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository repository;

    public List<Usuario> listar() { return repository.findAll(); }
    public Usuario buscar(Long id) { return repository.findById(id).orElseThrow(); }
    public Usuario salvar(Usuario obj) { return repository.save(obj); }

    public Usuario atualizar(Long id, Usuario obj) {
        obj.setId(id);
        return repository.save(obj);
    }

    public void deletar(Long id) { repository.deleteById(id); }
}