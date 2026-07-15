package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.dto.LancarSimuladoRequest;
import studojurata_api.dto.SimuladoRequestDTO;
import studojurata_api.dto.SimuladoResponseDTO;
import studojurata_api.mapper.SimuladoMapper;
import studojurata_api.model.Simulado;
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
    public SimuladoResponseDTO salvar(@RequestBody SimuladoRequestDTO dto) {
        return mapper.toResponseDTO(service.salvar(mapper.toEntity(dto)));
    }

    @PutMapping("/{id}")
    public SimuladoResponseDTO atualizar(@PathVariable Long id, @RequestBody SimuladoRequestDTO dto) {
        return mapper.toResponseDTO(service.atualizar(id, mapper.toEntity(dto)));
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable Long id) { service.deletar(id); }

    /** Lança o simulado: cria SimuladoAluno (PENDENTE) para os elegíveis (item 1.3). */
    @PostMapping("/{id}/lancar")
    public SimuladoResponseDTO lancar(@PathVariable Long id, @RequestBody(required = false) LancarSimuladoRequest request) {
        List<Long> alunoIds = request != null ? request.getAlunoIds() : null;
        Simulado simulado = service.lancar(id, alunoIds);
        return mapper.toResponseDTO(simulado);
    }

    /** Encerra manualmente o simulado, impedindo novas tentativas. */
    @PostMapping("/{id}/encerrar")
    public SimuladoResponseDTO encerrar(@PathVariable Long id) {
        return mapper.toResponseDTO(service.encerrar(id));
    }
}
