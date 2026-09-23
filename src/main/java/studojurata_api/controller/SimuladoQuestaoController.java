package studojurata_api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.dto.SimuladoQuestaoRequestDTO;
import studojurata_api.dto.SimuladoQuestaoResponseDTO;
import studojurata_api.mapper.SimuladoQuestaoMapper;
import studojurata_api.service.SimuladoQuestaoService;

import java.util.List;

@RestController
@RequestMapping("/simulado-questao")
@RequiredArgsConstructor
public class SimuladoQuestaoController {

    private final SimuladoQuestaoService service;
    private final SimuladoQuestaoMapper mapper;

    @GetMapping
    public List<SimuladoQuestaoResponseDTO> listar() {
        return service.listar().stream().map(mapper::toResponseDTO).toList();
    }

    @PostMapping
    public SimuladoQuestaoResponseDTO salvar(@Valid @RequestBody SimuladoQuestaoRequestDTO dto) {
        return mapper.toResponseDTO(service.salvar(mapper.toEntity(dto)));
    }

    /** Tira a questão do simulado; a questão em si continua existindo. */
    @DeleteMapping("/simulado/{simuladoId}/questao/{questaoId}")
    public void desvincular(@PathVariable Long simuladoId, @PathVariable Long questaoId) {
        service.desvincular(simuladoId, questaoId);
    }
}
