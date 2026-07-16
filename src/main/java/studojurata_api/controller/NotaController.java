package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.model.Nota;
import studojurata_api.service.NotaService;

import java.util.List;

/**
 * Correção 1.2/2.13/5.1 da Segunda Análise Crítica: passa a falar com
 * NotaService (não mais com o Repository diretamente), e Nota.total deixa de
 * ser aceito diretamente do cliente — é sempre recalculado via /recalcular.
 */
@RestController
@RequestMapping("/notas")
@RequiredArgsConstructor
public class NotaController {

    private final NotaService service;

    @GetMapping public List<Nota> listar(){ return service.listar(); }
    @GetMapping("/{id}") public Nota buscar(@PathVariable Long id){ return service.buscar(id); }

    /** Histórico de notas do aluno em todos os períodos letivos — acessível ao próprio aluno (ver SecurityConfig). */
    @GetMapping("/aluno/{alunoId}/historico")
    public List<Nota> historicoPorAluno(@PathVariable Long alunoId) {
        return service.historicoPorAluno(alunoId);
    }

    @GetMapping("/aluno/{alunoId}/disciplina/{disciplinaId}/historico")
    public List<Nota> historicoPorAlunoEDisciplina(@PathVariable Long alunoId, @PathVariable Long disciplinaId) {
        return service.historicoPorAlunoEDisciplina(alunoId, disciplinaId);
    }

    /**
     * Recalcula (deriva) a nota do aluno numa disciplina/período letivo a
     * partir dos simulados concluídos — este é o único jeito de gerar/alterar
     * uma Nota agora; não existe mais POST/PUT de total livre.
     */
    @PostMapping("/recalcular")
    public Nota recalcular(@RequestParam Long alunoId, @RequestParam Long disciplinaId, @RequestParam String periodoLetivo) {
        return service.recalcular(alunoId, disciplinaId, periodoLetivo);
    }

    @DeleteMapping("/{id}") public void deletar(@PathVariable Long id){ service.deletar(id); }
}
