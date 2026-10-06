package studojurata_api.ia.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.dto.simulado.SimuladoResponse;
import studojurata_api.ia.dto.GerarSimuladoIARequest;
import studojurata_api.ia.dto.SimuladoGeradoIAResponse;
import studojurata_api.ia.mapper.SimuladoGeradoIAMapper;
import studojurata_api.ia.service.GeracaoSimuladoIAService;
import studojurata_api.mapper.simulado.SimuladoMapper;

import java.util.List;

/** O simulado gerado nasce em RASCUNHO */
@RestController
@RequestMapping("/ia/geracao")
@RequiredArgsConstructor
public class GeracaoIAController {
    private final GeracaoSimuladoIAService service;
    private final SimuladoMapper simuladoMapper;
    private final SimuladoGeradoIAMapper simuladoGeradoIAMapper;

    @PostMapping("/simulado")
    public SimuladoResponse gerarSimulado(@Valid @RequestBody GerarSimuladoIARequest request) {
        return simuladoMapper.toResponse(service.gerarParaAluno(
                request.getAlunoId(),
                request.getConteudoPlanoId(),
                request.getNivelDificuldade(),
                request.getMotivos()));
    }

    /** Simulados criados manualmente não aparecem aqui — só os gerados pela IA têm esse vínculo */
    @GetMapping("/simulado")
    public List<SimuladoGeradoIAResponse> listarVinculos() {
        return service.listarGerados().stream().map(simuladoGeradoIAMapper::toResponse).toList();
    }
}
