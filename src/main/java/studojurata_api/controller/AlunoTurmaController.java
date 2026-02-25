package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.model.AlunoTurma;
import studojurata_api.repository.AlunoTurmaRepository;

import java.util.List;

@RestController
@RequestMapping("/aluno-turma")
@RequiredArgsConstructor
public class AlunoTurmaController {

    private final AlunoTurmaRepository repository;

    @GetMapping public List<AlunoTurma> listar(){ return repository.findAll();}
    @GetMapping("/{id}") public AlunoTurma buscar(@PathVariable Long id){ return repository.findById(id).orElseThrow();}
    @PostMapping public AlunoTurma salvar(@RequestBody AlunoTurma o){ return repository.save(o);}
    @PutMapping("/{id}") public AlunoTurma atualizar(@PathVariable Long id,@RequestBody AlunoTurma o){ o.setId(id); return repository.save(o);}
    @DeleteMapping("/{id}") public void deletar(@PathVariable Long id){ repository.deleteById(id);}
}