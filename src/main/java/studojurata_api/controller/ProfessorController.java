package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.model.Professor;
import studojurata_api.model.TurmaDisciplina;
import studojurata_api.service.ProfessorService;

import java.util.List;

/** Correção 5.1: passa a falar com ProfessorService (soft-delete + reatribuição de turmas), não mais com o Repository. */
@RestController
@RequestMapping("/professores")
@RequiredArgsConstructor
public class ProfessorController {

    private final ProfessorService service;

    @GetMapping public List<Professor> listar(){ return service.listar(); }
    @GetMapping("/{id}") public Professor buscar(@PathVariable Long id){ return service.buscar(id); }
    @GetMapping("/{id}/turmas") public List<TurmaDisciplina> turmasLecionadas(@PathVariable Long id){ return service.turmasLecionadas(id); }
    @PostMapping public Professor salvar(@RequestBody Professor o){ return service.salvar(o); }
    @PutMapping("/{id}") public Professor atualizar(@PathVariable Long id,@RequestBody Professor o){ return service.atualizar(id, o); }
    @DeleteMapping("/{id}") public void deletar(@PathVariable Long id){ service.deletar(id); }
}
