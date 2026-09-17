package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.dto.FinalizarSimuladoRequest;
import studojurata_api.dto.SimuladoAlunoResponseDTO;
import studojurata_api.mapper.SimuladoAlunoMapper;
import studojurata_api.service.SimuladoAlunoService;

import java.util.List;

@RestController
@RequestMapping("/simulado-aluno")
@RequiredArgsConstructor
public class SimuladoAlunoController {

    private final SimuladoAlunoService service;
    private final SimuladoAlunoMapper mapper;

    @GetMapping
    public List<SimuladoAlunoResponseDTO> listar() {
        return service.listar().stream().map(mapper::toResponseDTO).toList();
    }

    @GetMapping("/{id}")
    public SimuladoAlunoResponseDTO buscar(@PathVariable Long id) {
        return mapper.toResponseDTO(service.buscar(id));
    }

    @GetMapping("/aluno/{alunoId}")
    public List<SimuladoAlunoResponseDTO> listarPorAluno(@PathVariable Long alunoId) {
        return service.listarPorAluno(alunoId).stream().map(mapper::toResponseDTO).toList();
    }

    @GetMapping("/simulado/{simuladoId}")
    public List<SimuladoAlunoResponseDTO> listarPorSimulado(@PathVariable Long simuladoId) {
        return service.listarPorSimulado(simuladoId).stream().map(mapper::toResponseDTO).toList();
    }

    /** Além de nota e acertos, devolve em quantos dias cai a próxima revisão espaçada. */
    @PostMapping("/{id}/finalizar")
    public SimuladoAlunoResponseDTO finalizar(@PathVariable Long id, @RequestBody FinalizarSimuladoRequest request) {
        SimuladoAlunoService.ResultadoFinalizacao resultado = service.finalizar(id, request);
        SimuladoAlunoResponseDTO dto = mapper.toResponseDTO(resultado.simuladoAluno());
        dto.setDiasProximaRevisao(resultado.diasProximaRevisao());
        return dto;
    }
}
