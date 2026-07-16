package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.model.Aluno;
import studojurata_api.service.AlunoService;

import java.util.List;

/** Correção 5.1: passa a falar com AlunoService (soft-delete), não mais com o Repository diretamente. */
@RestController
@RequestMapping("/alunos")
@RequiredArgsConstructor
public class AlunoController {

    private final AlunoService service;

    @GetMapping public List<Aluno> listar(){ return service.listar(); }
    @GetMapping("/{id}") public Aluno buscar(@PathVariable Long id){ return service.buscar(id); }
    @PostMapping public Aluno salvar(@RequestBody Aluno o){ return service.salvar(o); }
    @PutMapping("/{id}") public Aluno atualizar(@PathVariable Long id,@RequestBody Aluno o){ return service.atualizar(id, o); }
    @DeleteMapping("/{id}") public void deletar(@PathVariable Long id){ service.deletar(id); }
}
