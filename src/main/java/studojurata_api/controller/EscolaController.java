package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.model.Escola;
import studojurata_api.service.EscolaService;

import java.util.List;

@RestController
@RequestMapping("/escolas")
@RequiredArgsConstructor
public class EscolaController {

    private final EscolaService service;

    @GetMapping public List<Escola> listar(){ return service.listar(); }
    @GetMapping("/{id}") public Escola buscar(@PathVariable Long id){ return service.buscar(id); }
    @PostMapping public Escola salvar(@RequestBody Escola o){ return service.salvar(o); }
    @PutMapping("/{id}") public Escola atualizar(@PathVariable Long id, @RequestBody Escola o){ return service.atualizar(id, o); }
    @DeleteMapping("/{id}") public void deletar(@PathVariable Long id){ service.deletar(id); }
}
