package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.model.Disciplina;
import studojurata_api.repository.DisciplinaRepository;

import java.util.List;

@RestController
@RequestMapping("/disciplinas")
@RequiredArgsConstructor
public class DisciplinaController {

    private final DisciplinaRepository repository;

    @GetMapping public List<Disciplina> listar(){ return repository.findAll();}
    @GetMapping("/{id}") public Disciplina buscar(@PathVariable Long id){ return repository.findById(id).orElseThrow();}
    @PostMapping public Disciplina salvar(@RequestBody Disciplina o){ return repository.save(o);}
    @PutMapping("/{id}") public Disciplina atualizar(@PathVariable Long id,@RequestBody Disciplina o){ o.setId(id); return repository.save(o);}
    @DeleteMapping("/{id}") public void deletar(@PathVariable Long id){ repository.deleteById(id);}
}