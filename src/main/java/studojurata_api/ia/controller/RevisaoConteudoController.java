package studojurata_api.ia.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.ia.dto.RegistrarReforcoRequest;
import studojurata_api.ia.model.RevisaoConteudo;
import studojurata_api.ia.service.RevisaoConteudoService;

import java.util.List;

@RestController
@RequestMapping("/ia/revisao-conteudo")
@RequiredArgsConstructor
public class RevisaoConteudoController {

    private final RevisaoConteudoService service;

    @GetMapping("/aluno/{alunoId}")
    public List<RevisaoConteudo> listarPorAluno(@PathVariable Long alunoId) {
        return service.listarPorAluno(alunoId);
    }

    @GetMapping("/devidos")
    public List<RevisaoConteudo> listarDevidosHoje() {
        return service.listarDevidosHoje();
    }

    @GetMapping("/devidos/aluno/{alunoId}")
    public List<RevisaoConteudo> listarDevidosHojePorAluno(@PathVariable Long alunoId) {
        return service.listarDevidosHojePorAluno(alunoId);
    }

    @PostMapping("/registrar")
    public RevisaoConteudo registrar(@RequestBody RegistrarReforcoRequest request) {
        return service.registrarReforco(request.getAlunoId(), request.getConteudoPlanoId(), request.getNivelDominio());
    }
}
