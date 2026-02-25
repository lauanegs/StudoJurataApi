package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.model.SimuladoQuestao;
import studojurata_api.repository.SimuladoQuestaoRepository;

import java.util.List;

@RestController
@RequestMapping("/simulado-questao")
@RequiredArgsConstructor
public class SimuladoQuestaoController {

    private final SimuladoQuestaoRepository repository;

    @GetMapping public List<SimuladoQuestao> listar(){ return repository.findAll();}
    @GetMapping("/{id}") public SimuladoQuestao buscar(@PathVariable Long id){ return repository.findById(id).orElseThrow();}
    @PostMapping public SimuladoQuestao salvar(@RequestBody SimuladoQuestao o){ return repository.save(o);}
    @PutMapping("/{id}") public SimuladoQuestao atualizar(@PathVariable Long id,@RequestBody SimuladoQuestao o){ o.setId(id); return repository.save(o);}
    @DeleteMapping("/{id}") public void deletar(@PathVariable Long id){ repository.deleteById(id);}
}