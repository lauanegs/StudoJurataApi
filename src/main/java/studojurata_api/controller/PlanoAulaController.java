package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.model.PlanoAula;
import studojurata_api.service.PlanoAulaService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/plano-aula")
@RequiredArgsConstructor
public class PlanoAulaController {

    private final PlanoAulaService service;

    @GetMapping
    public List<PlanoAula> listar() { return service.listar(); }

    @GetMapping("/{id}")
    public PlanoAula buscar(@PathVariable Long id) { return service.buscar(id); }

    /** Planos de aula de uma TurmaDisciplina específica. */
    @GetMapping("/turma-disciplina/{turmaDisciplinaId}")
    public List<PlanoAula> listarPorTurmaDisciplina(@PathVariable Long turmaDisciplinaId) {
        return service.listarPorTurmaDisciplina(turmaDisciplinaId);
    }

    /** Estatísticas exibidas na tela "Aulas": aulas realizadas / total previsto e carga horária realizada. */
    @GetMapping("/{id}/estatisticas")
    public Map<String, Object> estatisticas(@PathVariable Long id) { return service.estatisticas(id); }

    @PostMapping
    public PlanoAula salvar(@RequestBody PlanoAula o) { return service.salvar(o); }

    @PutMapping("/{id}")
    public PlanoAula atualizar(@PathVariable Long id, @RequestBody PlanoAula o) { return service.atualizar(id, o); }

    /** Soft delete: marca o plano de aula como INATIVO, preservando o histórico (ver 4.3). */
    @DeleteMapping("/{id}")
    public void deletar(@PathVariable Long id) { service.deletar(id); }
}
