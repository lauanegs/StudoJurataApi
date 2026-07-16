package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.model.Nota;
import studojurata_api.security.AlunoAccessGuard;
import studojurata_api.service.NotaService;

import java.util.List;

/**
 * Correção 1.2/2.13/5.1 da Segunda Análise Crítica: passa a falar com
 * NotaService (não mais com o Repository diretamente), e Nota.total deixa de
 * ser aceito diretamente do cliente — é sempre recalculado via /recalcular.
 *
 * Correção 2.1 da Terceira Análise Crítica (IDOR): listar()/recalcular() são
 * agora restritos a PROFESSOR/ADMINISTRADOR (ver SecurityConfig); buscar()
 * e os endpoints de histórico por aluno passam por AlunoAccessGuard, que
 * garante que um Aluno só veja as suas próprias notas.
 */
@RestController
@RequestMapping("/notas")
@RequiredArgsConstructor
public class NotaController {

    private final NotaService service;
    private final AlunoAccessGuard alunoAccessGuard;

    /** Restrito a PROFESSOR/ADMINISTRADOR (ver SecurityConfig) — listagem geral não é por aluno específico. */
    @GetMapping public List<Nota> listar(){ return service.listar(); }

    @GetMapping("/{id}")
    public Nota buscar(@PathVariable Long id){
        Nota nota = service.buscar(id);
        alunoAccessGuard.garantir(nota.getAluno().getId());
        return nota;
    }

    /** Histórico de notas do aluno em todos os períodos letivos — acessível ao próprio aluno. */
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

    /**
     * Recalcula (deriva) a nota do aluno numa disciplina/período letivo a
     * partir dos simulados concluídos — este é o único jeito de gerar/alterar
     * uma Nota agora; não existe mais POST/PUT de total livre. Restrito a
     * PROFESSOR/ADMINISTRADOR (ver SecurityConfig): é uma operação de
     * reprocessamento, não uma consulta do aluno.
     */
    @PostMapping("/recalcular")
    public Nota recalcular(@RequestParam Long alunoId, @RequestParam Long disciplinaId, @RequestParam String periodoLetivo) {
        return service.recalcular(alunoId, disciplinaId, periodoLetivo);
    }

    /** Restrito a ADMINISTRADOR (ver SecurityConfig). */
    @DeleteMapping("/{id}") public void deletar(@PathVariable Long id){ service.deletar(id); }
}
