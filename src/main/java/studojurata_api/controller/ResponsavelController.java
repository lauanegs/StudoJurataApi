package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.model.Responsavel;
import studojurata_api.service.ResponsavelService;

import java.util.List;

/** Correção 5.1: passa a falar com ResponsavelService (soft-delete), não mais com o Repository diretamente. */
@RestController
@RequestMapping("/responsaveis")
@RequiredArgsConstructor
public class ResponsavelController {

    private final ResponsavelService service;

    @GetMapping public List<Responsavel> listar(){ return service.listar(); }
    @GetMapping("/{id}") public Responsavel buscar(@PathVariable Long id){ return service.buscar(id); }
    @PostMapping public Responsavel salvar(@RequestBody Responsavel o){ return service.salvar(o); }
    @PutMapping("/{id}") public Responsavel atualizar(@PathVariable Long id,@RequestBody Responsavel o){ return service.atualizar(id, o); }
    @DeleteMapping("/{id}") public void deletar(@PathVariable Long id){ service.deletar(id); }
}
