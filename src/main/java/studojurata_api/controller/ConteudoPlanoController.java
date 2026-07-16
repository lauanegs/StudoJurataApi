package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.model.ConteudoPlano;
import studojurata_api.service.ConteudoPlanoService;

import java.util.List;

/** Correção 5.1: passa a falar com ConteudoPlanoService (soft-delete), não mais com o Repository diretamente. */
@RestController
@RequestMapping("/conteudo-plano")
@RequiredArgsConstructor
public class ConteudoPlanoController {

    private final ConteudoPlanoService service;

    @GetMapping public List<ConteudoPlano> listar(){ return service.listar(); }
    @GetMapping("/{id}") public ConteudoPlano buscar(@PathVariable Long id){ return service.buscar(id); }
    @PostMapping public ConteudoPlano salvar(@RequestBody ConteudoPlano o){ return service.salvar(o); }
    @PutMapping("/{id}") public ConteudoPlano atualizar(@PathVariable Long id,@RequestBody ConteudoPlano o){ return service.atualizar(id, o); }
    @DeleteMapping("/{id}") public void deletar(@PathVariable Long id){ service.deletar(id); }
}
