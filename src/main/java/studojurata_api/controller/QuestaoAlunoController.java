package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.dto.QuestaoAlunoResponseDTO;
import studojurata_api.mapper.QuestaoAlunoMapper;
import studojurata_api.model.SimuladoAluno;
import studojurata_api.security.AlunoAccessGuard;
import studojurata_api.service.QuestaoAlunoService;
import studojurata_api.service.SimuladoAlunoService;

import java.util.List;

@RestController
@RequestMapping("/questao-aluno")
@RequiredArgsConstructor
public class QuestaoAlunoController {

    private final QuestaoAlunoService service;
    private final QuestaoAlunoMapper mapper;
    private final SimuladoAlunoService simuladoAlunoService;
    private final AlunoAccessGuard alunoAccessGuard;

    @GetMapping
    public List<QuestaoAlunoResponseDTO> listar() {
        // Listagem agregada de respostas: recurso de gestão pedagógica.
        alunoAccessGuard.garantirAcessoDeGestao();
        return service.listar().stream().map(mapper::toResponseDTO).toList();
    }

    @GetMapping("/simulado-aluno/{simuladoAlunoId}")
    public List<QuestaoAlunoResponseDTO> listarPorSimuladoAluno(@PathVariable Long simuladoAlunoId) {
        SimuladoAluno tentativa = simuladoAlunoService.buscar(simuladoAlunoId);
        alunoAccessGuard.garantir(tentativa.getAluno() != null ? tentativa.getAluno().getId() : null);
        return service.listarPorSimuladoAluno(simuladoAlunoId).stream().map(mapper::toResponseDTO).toList();
    }
}
