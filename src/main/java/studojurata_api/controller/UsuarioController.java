package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.model.Usuario;
import studojurata_api.service.UsuarioService;

import java.util.List;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService service;

    @GetMapping public List<Usuario> listar(){ return service.listar(); }
    @GetMapping("/{id}") public Usuario buscar(@PathVariable Long id){ return service.buscar(id); }
    @PostMapping public Usuario salvar(@RequestBody Usuario o){ return service.salvar(o); }
    @PutMapping("/{id}") public Usuario atualizar(@PathVariable Long id,@RequestBody Usuario o){ return service.atualizar(id, o); }
    @DeleteMapping("/{id}") public void deletar(@PathVariable Long id){ service.deletar(id); }
    @PostMapping("/{id}/ativar") public Usuario ativar(@PathVariable Long id){ return service.ativar(id); }
}
