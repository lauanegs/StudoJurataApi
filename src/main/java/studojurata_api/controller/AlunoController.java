package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.model.Aluno;
import studojurata_api.repository.AlunoRepository;

import java.util.List;

@RestController
@RequestMapping("/alunos")
@RequiredArgsConstructor
public class AlunoController {

    private final AlunoRepository repository;

    @GetMapping public List<Aluno> listar(){ return repository.findAll();}
    @GetMapping("/{id}") public Aluno buscar(@PathVariable Long id){ return repository.findById(id).orElseThrow();}
    @PostMapping public Aluno salvar(@RequestBody Aluno o){ return repository.save(o);}
    @PutMapping("/{id}") public Aluno atualizar(@PathVariable Long id,@RequestBody Aluno o){ o.setId(id); return repository.save(o);}
    @DeleteMapping("/{id}") public void deletar(@PathVariable Long id){ repository.deleteById(id);}
}