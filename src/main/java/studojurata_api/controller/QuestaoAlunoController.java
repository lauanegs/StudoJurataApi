package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.model.QuestaoAluno;
import studojurata_api.repository.QuestaoAlunoRepository;

import java.util.List;

@RestController
@RequestMapping("/questao-aluno")
@RequiredArgsConstructor
public class QuestaoAlunoController {

    private final QuestaoAlunoRepository repository;

    @GetMapping public List<QuestaoAluno> listar(){ return repository.findAll();}
    @GetMapping("/{id}") public QuestaoAluno buscar(@PathVariable Long id){ return repository.findById(id).orElseThrow();}
    @PostMapping public QuestaoAluno salvar(@RequestBody QuestaoAluno o){ return repository.save(o);}
    @PutMapping("/{id}") public QuestaoAluno atualizar(@PathVariable Long id,@RequestBody QuestaoAluno o){ o.setId(id); return repository.save(o);}
    @DeleteMapping("/{id}") public void deletar(@PathVariable Long id){ repository.deleteById(id);}
}