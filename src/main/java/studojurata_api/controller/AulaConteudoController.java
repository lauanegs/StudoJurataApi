package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.model.AulaConteudo;
import studojurata_api.repository.AulaConteudoRepository;

import java.util.List;

@RestController
@RequestMapping("/aula-conteudo")
@RequiredArgsConstructor
public class AulaConteudoController {

    private final AulaConteudoRepository repository;

    @GetMapping public List<AulaConteudo> listar(){ return repository.findAll();}
    @GetMapping("/{id}") public AulaConteudo buscar(@PathVariable Long id){ return repository.findById(id).orElseThrow();}
    @PostMapping public AulaConteudo salvar(@RequestBody AulaConteudo o){ return repository.save(o);}
    @PutMapping("/{id}") public AulaConteudo atualizar(@PathVariable Long id,@RequestBody AulaConteudo o){ o.setId(id); return repository.save(o);}
    @DeleteMapping("/{id}") public void deletar(@PathVariable Long id){ repository.deleteById(id);}
}