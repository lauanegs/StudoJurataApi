package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.model.QuestaoConteudo;
import studojurata_api.service.QuestaoConteudoService;

import java.util.List;

/** Correção 5.1: passa a falar com QuestaoConteudoService, não mais com o Repository diretamente. */
@RestController
@RequestMapping("/questao-conteudo")
@RequiredArgsConstructor
public class QuestaoConteudoController {

    private final QuestaoConteudoService service;

    @GetMapping public List<QuestaoConteudo> listar(){ return service.listar(); }
    @GetMapping("/{id}") public QuestaoConteudo buscar(@PathVariable Long id){ return service.buscar(id); }
    @PostMapping public QuestaoConteudo salvar(@RequestBody QuestaoConteudo o){ return service.salvar(o); }
    @PutMapping("/{id}") public QuestaoConteudo atualizar(@PathVariable Long id,@RequestBody QuestaoConteudo o){ return service.atualizar(id, o); }
    @DeleteMapping("/{id}") public void deletar(@PathVariable Long id){ service.deletar(id); }
}
