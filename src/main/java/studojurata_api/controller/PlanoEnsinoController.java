package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.model.PlanoEnsino;
import studojurata_api.repository.PlanoEnsinoRepository;

import java.util.List;

@RestController
@RequestMapping("/plano-ensino")
@RequiredArgsConstructor
public class PlanoEnsinoController {

    private final PlanoEnsinoRepository repository;

    @GetMapping public List<PlanoEnsino> listar(){ return repository.findAll();}
    @GetMapping("/{id}") public PlanoEnsino buscar(@PathVariable Long id){ return repository.findById(id).orElseThrow();}
    @PostMapping public PlanoEnsino salvar(@RequestBody PlanoEnsino o){ return repository.save(o);}
    @PutMapping("/{id}") public PlanoEnsino atualizar(@PathVariable Long id,@RequestBody PlanoEnsino o){ o.setId(id); return repository.save(o);}
    @DeleteMapping("/{id}") public void deletar(@PathVariable Long id){ repository.deleteById(id);}
}