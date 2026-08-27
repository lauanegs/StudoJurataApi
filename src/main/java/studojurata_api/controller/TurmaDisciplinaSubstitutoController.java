package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.model.TurmaDisciplinaSubstituto;
import studojurata_api.service.TurmaDisciplinaSubstitutoService;

import java.util.List;

/** Consulta liberada a qualquer autenticado; escrita restrita a Professor/Administrador (coberto por /turma-disciplina/** no SecurityConfig). */
@RestController
@RequiredArgsConstructor
public class TurmaDisciplinaSubstitutoController {

    private final TurmaDisciplinaSubstitutoService service;

    @GetMapping("/turma-disciplina/{turmaDisciplinaId}/substitutos")
    public List<TurmaDisciplinaSubstituto> listarPorTurmaDisciplina(@PathVariable Long turmaDisciplinaId) {
        return service.listarPorTurmaDisciplina(turmaDisciplinaId);
    }

    @PostMapping("/turma-disciplina/{turmaDisciplinaId}/substitutos")
    public TurmaDisciplinaSubstituto adicionar(
            @PathVariable Long turmaDisciplinaId, @RequestBody TurmaDisciplinaSubstituto obj) {
        return service.adicionar(turmaDisciplinaId, obj);
    }

    @DeleteMapping("/turma-disciplina-substituto/{id}")
    public void remover(@PathVariable Long id) {
        service.remover(id);
    }
}
