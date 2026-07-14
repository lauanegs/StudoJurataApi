package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.model.Frequencia;
import studojurata_api.service.FrequenciaService;

import java.util.List;

@RestController
@RequestMapping("/frequencia")
@RequiredArgsConstructor
public class FrequenciaController {

    private final FrequenciaService service;

    @GetMapping("/aula/{aulaId}")
    public List<Frequencia> listarPorAula(@PathVariable Long aulaId) { return service.listarPorAula(aulaId); }

    @GetMapping("/aluno/{alunoId}")
    public List<Frequencia> listarPorAluno(@PathVariable Long alunoId) { return service.listarPorAluno(alunoId); }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable Long id) { service.deletar(id); }
}
