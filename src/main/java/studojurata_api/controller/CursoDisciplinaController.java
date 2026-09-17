package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.model.CursoDisciplina;
import studojurata_api.service.CursoDisciplinaService;

@RestController
@RequestMapping("/curso-disciplina")
@RequiredArgsConstructor
public class CursoDisciplinaController {

    private final CursoDisciplinaService service;

    @PostMapping public CursoDisciplina salvar(@RequestBody CursoDisciplina o){ return service.salvar(o); }
    @DeleteMapping("/{id}") public void deletar(@PathVariable Long id){ service.deletar(id); }
}
