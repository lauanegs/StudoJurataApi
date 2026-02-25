package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.model.Aula;
import studojurata_api.repository.AulaRepository;

import java.util.List;

@RestController
@RequestMapping("/aulas")
@RequiredArgsConstructor
public class AulaController {

    private final AulaRepository repository;

    @GetMapping public List<Aula> listar(){ return repository.findAll();}
    @GetMapping("/{id}") public Aula buscar(@PathVariable Long id){ return repository.findById(id).orElseThrow();}
    @PostMapping public Aula salvar(@RequestBody Aula o){ return repository.save(o);}
    @PutMapping("/{id}") public Aula atualizar(@PathVariable Long id,@RequestBody Aula o){ o.setId(id); return repository.save(o);}
    @DeleteMapping("/{id}") public void deletar(@PathVariable Long id){ repository.deleteById(id);}
}