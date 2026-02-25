package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.model.Turma;
import studojurata_api.repository.TurmaRepository;

import java.util.List;

@RestController
@RequestMapping("/turmas")
@RequiredArgsConstructor
public class TurmaController {

    private final TurmaRepository repository;

    @GetMapping public List<Turma> listar(){ return repository.findAll();}
    @GetMapping("/{id}") public Turma buscar(@PathVariable Long id){ return repository.findById(id).orElseThrow();}
    @PostMapping public Turma salvar(@RequestBody Turma o){ return repository.save(o);}
    @PutMapping("/{id}") public Turma atualizar(@PathVariable Long id,@RequestBody Turma o){ o.setId(id); return repository.save(o);}
    @DeleteMapping("/{id}") public void deletar(@PathVariable Long id){ repository.deleteById(id);}
}