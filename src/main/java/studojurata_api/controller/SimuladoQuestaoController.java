package studojurata_api.controller;

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

    @GetMapping("/{id}")
    public SimuladoQuestaoResponseDTO buscar(@PathVariable Long id) {
        return mapper.toResponseDTO(service.buscar(id));
    }

    @PostMapping
    public SimuladoQuestaoResponseDTO salvar(@RequestBody SimuladoQuestaoRequestDTO dto) {
        return mapper.toResponseDTO(service.salvar(mapper.toEntity(dto)));
    }

    @PutMapping("/{id}")
    public SimuladoQuestaoResponseDTO atualizar(@PathVariable Long id, @RequestBody SimuladoQuestaoRequestDTO dto) {
        return mapper.toResponseDTO(service.atualizar(id, mapper.toEntity(dto)));
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable Long id) { service.deletar(id); }

    /** Remove (soft-delete) a questão do simulado, preservando histórico de respostas. */
    @PostMapping("/{id}/remover")
    public SimuladoQuestaoResponseDTO remover(@PathVariable Long id) {
        return mapper.toResponseDTO(service.remover(id));
    }
}
