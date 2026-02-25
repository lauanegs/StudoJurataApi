package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.model.QuestaoConteudo;
import studojurata_api.repository.QuestaoConteudoRepository;

import java.util.List;

@RestController
@RequestMapping("/questao-conteudo")
@RequiredArgsConstructor
public class QuestaoConteudoController {

    private final QuestaoConteudoRepository repository;

    @GetMapping public List<QuestaoConteudo> listar(){ return repository.findAll();}
    @GetMapping("/{id}") public QuestaoConteudo buscar(@PathVariable Long id){ return repository.findById(id).orElseThrow();}
    @PostMapping public QuestaoConteudo salvar(@RequestBody QuestaoConteudo o){ return repository.save(o);}
    @PutMapping("/{id}") public QuestaoConteudo atualizar(@PathVariable Long id,@RequestBody QuestaoConteudo o){ o.setId(id); return repository.save(o);}
    @DeleteMapping("/{id}") public void deletar(@PathVariable Long id){ repository.deleteById(id);}
}