package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.model.Professor;
import studojurata_api.repository.ProfessorRepository;

import java.util.List;

@RestController
@RequestMapping("/professores")
@RequiredArgsConstructor
public class ProfessorController {

    private final ProfessorRepository repository;

    @GetMapping public List<Professor> listar(){ return repository.findAll();}
    @GetMapping("/{id}") public Professor buscar(@PathVariable Long id){ return repository.findById(id).orElseThrow();}
    @PostMapping public Professor salvar(@RequestBody Professor o){ return repository.save(o);}
    @PutMapping("/{id}") public Professor atualizar(@PathVariable Long id,@RequestBody Professor o){ o.setId(id); return repository.save(o);}
    @DeleteMapping("/{id}") public void deletar(@PathVariable Long id){ repository.deleteById(id);}
}