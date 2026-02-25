package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.model.Questao;
import studojurata_api.repository.QuestaoRepository;

import java.util.List;

@RestController
@RequestMapping("/questoes")
@RequiredArgsConstructor
public class QuestaoController {

    private final QuestaoRepository repository;

    @GetMapping public List<Questao> listar(){ return repository.findAll();}
    @GetMapping("/{id}") public Questao buscar(@PathVariable Long id){ return repository.findById(id).orElseThrow();}
    @PostMapping public Questao salvar(@RequestBody Questao o){ return repository.save(o);}
    @PutMapping("/{id}") public Questao atualizar(@PathVariable Long id,@RequestBody Questao o){ o.setId(id); return repository.save(o);}
    @DeleteMapping("/{id}") public void deletar(@PathVariable Long id){ repository.deleteById(id);}
}