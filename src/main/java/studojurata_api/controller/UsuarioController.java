package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import studojurata_api.model.Usuario;
import studojurata_api.repository.UsuarioRepository;

import java.util.List;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping public List<Usuario> listar(){ return repository.findAll();}
    @GetMapping("/{id}") public Usuario buscar(@PathVariable Long id){ return repository.findById(id).orElseThrow();}

    @PostMapping
    public Usuario salvar(@RequestBody Usuario o){
        o.setSenha(passwordEncoder.encode(o.getSenha()));
        return repository.save(o);
    }

    @PutMapping("/{id}")
    public Usuario atualizar(@PathVariable Long id,@RequestBody Usuario o){
        o.setId(id);
        // Só re-hasheia a senha se uma nova senha em texto puro foi enviada;
        // evita hashear novamente um hash já persistido.
        if (o.getSenha() != null && !o.getSenha().isBlank()) {
            o.setSenha(passwordEncoder.encode(o.getSenha()));
        } else {
            repository.findById(id).ifPresent(existente -> o.setSenha(existente.getSenha()));
        }
        return repository.save(o);
    }

    @DeleteMapping("/{id}") public void deletar(@PathVariable Long id){ repository.deleteById(id);}
}
