package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.model.ConteudoPlano;
import studojurata_api.repository.ConteudoPlanoRepository;

import java.util.List;

@RestController
@RequestMapping("/conteudo-plano")
@RequiredArgsConstructor
public class ConteudoPlanoController {

    private final ConteudoPlanoRepository repository;

    @GetMapping public List<ConteudoPlano> listar(){ return repository.findAll();}
    @GetMapping("/{id}") public ConteudoPlano buscar(@PathVariable Long id){ return repository.findById(id).orElseThrow();}
    @PostMapping public ConteudoPlano salvar(@RequestBody ConteudoPlano o){ return repository.save(o);}
    @PutMapping("/{id}") public ConteudoPlano atualizar(@PathVariable Long id,@RequestBody ConteudoPlano o){ o.setId(id); return repository.save(o);}
    @DeleteMapping("/{id}") public void deletar(@PathVariable Long id){ repository.deleteById(id);}
}