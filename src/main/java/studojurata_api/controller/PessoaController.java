package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.model.Pessoa;
import studojurata_api.repository.PessoaRepository;

import java.util.List;

@RestController
@RequestMapping("/pessoas")
@RequiredArgsConstructor
public class PessoaController {

    private final PessoaRepository repository;

    @GetMapping public List<Pessoa> listar(){ return repository.findAll();}
    @GetMapping("/{id}") public Pessoa buscar(@PathVariable Long id){ return repository.findById(id).orElseThrow();}
    @PostMapping public Pessoa salvar(@RequestBody Pessoa o){ return repository.save(o);}
    @PutMapping("/{id}") public Pessoa atualizar(@PathVariable Long id,@RequestBody Pessoa o){ o.setId(id); return repository.save(o);}
    @DeleteMapping("/{id}") public void deletar(@PathVariable Long id){ repository.deleteById(id);}
}