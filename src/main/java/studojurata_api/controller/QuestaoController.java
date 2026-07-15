package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.model.Questao;
import studojurata_api.service.QuestaoService;

import java.util.List;

@RestController
@RequestMapping("/questoes")
@RequiredArgsConstructor
public class QuestaoController {

    private final QuestaoService service;

    @GetMapping public List<Questao> listar(){ return service.listar();}
    @GetMapping("/{id}") public Questao buscar(@PathVariable Long id){ return service.buscar(id);}
    @PostMapping public Questao salvar(@RequestBody Questao o){ return service.salvar(o);}
    @PutMapping("/{id}") public Questao atualizar(@PathVariable Long id,@RequestBody Questao o){ return service.atualizar(id, o);}
    @DeleteMapping("/{id}") public void deletar(@PathVariable Long id){ service.deletar(id);}

    /** Fila da tela de Revisão do professor (itens 1.4/7.3). */
    @GetMapping("/pendentes")
    public List<Questao> listarPendentes() { return service.listarPendentes(); }

    @PostMapping("/{id}/aprovar")
    public Questao aprovar(@PathVariable Long id) { return service.aprovar(id); }

    @PostMapping("/{id}/rejeitar")
    public Questao rejeitar(@PathVariable Long id) { return service.rejeitar(id); }
}
