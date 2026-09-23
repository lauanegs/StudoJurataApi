package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.model.ResponsavelAluno;
import studojurata_api.security.AlunoAccessGuard;
import studojurata_api.service.ResponsavelAlunoService;

import java.util.List;

@RestController
@RequestMapping("/responsavel-aluno")
@RequiredArgsConstructor
public class ResponsavelAlunoController {

    private final ResponsavelAlunoService service;
    private final AlunoAccessGuard alunoAccessGuard;

    /**
     * Responsáveis de um aluno: o próprio aluno, o professor que leciona para ele
     * ou o administrador. O vínculo carrega o responsável (com dados pessoais da
     * Pessoa), então a leitura passa pelo mesmo guard das demais rotas
     * indexadas por aluno — antes de consultar o repositório.
     */
    @GetMapping("/por-aluno/{alunoId}")
    public List<ResponsavelAluno> porAluno(@PathVariable Long alunoId) {
        alunoAccessGuard.garantir(alunoId);
        return service.porAluno(alunoId);
    }

    @GetMapping("/por-responsavel/{responsavelId}") public List<ResponsavelAluno> porResponsavel(@PathVariable Long responsavelId){ return service.porResponsavel(responsavelId); }
    @PostMapping public ResponsavelAluno salvar(@RequestBody ResponsavelAluno o){ return service.salvar(o); }
    @PutMapping("/{id}") public ResponsavelAluno atualizar(@PathVariable Long id,@RequestBody ResponsavelAluno o){ return service.atualizar(id, o); }

    @DeleteMapping("/{id}") public void deletar(@PathVariable Long id){ service.deletar(id); }
}
