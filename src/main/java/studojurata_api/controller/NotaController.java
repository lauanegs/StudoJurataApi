package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.model.Nota;
import studojurata_api.repository.NotaRepository;

import java.util.List;

@RestController
@RequestMapping("/notas")
@RequiredArgsConstructor
public class NotaController {

    private final NotaRepository repository;

    @GetMapping public List<Nota> listar(){ return repository.findAll();}
    @GetMapping("/{id}") public Nota buscar(@PathVariable Long id){ return repository.findById(id).orElseThrow();}
    @PostMapping public Nota salvar(@RequestBody Nota o){ return repository.save(o);}
    @PutMapping("/{id}") public Nota atualizar(@PathVariable Long id,@RequestBody Nota o){ o.setId(id); return repository.save(o);}
    @DeleteMapping("/{id}") public void deletar(@PathVariable Long id){ repository.deleteById(id);}
}