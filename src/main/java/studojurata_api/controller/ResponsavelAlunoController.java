package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.model.ResponsavelAluno;
import studojurata_api.service.ResponsavelAlunoService;

import java.util.List;
import java.util.Map;

/** Correção 5.1: passa a falar com ResponsavelAlunoService, não mais com o Repository diretamente. */
@RestController
@RequestMapping("/responsavel-aluno")
@RequiredArgsConstructor
public class ResponsavelAlunoController {

    private final ResponsavelAlunoService service;

    @GetMapping public List<ResponsavelAluno> listar(){ return service.listar(); }
    @GetMapping("/{id}") public ResponsavelAluno buscar(@PathVariable Long id){ return service.buscar(id); }
    @GetMapping("/por-aluno/{alunoId}") public List<ResponsavelAluno> porAluno(@PathVariable Long alunoId){ return service.porAluno(alunoId); }
    @GetMapping("/por-responsavel/{responsavelId}") public List<ResponsavelAluno> porResponsavel(@PathVariable Long responsavelId){ return service.porResponsavel(responsavelId); }
    @PostMapping public ResponsavelAluno salvar(@RequestBody ResponsavelAluno o){ return service.salvar(o); }
    @PutMapping("/{id}") public ResponsavelAluno atualizar(@PathVariable Long id,@RequestBody ResponsavelAluno o){ return service.atualizar(id, o); }

    /** Item 10.3 — checkbox de aceite: { "textoVersao": "..." }. */
    @PostMapping("/{id}/aceitar-termos")
    public ResponsavelAluno aceitarTermos(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return service.aceitarTermos(id, body.get("textoVersao"));
    }

    @DeleteMapping("/{id}") public void deletar(@PathVariable Long id){ service.deletar(id); }
}
