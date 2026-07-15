package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.model.Alternativa;
import studojurata_api.service.AlternativaService;

import java.util.List;

@RestController
@RequestMapping("/alternativas")
@RequiredArgsConstructor
public class AlternativaController {

    private final AlternativaService service;

    @GetMapping
    public List<Alternativa> listar() { return service.listar(); }

    @GetMapping("/{id}")
    public Alternativa buscar(@PathVariable Long id) { return service.buscar(id); }

    @PostMapping
    public Alternativa salvar(@RequestBody Alternativa obj) { return service.salvar(obj); }

    @PutMapping("/{id}")
    public Alternativa atualizar(@PathVariable Long id, @RequestBody Alternativa obj) {
        return service.atualizar(id, obj);
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable Long id) { service.deletar(id); }
}
