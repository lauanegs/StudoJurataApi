package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.model.Responsavel;
import studojurata_api.repository.ResponsavelRepository;

import java.util.List;

@RestController
@RequestMapping("/responsaveis")
@RequiredArgsConstructor
public class ResponsavelController {

    private final ResponsavelRepository repository;

    @GetMapping public List<Responsavel> listar(){ return repository.findAll();}
    @GetMapping("/{id}") public Responsavel buscar(@PathVariable Long id){ return repository.findById(id).orElseThrow();}
    @PostMapping public Responsavel salvar(@RequestBody Responsavel o){ return repository.save(o);}
    @PutMapping("/{id}") public Responsavel atualizar(@PathVariable Long id,@RequestBody Responsavel o){ o.setId(id); return repository.save(o);}
    @DeleteMapping("/{id}") public void deletar(@PathVariable Long id){ repository.deleteById(id);}
}