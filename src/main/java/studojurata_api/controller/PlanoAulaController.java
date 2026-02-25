package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.model.PlanoAula;
import studojurata_api.repository.PlanoAulaRepository;

import java.util.List;

@RestController
@RequestMapping("/plano-aula")
@RequiredArgsConstructor
public class PlanoAulaController {

    private final PlanoAulaRepository repository;

    @GetMapping public List<PlanoAula> listar(){ return repository.findAll();}
    @GetMapping("/{id}") public PlanoAula buscar(@PathVariable Long id){ return repository.findById(id).orElseThrow();}
    @PostMapping public PlanoAula salvar(@RequestBody PlanoAula o){ return repository.save(o);}
    @PutMapping("/{id}") public PlanoAula atualizar(@PathVariable Long id,@RequestBody PlanoAula o){ o.setId(id); return repository.save(o);}
    @DeleteMapping("/{id}") public void deletar(@PathVariable Long id){ repository.deleteById(id);}
}