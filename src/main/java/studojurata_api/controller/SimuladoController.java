package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.dto.LancarSimuladoRequest;
import studojurata_api.model.Simulado;
import studojurata_api.service.SimuladoService;

import java.util.List;

@RestController
@RequestMapping("/simulados")
@RequiredArgsConstructor
public class SimuladoController {

    private final SimuladoService service;

    @GetMapping public List<Simulado> listar(){ return service.listar();}
    @GetMapping("/{id}") public Simulado buscar(@PathVariable Long id){ return service.buscar(id);}
    @PostMapping public Simulado salvar(@RequestBody Simulado o){ return service.salvar(o);}
    @PutMapping("/{id}") public Simulado atualizar(@PathVariable Long id,@RequestBody Simulado o){ return service.atualizar(id, o);}
    @DeleteMapping("/{id}") public void deletar(@PathVariable Long id){ service.deletar(id);}

    /** Lança o simulado: cria SimuladoAluno (PENDENTE) para os elegíveis (item 1.3). */
    @PostMapping("/{id}/lancar")
    public Simulado lancar(@PathVariable Long id, @RequestBody(required = false) LancarSimuladoRequest request) {
        List<Long> alunoIds = request != null ? request.getAlunoIds() : null;
        return service.lancar(id, alunoIds);
    }

    /** Encerra manualmente o simulado, impedindo novas tentativas. */
    @PostMapping("/{id}/encerrar")
    public Simulado encerrar(@PathVariable Long id) {
        return service.encerrar(id);
    }
}
