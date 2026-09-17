package studojurata_api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.dto.EstenderDisponibilidadeRequest;
import studojurata_api.dto.LancarSimuladoRequest;
import studojurata_api.dto.SimuladoRequestDTO;
import studojurata_api.dto.SimuladoResponseDTO;
import studojurata_api.mapper.SimuladoMapper;
import studojurata_api.service.SimuladoService;

import java.util.List;

@RestController
@RequestMapping("/simulados")
@RequiredArgsConstructor
public class SimuladoController {

    private final SimuladoService service;
    private final SimuladoMapper mapper;

    @GetMapping
    public List<SimuladoResponseDTO> listar() {
        return service.listar().stream().map(mapper::toResponseDTO).toList();
    }

    @GetMapping("/{id}")
    public SimuladoResponseDTO buscar(@PathVariable Long id) {
        return mapper.toResponseDTO(service.buscar(id));
    }

    @PostMapping
    public SimuladoResponseDTO salvar(@Valid @RequestBody SimuladoRequestDTO dto) {
        return mapper.toResponseDTO(service.salvar(mapper.toEntity(dto)));
    }

    @PutMapping("/{id}")
    public SimuladoResponseDTO atualizar(@PathVariable Long id, @Valid @RequestBody SimuladoRequestDTO dto) {
        return mapper.toResponseDTO(service.atualizar(id, mapper.toEntity(dto)));
    }

    /** Cria uma tentativa PENDENTE para cada aluno elegível. */
    @PostMapping("/{id}/lancar")
    public SimuladoResponseDTO lancar(@PathVariable Long id, @RequestBody(required = false) LancarSimuladoRequest request) {
        return mapper.toResponseDTO(service.lancar(id, request));
    }

    @PostMapping("/{id}/encerrar")
    public SimuladoResponseDTO encerrar(@PathVariable Long id) {
        return mapper.toResponseDTO(service.encerrar(id));
    }

    /** Único campo editável depois do lançamento. */
    @PatchMapping("/{id}/disponibilidade")
    public SimuladoResponseDTO estenderDisponibilidade(
            @PathVariable Long id, @RequestBody EstenderDisponibilidadeRequest request) {
        return mapper.toResponseDTO(service.estenderDisponibilidade(id, request.getDataFim()));
    }
}
