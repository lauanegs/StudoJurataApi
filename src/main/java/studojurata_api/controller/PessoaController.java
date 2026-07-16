package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.model.Pessoa;
import studojurata_api.service.PessoaService;

import java.util.List;

/** Correção 5.1: passa a falar com PessoaService (soft-delete), não mais com o Repository diretamente. */
@RestController
@RequestMapping("/pessoas")
@RequiredArgsConstructor
public class PessoaController {

    private final PessoaService service;

    @GetMapping public List<Pessoa> listar(){ return service.listar(); }
    @GetMapping("/{id}") public Pessoa buscar(@PathVariable Long id){ return service.buscar(id); }
    @PostMapping public Pessoa salvar(@RequestBody Pessoa o){ return service.salvar(o); }
    @PutMapping("/{id}") public Pessoa atualizar(@PathVariable Long id,@RequestBody Pessoa o){ return service.atualizar(id, o); }
    @DeleteMapping("/{id}") public void deletar(@PathVariable Long id){ service.deletar(id); }
}
