package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.model.Usuario;
import studojurata_api.repository.UsuarioRepository;

import java.util.List;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioRepository repository;

    @GetMapping public List<Usuario> listar(){ return repository.findAll();}
    @GetMapping("/{id}") public Usuario buscar(@PathVariable Long id){ return repository.findById(id).orElseThrow();}
    @PostMapping public Usuario salvar(@RequestBody Usuario o){ return repository.save(o);}
    @PutMapping("/{id}") public Usuario atualizar(@PathVariable Long id,@RequestBody Usuario o){ o.setId(id); return repository.save(o);}
    @DeleteMapping("/{id}") public void deletar(@PathVariable Long id){ repository.deleteById(id);}
}