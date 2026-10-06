package studojurata_api.controller.simulado;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.dto.simulado.EstenderDisponibilidadeRequest;
import studojurata_api.dto.simulado.LancarSimuladoRequest;
import studojurata_api.dto.simulado.SimuladoRequest;
import studojurata_api.dto.simulado.SimuladoResponse;
import studojurata_api.mapper.simulado.SimuladoMapper;
import studojurata_api.service.simulado.SimuladoService;

import java.util.List;

@RestController
@RequestMapping("/simulados")
@RequiredArgsConstructor
public class SimuladoController {

    private final SimuladoService service;
    private final SimuladoMapper mapper;

    @GetMapping
    public List<SimuladoResponse> listar() {
        return service.listar().stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    public SimuladoResponse buscar(@PathVariable Long id) {
        return mapper.toResponse(service.buscarParaLeitura(id));
    }

    @PostMapping
    public SimuladoResponse salvar(@Valid @RequestBody SimuladoRequest dto) {
        return mapper.toResponse(service.salvar(mapper.toEntity(dto)));
    }

    @PutMapping("/{id}")
    public SimuladoResponse atualizar(@PathVariable Long id, @Valid @RequestBody SimuladoRequest dto) {
        return mapper.toResponse(service.atualizar(id, mapper.toEntity(dto)));
    }

    /** Cria uma tentativa PENDENTE para cada aluno elegível. */
    @PostMapping("/{id}/lancar")
    public SimuladoResponse lancar(@PathVariable Long id, @RequestBody(required = false) LancarSimuladoRequest request) {
        return mapper.toResponse(service.lancar(id, request));
    }

    @PostMapping("/{id}/encerrar")
    public SimuladoResponse encerrar(@PathVariable Long id) {
        return mapper.toResponse(service.encerrar(id));
    }

    /** Único campo editável depois do lançamento. */
    @PatchMapping("/{id}/disponibilidade")
    public SimuladoResponse estenderDisponibilidade(
            @PathVariable Long id, @RequestBody EstenderDisponibilidadeRequest request) {
        return mapper.toResponse(service.estenderDisponibilidade(id, request.getDataFim()));
    }
}
