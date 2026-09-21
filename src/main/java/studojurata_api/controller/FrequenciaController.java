package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.model.Frequencia;
import studojurata_api.security.AlunoAccessGuard;
import studojurata_api.service.FrequenciaService;

import java.util.List;

@RestController
@RequestMapping("/frequencia")
@RequiredArgsConstructor
public class FrequenciaController {

    private final FrequenciaService service;
    private final AlunoAccessGuard alunoAccessGuard;

    @GetMapping("/aluno/{alunoId}")
    public List<Frequencia> listarPorAluno(@PathVariable Long alunoId) {
        alunoAccessGuard.garantir(alunoId);
        return service.listarPorAluno(alunoId);
    }
}
