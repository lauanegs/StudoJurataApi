package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.model.Curso;
import studojurata_api.model.CursoDisciplina;
import studojurata_api.service.CursoDisciplinaService;
import studojurata_api.service.CursoService;

import java.util.List;

@RestController
@RequestMapping("/cursos")
@RequiredArgsConstructor
public class CursoController {

    private final CursoService service;
    private final CursoDisciplinaService cursoDisciplinaService;

    @GetMapping public List<Curso> listar(){ return service.listar(); }
    @GetMapping("/{id}") public Curso buscar(@PathVariable Long id){ return service.buscarParaLeitura(id); }
    @PostMapping public Curso salvar(@RequestBody Curso o){ return service.salvar(o); }
    @PutMapping("/{id}") public Curso atualizar(@PathVariable Long id, @RequestBody Curso o){ return service.atualizar(id, o); }
    @DeleteMapping("/{id}") public void deletar(@PathVariable Long id){ service.deletar(id); }
    @PostMapping("/{id}/ativar") public Curso ativar(@PathVariable Long id){ return service.ativar(id); }

    /** Grade curricular (disciplinas + carga horária) deste curso — ver CursoDisciplina. */
    @GetMapping("/{id}/disciplinas")
    public List<CursoDisciplina> disciplinas(@PathVariable Long id) {
        return cursoDisciplinaService.listarPorCurso(id);
    }
}
