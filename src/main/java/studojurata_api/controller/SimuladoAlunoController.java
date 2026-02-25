package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.model.SimuladoAluno;
import studojurata_api.repository.SimuladoAlunoRepository;

import java.util.List;

@RestController
@RequestMapping("/simulado-aluno")
@RequiredArgsConstructor
public class SimuladoAlunoController {

    private final SimuladoAlunoRepository repository;

    @GetMapping public List<SimuladoAluno> listar(){ return repository.findAll();}
    @GetMapping("/{id}") public SimuladoAluno buscar(@PathVariable Long id){ return repository.findById(id).orElseThrow();}
    @PostMapping public SimuladoAluno salvar(@RequestBody SimuladoAluno o){ return repository.save(o);}
    @PutMapping("/{id}") public SimuladoAluno atualizar(@PathVariable Long id,@RequestBody SimuladoAluno o){ o.setId(id); return repository.save(o);}
    @DeleteMapping("/{id}") public void deletar(@PathVariable Long id){ repository.deleteById(id);}
}