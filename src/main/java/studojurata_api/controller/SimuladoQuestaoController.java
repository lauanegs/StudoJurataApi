package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.model.SimuladoQuestao;
import studojurata_api.service.SimuladoQuestaoService;

import java.util.List;

@RestController
@RequestMapping("/simulado-questao")
@RequiredArgsConstructor
public class SimuladoQuestaoController {

    private final SimuladoQuestaoService service;

    @GetMapping public List<SimuladoQuestao> listar(){ return service.listar();}
    @GetMapping("/{id}") public SimuladoQuestao buscar(@PathVariable Long id){ return service.buscar(id);}
    @PostMapping public SimuladoQuestao salvar(@RequestBody SimuladoQuestao o){ return service.salvar(o);}
    @PutMapping("/{id}") public SimuladoQuestao atualizar(@PathVariable Long id,@RequestBody SimuladoQuestao o){ return service.atualizar(id, o);}
    @DeleteMapping("/{id}") public void deletar(@PathVariable Long id){ service.deletar(id);}

    /** Remove (soft-delete) a questão do simulado, preservando histórico de respostas. */
    @PostMapping("/{id}/remover")
    public SimuladoQuestao remover(@PathVariable Long id) { return service.remover(id); }
}
