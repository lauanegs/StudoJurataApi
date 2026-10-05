package studojurata_api.controller.pessoa;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.model.pessoa.Professor;
import studojurata_api.model.turma.TurmaDisciplina;
import studojurata_api.service.nota.DesempenhoService;
import studojurata_api.service.pessoa.ProfessorService;

import java.util.List;

@RestController
@RequestMapping("/professores")
@RequiredArgsConstructor
public class ProfessorController {

    private final ProfessorService service;
    private final DesempenhoService desempenhoService;

    @GetMapping public List<Professor> listar(){ return service.listar(); }
    @GetMapping("/{id}") public Professor buscar(@PathVariable Long id){ return service.buscar(id); }
    @GetMapping("/{id}/turmas") public List<TurmaDisciplina> turmasLecionadas(@PathVariable Long id){ return service.turmasLecionadas(id); }
    @GetMapping("/{id}/desempenho") public List<DesempenhoService.TentativaDesempenho> desempenho(@PathVariable Long id){ return desempenhoService.tentativasDoProfessor(id); }
    @PostMapping public Professor salvar(@RequestBody Professor o){ return service.salvar(o); }
    @PutMapping("/{id}") public Professor atualizar(@PathVariable Long id,@RequestBody Professor o){ return service.atualizar(id, o); }
    @DeleteMapping("/{id}") public void deletar(@PathVariable Long id){ service.deletar(id); }
    @PostMapping("/{id}/ativar") public Professor ativar(@PathVariable Long id){ return service.ativar(id); }
}
