package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.model.PlanoEnsino;
import studojurata_api.service.PlanoEnsinoService;

import java.util.List;

/** Correção 5.1: passa a falar com PlanoEnsinoService (soft-delete), não mais com o Repository diretamente. */
@RestController
@RequestMapping("/plano-ensino")
@RequiredArgsConstructor
public class PlanoEnsinoController {

    private final PlanoEnsinoService service;

    @GetMapping public List<PlanoEnsino> listar(){ return service.listar(); }
    @GetMapping("/{id}") public PlanoEnsino buscar(@PathVariable Long id){ return service.buscar(id); }
    @PostMapping public PlanoEnsino salvar(@RequestBody PlanoEnsino o){ return service.salvar(o); }
    @PutMapping("/{id}") public PlanoEnsino atualizar(@PathVariable Long id,@RequestBody PlanoEnsino o){ return service.atualizar(id, o); }
    @DeleteMapping("/{id}") public void deletar(@PathVariable Long id){ service.deletar(id); }
}
