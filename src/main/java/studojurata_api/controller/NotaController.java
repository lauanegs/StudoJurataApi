package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.model.Nota;
import studojurata_api.security.AlunoAccessGuard;
import studojurata_api.service.NotaService;

import java.util.List;

/**
 * Não há escrita livre de nota: ela é sempre derivada dos simulados via
 * /recalcular. Consultas por aluno passam por AlunoAccessGuard para que um
 * aluno só veja as próprias notas.
 */
@RestController
@RequestMapping("/notas")
@RequiredArgsConstructor
public class NotaController {

    private final NotaService service;
    private final AlunoAccessGuard alunoAccessGuard;

    @GetMapping public List<Nota> listar(){ return service.listar(); }

    @GetMapping("/{id}")
    public Nota buscar(@PathVariable Long id){
        Nota nota = service.buscar(id);
        alunoAccessGuard.garantir(nota.getAluno().getId());
        return nota;
    }

    @GetMapping("/aluno/{alunoId}/historico")
    public List<Nota> historicoPorAluno(@PathVariable Long alunoId) {
        alunoAccessGuard.garantir(alunoId);
        return service.historicoPorAluno(alunoId);
    }

    @GetMapping("/aluno/{alunoId}/disciplina/{disciplinaId}/historico")
    public List<Nota> historicoPorAlunoEDisciplina(@PathVariable Long alunoId, @PathVariable Long disciplinaId) {
        alunoAccessGuard.garantir(alunoId);
        return service.historicoPorAlunoEDisciplina(alunoId, disciplinaId);
    }

    @PostMapping("/recalcular")
    public Nota recalcular(@RequestParam Long alunoId, @RequestParam Long disciplinaId, @RequestParam Long turmaId) {
        return service.recalcular(alunoId, disciplinaId, turmaId);
    }

    @DeleteMapping("/{id}") public void deletar(@PathVariable Long id){ service.deletar(id); }
}
