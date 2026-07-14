package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.model.AlunoTurma;
import studojurata_api.service.AlunoTurmaService;

import java.time.LocalDate;
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

    /** Histórico completo de matrículas de uma turma (ativas, concluídas, canceladas, transferidas). */
    @GetMapping("/turma/{turmaId}/historico")
    public List<AlunoTurma> historicoPorTurma(@PathVariable Long turmaId) { return service.historicoPorTurma(turmaId); }

    /** Apenas os alunos com matrícula ativa na turma. */
    @GetMapping("/turma/{turmaId}/ativos")
    public List<AlunoTurma> ativosPorTurma(@PathVariable Long turmaId) { return service.ativosPorTurma(turmaId); }

    /** Quantidade de alunos ativos na turma, calculada sempre a partir das matrículas. */
    @GetMapping("/turma/{turmaId}/quantidade-ativos")
    public long quantidadeAtivos(@PathVariable Long turmaId) { return service.contarAtivosPorTurma(turmaId); }

    /** Histórico completo de matrículas de um aluno em todas as turmas. */
    @GetMapping("/aluno/{alunoId}/historico")
    public List<AlunoTurma> historicoPorAluno(@PathVariable Long alunoId) { return service.historicoPorAluno(alunoId); }

    @PostMapping
    public AlunoTurma matricular(@RequestBody AlunoTurma o) { return service.matricular(o); }

    @PutMapping("/{id}")
    public AlunoTurma atualizar(@PathVariable Long id, @RequestBody AlunoTurma o) { return service.atualizar(id, o); }

    /** Cancela a matrícula (soft delete), preservando o histórico. */
    @PostMapping("/{id}/cancelar")
    public AlunoTurma cancelar(@PathVariable Long id,
                                @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate dataFim) {
        return service.cancelar(id, dataFim);
    }

    /** Conclui a matrícula (encerramento natural do ciclo), preservando o histórico. */
    @PostMapping("/{id}/concluir")
    public AlunoTurma concluir(@PathVariable Long id,
                                @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate dataFim) {
        return service.concluir(id, dataFim);
    }

    /** Transfere o aluno da matrícula informada para outra turma. */
    @PostMapping("/{id}/transferir")
    public AlunoTurma transferir(@PathVariable Long id,
                                  @RequestParam Long turmaDestinoId,
                                  @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate dataTransferencia) {
        return service.transferir(id, turmaDestinoId, dataTransferencia);
    }

    /**
     * Exclusão física — mantida apenas por compatibilidade. Prefira
     * /{id}/cancelar para preservar o histórico pedagógico.
     */
    @DeleteMapping("/{id}")
    public void deletar(@PathVariable Long id) { service.deletar(id); }
}