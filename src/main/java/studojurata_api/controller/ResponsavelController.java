package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.model.Responsavel;
import studojurata_api.service.ResponsavelService;

import java.util.List;

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
    @PostMapping("/{id}/ativar") public Responsavel ativar(@PathVariable Long id){ return service.ativar(id); }
}
