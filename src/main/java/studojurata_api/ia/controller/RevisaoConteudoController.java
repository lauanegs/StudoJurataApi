package studojurata_api.ia.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.ia.dto.RegistrarReforcoRequest;
import studojurata_api.ia.dto.RevisaoConteudoResponseDTO;
import studojurata_api.ia.mapper.RevisaoConteudoMapper;
import studojurata_api.ia.service.RevisaoConteudoService;

import java.util.List;

@RestController
@RequestMapping("/ia/revisao-conteudo")
@RequiredArgsConstructor
public class RevisaoConteudoController {

    private final RevisaoConteudoService service;
    private final RevisaoConteudoMapper mapper;

    @GetMapping("/aluno/{alunoId}")
    public List<RevisaoConteudoResponseDTO> listarPorAluno(@PathVariable Long alunoId) {
        return service.listarPorAluno(alunoId).stream().map(mapper::toResponseDTO).toList();
    }

    @GetMapping("/devidos")
    public List<RevisaoConteudoResponseDTO> listarDevidosHoje() {
        return service.listarDevidosHoje().stream().map(mapper::toResponseDTO).toList();
    }

    @GetMapping("/devidos/aluno/{alunoId}")
    public List<RevisaoConteudoResponseDTO> listarDevidosHojePorAluno(@PathVariable Long alunoId) {
        return service.listarDevidosHojePorAluno(alunoId).stream().map(mapper::toResponseDTO).toList();
    }

    @PostMapping("/registrar")
    public RevisaoConteudoResponseDTO registrar(@RequestBody RegistrarReforcoRequest request) {
        return mapper.toResponseDTO(
                service.registrarReforco(request.getAlunoId(), request.getConteudoPlanoId(), request.getNivelDominio()));
    }
}
