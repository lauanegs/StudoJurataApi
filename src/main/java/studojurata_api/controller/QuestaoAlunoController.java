package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.model.QuestaoAluno;
import studojurata_api.service.QuestaoAlunoService;

import java.util.List;

@RestController
@RequestMapping("/questao-aluno")
@RequiredArgsConstructor
public class QuestaoAlunoController {

    private final QuestaoAlunoService service;

    @GetMapping public List<QuestaoAluno> listar(){ return service.listar();}
    @GetMapping("/{id}") public QuestaoAluno buscar(@PathVariable Long id){ return service.buscar(id);}
    @GetMapping("/simulado-aluno/{simuladoAlunoId}") public List<QuestaoAluno> listarPorSimuladoAluno(@PathVariable Long simuladoAlunoId){ return service.listarPorSimuladoAluno(simuladoAlunoId);}
    @PostMapping public QuestaoAluno salvar(@RequestBody QuestaoAluno o){ return service.salvar(o);}
    @PutMapping("/{id}") public QuestaoAluno atualizar(@PathVariable Long id,@RequestBody QuestaoAluno o){ return service.atualizar(id, o);}
    @DeleteMapping("/{id}") public void deletar(@PathVariable Long id){ service.deletar(id);}
}
