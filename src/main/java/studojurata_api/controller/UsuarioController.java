package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.model.Usuario;
import studojurata_api.service.UsuarioService;

import java.util.List;

/**
 * Correção 5.1 + 5.2: passa a falar com UsuarioService (que agora é o único
 * caminho de escrita e faz o hash da senha), não mais com o Repository
 * diretamente. Antes existiam dois caminhos (este controller e o
 * UsuarioService, que era código morto e não hasheava) — agora há um só.
 */
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
}
