package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.model.AlunoTurma;
import studojurata_api.security.AlunoAccessGuard;
import studojurata_api.service.AlunoTurmaService;

import java.util.List;

@RestController
@RequestMapping("/aluno-turma")
@RequiredArgsConstructor
public class AlunoTurmaController {

    private final AlunoTurmaService service;
    private final AlunoAccessGuard alunoAccessGuard;

    @GetMapping
    public List<AlunoTurma> listar() {
        // Listagem agregada de matrículas: hoje alimenta telas de gestão; o
        // recorte por turma para o professor fica no C2.
        alunoAccessGuard.garantirAcessoDeGestao();
        return service.listar();
    }

    @GetMapping("/{id}")
    public AlunoTurma buscar(@PathVariable Long id) {
        AlunoTurma matricula = service.buscar(id);
        alunoAccessGuard.garantir(matricula.getAluno() != null ? matricula.getAluno().getId() : null);
        return matricula;
    }

    /** Histórico completo de matrículas de uma turma (ativas, concluídas, canceladas). */
    @GetMapping("/turma/{turmaId}/historico")
    public List<AlunoTurma> historicoPorTurma(@PathVariable Long turmaId) {
        alunoAccessGuard.garantirAcessoATurma(turmaId);
        return service.historicoPorTurma(turmaId);
    }

    @GetMapping("/turma/{turmaId}/ativos")
    public List<AlunoTurma> ativosPorTurma(@PathVariable Long turmaId) {
        alunoAccessGuard.garantirAcessoATurma(turmaId);
        return service.ativosPorTurma(turmaId);
    }

    @PostMapping
    public AlunoTurma matricular(@RequestBody AlunoTurma o) { return service.matricular(o); }

    @PutMapping("/{id}")
    public AlunoTurma atualizar(@PathVariable Long id, @RequestBody AlunoTurma o) { return service.atualizar(id, o); }
}
