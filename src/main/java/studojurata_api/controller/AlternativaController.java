package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.model.Alternativa;
import studojurata_api.repository.AlternativaRepository;

import java.util.List;

@RestController
@RequestMapping("/alternativas")
@RequiredArgsConstructor
public class AlternativaController {

    private final AlternativaRepository repository;

    @GetMapping
    public List<Alternativa> listar() { return repository.findAll(); }

    @GetMapping("/{id}")
    public Alternativa buscar(@PathVariable Long id) { return repository.findById(id).orElseThrow(); }

    @PostMapping
    public Alternativa salvar(@RequestBody Alternativa obj) { return repository.save(obj); }

    @PutMapping("/{id}")
    public Alternativa atualizar(@PathVariable Long id, @RequestBody Alternativa obj) {
        obj.setId(id);
        return repository.save(obj);
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable Long id) { repository.deleteById(id); }
}