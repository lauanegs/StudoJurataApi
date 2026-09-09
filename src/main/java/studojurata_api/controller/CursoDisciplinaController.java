package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.model.CursoDisciplina;
import studojurata_api.service.CursoDisciplinaService;

import java.util.List;

@RestController
@RequestMapping("/curso-disciplina")
@RequiredArgsConstructor
public class CursoDisciplinaController {

    private final CursoDisciplinaService service;

    @GetMapping public List<CursoDisciplina> listar(){ return service.listar(); }
    @GetMapping("/{id}") public CursoDisciplina buscar(@PathVariable Long id){ return service.buscar(id); }
    @PostMapping public CursoDisciplina salvar(@RequestBody CursoDisciplina o){ return service.salvar(o); }
    @PutMapping("/{id}") public CursoDisciplina atualizar(@PathVariable Long id,@RequestBody CursoDisciplina o){ return service.atualizar(id, o); }
    @DeleteMapping("/{id}") public void deletar(@PathVariable Long id){ service.deletar(id); }
}
