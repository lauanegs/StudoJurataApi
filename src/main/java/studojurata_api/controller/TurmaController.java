package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.model.Turma;
import studojurata_api.service.TurmaService;

import java.util.List;

@RestController
@RequestMapping("/turmas")
@RequiredArgsConstructor
public class TurmaController {

    private final TurmaService service;

    @GetMapping public List<Turma> listar(){ return service.listar(); }
    @GetMapping("/{id}") public Turma buscar(@PathVariable Long id){ return service.buscar(id); }
    @PostMapping public Turma salvar(@RequestBody Turma o){ return service.salvar(o); }
    @PutMapping("/{id}") public Turma atualizar(@PathVariable Long id,@RequestBody Turma o){ return service.atualizar(id, o); }
    @DeleteMapping("/{id}") public void deletar(@PathVariable Long id){ service.deletar(id); }

    /** Quantidade de alunos com matrícula ativa nesta turma (derivada, nunca um campo persistido). */
    @GetMapping("/{id}/alunos-ativos")
    public long alunosAtivos(@PathVariable Long id) { return service.contarAlunosAtivos(id); }
}