package studojurata_api.ia.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.dto.SimuladoResponseDTO;
import studojurata_api.ia.dto.GerarSimuladoIARequest;
import studojurata_api.ia.service.GeracaoSimuladoIAService;
import studojurata_api.mapper.SimuladoMapper;

/**
 * Aciona a geração automática de um simulado de reforço via IA (item 1.4).
 * O simulado retornado nasce em RASCUNHO — só é liberado ao aluno após o
 * professor revisar/aprovar as questões (endpoints já existentes em
 * /questoes/pendentes, /questoes/{id}/aprovar) e chamar
 * /simulados/{id}/lancar.
 */
@RestController
@RequestMapping("/ia/geracao")
@RequiredArgsConstructor
public class GeracaoIAController {

    private final GeracaoSimuladoIAService service;
    private final SimuladoMapper simuladoMapper;

    @PostMapping("/simulado")
    public SimuladoResponseDTO gerarSimulado(@RequestBody GerarSimuladoIARequest request) {
        return simuladoMapper.toResponseDTO(service.gerarParaAluno(
                request.getAlunoId(),
                request.getConteudoPlanoId(),
                request.getQuantidadeQuestoes(),
                request.getNivelDificuldade()));
    }
}
