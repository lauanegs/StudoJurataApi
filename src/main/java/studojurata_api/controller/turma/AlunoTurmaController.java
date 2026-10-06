package studojurata_api.controller.turma;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.model.turma.AlunoTurma;
import studojurata_api.security.AlunoAccessGuard;
import studojurata_api.service.turma.AlunoTurmaService;

import java.util.List;

@RestController
@RequestMapping("/aluno-turma")
@RequiredArgsConstructor
public class AlunoTurmaController {
    private final AlunoTurmaService service;
    private final AlunoAccessGuard alunoAccessGuard;

    @GetMapping
    public List<AlunoTurma> listar() {
        // Listagem agregada de matrículas
        alunoAccessGuard.garantirAcessoDeGestao();
        return service.listar();
    }

    @GetMapping("/{id}")
    public AlunoTurma buscar(@PathVariable Long id) {
        AlunoTurma matricula = service.buscar(id);
        alunoAccessGuard.garantir(matricula.getAluno() != null ? matricula.getAluno().getId() : null);
        return matricula;
    }

    /** Matrículas (ativas e concluídas) de um aluno */
    @GetMapping("/aluno/{alunoId}")
    public List<AlunoTurma> porAluno(@PathVariable Long alunoId) {
        alunoAccessGuard.garantir(alunoId);
        return service.matriculasDoAluno(alunoId);
    }

    /** Histórico completo de matrículas de uma turma (ativas, concluídas, canceladas) */
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
