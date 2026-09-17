package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.model.AlunoTurma;
import studojurata_api.service.AlunoTurmaService;

import java.util.List;

@RestController
@RequestMapping("/aluno-turma")
@RequiredArgsConstructor
public class AlunoTurmaController {

    private final AlunoTurmaService service;

    @GetMapping
    public List<AlunoTurma> listar() { return service.listar(); }

    @GetMapping("/{id}")
    public AlunoTurma buscar(@PathVariable Long id) { return service.buscar(id); }

    /** Histórico completo de matrículas de uma turma (ativas, concluídas, canceladas). */
    @GetMapping("/turma/{turmaId}/historico")
    public List<AlunoTurma> historicoPorTurma(@PathVariable Long turmaId) { return service.historicoPorTurma(turmaId); }

    @GetMapping("/turma/{turmaId}/ativos")
    public List<AlunoTurma> ativosPorTurma(@PathVariable Long turmaId) { return service.ativosPorTurma(turmaId); }

    @PostMapping
    public AlunoTurma matricular(@RequestBody AlunoTurma o) { return service.matricular(o); }

    @PutMapping("/{id}")
    public AlunoTurma atualizar(@PathVariable Long id, @RequestBody AlunoTurma o) { return service.atualizar(id, o); }
}