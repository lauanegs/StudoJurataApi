package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.model.Curso;
import studojurata_api.model.PlanoEnsino;
import studojurata_api.service.CursoService;
import studojurata_api.service.PlanoEnsinoService;

import java.util.List;

/** Consulta liberada a qualquer autenticado; escrita restrita a Professor/Administrador (ver SecurityConfig). */
@RestController
@RequestMapping("/cursos")
@RequiredArgsConstructor
public class CursoController {

    private final CursoService service;
    private final PlanoEnsinoService planoEnsinoService;

    @GetMapping public List<Curso> listar(){ return service.listar(); }
    @GetMapping("/{id}") public Curso buscar(@PathVariable Long id){ return service.buscar(id); }
    @PostMapping public Curso salvar(@RequestBody Curso o){ return service.salvar(o); }
    @PutMapping("/{id}") public Curso atualizar(@PathVariable Long id, @RequestBody Curso o){ return service.atualizar(id, o); }
    @DeleteMapping("/{id}") public void deletar(@PathVariable Long id){ service.deletar(id); }

    /** Planos de ensino (um por disciplina do currículo) vinculados a este curso. */
    @GetMapping("/{id}/planos-ensino")
    public List<PlanoEnsino> planosDeEnsino(@PathVariable Long id) {
        return planoEnsinoService.listarPorCurso(id);
    }
}
