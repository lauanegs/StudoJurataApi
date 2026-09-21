package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.dto.FinalizarSimuladoRequest;
import studojurata_api.dto.SimuladoAlunoResponseDTO;
import studojurata_api.mapper.SimuladoAlunoMapper;
import studojurata_api.model.Simulado;
import studojurata_api.model.SimuladoAluno;
import studojurata_api.security.AlunoAccessGuard;
import studojurata_api.service.SimuladoAlunoService;
import studojurata_api.service.SimuladoService;

import java.util.List;

@RestController
@RequestMapping("/simulado-aluno")
@RequiredArgsConstructor
public class SimuladoAlunoController {

    private final SimuladoAlunoService service;
    private final SimuladoAlunoMapper mapper;
    private final SimuladoService simuladoService;
    private final AlunoAccessGuard alunoAccessGuard;

    @GetMapping
    public List<SimuladoAlunoResponseDTO> listar() {
        return service.listar().stream().map(mapper::toResponseDTO).toList();
    }

    @GetMapping("/{id}")
    public SimuladoAlunoResponseDTO buscar(@PathVariable Long id) {
        SimuladoAluno tentativa = service.buscar(id);
        alunoAccessGuard.garantir(alunoDaTentativa(tentativa));
        return mapper.toResponseDTO(tentativa);
    }

    @GetMapping("/aluno/{alunoId}")
    public List<SimuladoAlunoResponseDTO> listarPorAluno(@PathVariable Long alunoId) {
        alunoAccessGuard.garantir(alunoId);
        return service.listarPorAluno(alunoId).stream().map(mapper::toResponseDTO).toList();
    }

    @GetMapping("/simulado/{simuladoId}")
    public List<SimuladoAlunoResponseDTO> listarPorSimulado(@PathVariable Long simuladoId) {
        Simulado simulado = simuladoService.buscar(simuladoId);

        // Simulado órfão (sem turma) mantém o comportamento atual, sem regra
        // definitiva nesta etapa — decisão registrada para o levantamento de uso
        // no C2. Simulado com turma é escopado ao professor dono dela.
        if (simulado.getTurma() != null) {
            alunoAccessGuard.garantirAcessoATurma(simulado.getTurma().getId());
        }

        return service.listarPorSimulado(simuladoId).stream().map(mapper::toResponseDTO).toList();
    }

    /** Id do aluno dono da tentativa; null vira recusa no guard (falha fechado). */
    private Long alunoDaTentativa(SimuladoAluno tentativa) {
        return tentativa.getAluno() != null ? tentativa.getAluno().getId() : null;
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
