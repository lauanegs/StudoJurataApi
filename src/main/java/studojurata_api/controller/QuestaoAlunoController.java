package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.dto.QuestaoAlunoResponseDTO;
import studojurata_api.mapper.QuestaoAlunoMapper;
import studojurata_api.service.QuestaoAlunoService;

import java.util.List;

@RestController
@RequestMapping("/questao-aluno")
@RequiredArgsConstructor
public class QuestaoAlunoController {

    private final QuestaoAlunoService service;
    private final QuestaoAlunoMapper mapper;

    @GetMapping
    public List<QuestaoAlunoResponseDTO> listar() {
        return service.listar().stream().map(mapper::toResponseDTO).toList();
    }

    @GetMapping("/simulado-aluno/{simuladoAlunoId}")
    public List<QuestaoAlunoResponseDTO> listarPorSimuladoAluno(@PathVariable Long simuladoAlunoId) {
        return service.listarPorSimuladoAluno(simuladoAlunoId).stream().map(mapper::toResponseDTO).toList();
    }
}
