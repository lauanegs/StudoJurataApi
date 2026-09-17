package studojurata_api.ia.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.dto.SimuladoResponseDTO;
import studojurata_api.ia.dto.GerarSimuladoIARequest;
import studojurata_api.ia.dto.SimuladoGeradoIAResponseDTO;
import studojurata_api.ia.mapper.SimuladoGeradoIAMapper;
import studojurata_api.ia.service.GeracaoSimuladoIAService;
import studojurata_api.mapper.SimuladoMapper;

import java.util.List;

/**
 * O simulado gerado nasce em RASCUNHO: só chega ao aluno depois que o
 * professor aprova as questões e lança o simulado.
 */
@RestController
@RequestMapping("/ia/geracao")
@RequiredArgsConstructor
public class GeracaoIAController {

    private final GeracaoSimuladoIAService service;
    private final SimuladoMapper simuladoMapper;
    private final SimuladoGeradoIAMapper simuladoGeradoIAMapper;

    @PostMapping("/simulado")
    public SimuladoResponseDTO gerarSimulado(@Valid @RequestBody GerarSimuladoIARequest request) {
        return simuladoMapper.toResponseDTO(service.gerarParaAluno(
                request.getAlunoId(),
                request.getConteudoPlanoId(),
                request.getNivelDificuldade(),
                request.getMotivos()));
    }

    /** Simulados criados manualmente não aparecem aqui — só os gerados pela IA têm esse vínculo. */
    @GetMapping("/simulado")
    public List<SimuladoGeradoIAResponseDTO> listarVinculos() {
        return service.listarGerados().stream().map(simuladoGeradoIAMapper::toResponseDTO).toList();
    }
}
