package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.model.HorarioTurma;
import studojurata_api.service.HorarioTurmaService;

import java.util.List;

/** Consulta liberada a qualquer autenticado; escrita restrita a Professor/Administrador (ver SecurityConfig). */
@RestController
@RequiredArgsConstructor
public class HorarioTurmaController {

    private final HorarioTurmaService service;

    @GetMapping("/turmas/{turmaId}/horarios")
    public List<HorarioTurma> listarPorTurma(@PathVariable Long turmaId) {
        return service.listarPorTurma(turmaId);
    }

    @PostMapping("/turmas/{turmaId}/horarios")
    public HorarioTurma adicionar(@PathVariable Long turmaId, @RequestBody HorarioTurma obj) {
        return service.adicionar(turmaId, obj);
    }

    @DeleteMapping("/horarios/{id}")
    public void remover(@PathVariable Long id) {
        service.remover(id);
    }
}
