package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.dto.FinalizarSimuladoRequest;
import studojurata_api.dto.SimuladoAlunoResponseDTO;
import studojurata_api.mapper.SimuladoAlunoMapper;
import studojurata_api.model.Simulado;
import studojurata_api.model.SimuladoAluno;
import studojurata_api.security.AlunoAccessGuard;
import studojurata_api.security.SimuladoAccessGuard;
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
    private final SimuladoAccessGuard simuladoAccessGuard;

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

        // Simulado sem turma não tem escopo verificável: só o administrador
        // consulta as tentativas dele. Com turma, vale o escopo da turma — o que
        // já barra o aluno, que só vê as próprias tentativas pelas rotas dele.
        simuladoAccessGuard.garantirLeituraDeOrfao(simulado);
        if (simulado.getTurma() != null) {
            alunoAccessGuard.garantirAcessoATurma(simulado.getTurma().getId());
        }

        return service.listarPorSimulado(simuladoId).stream().map(mapper::toResponseDTO).toList();
    }

    /**
     * Conteudo da prova para o proprio aluno: questoes e alternativas da
     * tentativa, com gabarito apenas quando ela ja foi concluida.
     */
    @GetMapping("/{id}/questoes")
    public studojurata_api.dto.QuestaoDaTentativaDTO questoesDaTentativa(@PathVariable Long id) {
        return service.questoesDaTentativa(id);
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
