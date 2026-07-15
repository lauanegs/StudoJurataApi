package studojurata_api.ia.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import studojurata_api.ia.dto.RecomendacaoDTO;
import studojurata_api.ia.service.RecomendacaoService;

import java.util.List;

@RestController
@RequestMapping("/ia/recomendacoes")
@RequiredArgsConstructor
public class RecomendacaoController {

    private final RecomendacaoService service;

    @GetMapping("/aluno/{alunoId}")
    public List<RecomendacaoDTO> recomendarParaAluno(@PathVariable Long alunoId) {
        return service.recomendarParaAluno(alunoId);
    }
}
