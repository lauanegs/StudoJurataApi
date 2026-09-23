package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.model.Disciplina;
import studojurata_api.service.DisciplinaService;

import java.util.List;

@RestController
@RequestMapping("/disciplinas")
@RequiredArgsConstructor
public class DisciplinaController {

    private final DisciplinaService service;

    @GetMapping public List<Disciplina> listar(){ return service.listar(); }
    @GetMapping("/{id}") public Disciplina buscar(@PathVariable Long id){ return service.buscarParaLeitura(id); }
    @PostMapping public Disciplina salvar(@RequestBody Disciplina o){ return service.salvar(o); }
    @PutMapping("/{id}") public Disciplina atualizar(@PathVariable Long id,@RequestBody Disciplina o){ return service.atualizar(id, o); }
    @DeleteMapping("/{id}") public void deletar(@PathVariable Long id){ service.deletar(id); }
    @PostMapping("/{id}/ativar") public Disciplina ativar(@PathVariable Long id){ return service.ativar(id); }
}
