package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.dto.FinalizarSimuladoRequest;
import studojurata_api.model.SimuladoAluno;
import studojurata_api.service.SimuladoAlunoService;

import java.util.List;

@RestController
@RequestMapping("/simulado-aluno")
@RequiredArgsConstructor
public class SimuladoAlunoController {

    private final SimuladoAlunoService service;

    @GetMapping public List<SimuladoAluno> listar(){ return service.listar();}
    @GetMapping("/{id}") public SimuladoAluno buscar(@PathVariable Long id){ return service.buscar(id);}
    @GetMapping("/aluno/{alunoId}") public List<SimuladoAluno> listarPorAluno(@PathVariable Long alunoId){ return service.listarPorAluno(alunoId);}
    @GetMapping("/simulado/{simuladoId}") public List<SimuladoAluno> listarPorSimulado(@PathVariable Long simuladoId){ return service.listarPorSimulado(simuladoId);}
    @PostMapping public SimuladoAluno salvar(@RequestBody SimuladoAluno o){ return service.salvar(o);}
    @DeleteMapping("/{id}") public void deletar(@PathVariable Long id){ service.deletar(id);}

    /** Finaliza a tentativa do aluno, calculando nota/acertos/tempoGasto (itens 1.3, 2.4, 4.2). */
    @PostMapping("/{id}/finalizar")
    public SimuladoAluno finalizar(@PathVariable Long id, @RequestBody FinalizarSimuladoRequest request) {
        return service.finalizar(id, request);
    }
}
