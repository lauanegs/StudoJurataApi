package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.model.TurmaDisciplina;
import studojurata_api.service.TurmaDisciplinaService;

import java.util.List;

/** Correção 5.1: passa a falar com TurmaDisciplinaService (soft-delete), não mais com o Repository diretamente. */
@RestController
@RequestMapping("/turma-disciplina")
@RequiredArgsConstructor
public class TurmaDisciplinaController {

    private final TurmaDisciplinaService service;

    @GetMapping public List<TurmaDisciplina> listar(){ return service.listar(); }
    @GetMapping("/{id}") public TurmaDisciplina buscar(@PathVariable Long id){ return service.buscar(id); }
    @PostMapping public TurmaDisciplina salvar(@RequestBody TurmaDisciplina o){ return service.salvar(o); }
    @PutMapping("/{id}") public TurmaDisciplina atualizar(@PathVariable Long id,@RequestBody TurmaDisciplina o){ return service.atualizar(id, o); }
    @DeleteMapping("/{id}") public void deletar(@PathVariable Long id){ service.deletar(id); }
}
