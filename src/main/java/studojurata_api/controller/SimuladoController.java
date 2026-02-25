package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.model.Simulado;
import studojurata_api.repository.SimuladoRepository;

import java.util.List;

@RestController
@RequestMapping("/simulados")
@RequiredArgsConstructor
public class SimuladoController {

    private final SimuladoRepository repository;

    @GetMapping public List<Simulado> listar(){ return repository.findAll();}
    @GetMapping("/{id}") public Simulado buscar(@PathVariable Long id){ return repository.findById(id).orElseThrow();}
    @PostMapping public Simulado salvar(@RequestBody Simulado o){ return repository.save(o);}
    @PutMapping("/{id}") public Simulado atualizar(@PathVariable Long id,@RequestBody Simulado o){ o.setId(id); return repository.save(o);}
    @DeleteMapping("/{id}") public void deletar(@PathVariable Long id){ repository.deleteById(id);}
}