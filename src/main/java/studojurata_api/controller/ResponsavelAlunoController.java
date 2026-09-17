package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.model.ResponsavelAluno;
import studojurata_api.service.ResponsavelAlunoService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/responsavel-aluno")
@RequiredArgsConstructor
public class ResponsavelAlunoController {

    private final ResponsavelAlunoService service;

    @GetMapping public List<ResponsavelAluno> listar(){ return service.listar(); }
    @GetMapping("/por-aluno/{alunoId}") public List<ResponsavelAluno> porAluno(@PathVariable Long alunoId){ return service.porAluno(alunoId); }
    @GetMapping("/por-responsavel/{responsavelId}") public List<ResponsavelAluno> porResponsavel(@PathVariable Long responsavelId){ return service.porResponsavel(responsavelId); }
    @PostMapping public ResponsavelAluno salvar(@RequestBody ResponsavelAluno o){ return service.salvar(o); }
    @PutMapping("/{id}") public ResponsavelAluno atualizar(@PathVariable Long id,@RequestBody ResponsavelAluno o){ return service.atualizar(id, o); }

    /** Corpo: { "textoVersao": "..." } — o texto exibido no momento do aceite. */
    @PostMapping("/{id}/aceitar-termos")
    public ResponsavelAluno aceitarTermos(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return service.aceitarTermos(id, body.get("textoVersao"));
    }

    @DeleteMapping("/{id}") public void deletar(@PathVariable Long id){ service.deletar(id); }
}
