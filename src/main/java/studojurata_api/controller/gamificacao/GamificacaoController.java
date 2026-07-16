package studojurata_api.controller.gamificacao;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.model.gamificacao.PontuacaoAluno;
import studojurata_api.model.gamificacao.Skin;
import studojurata_api.model.gamificacao.SkinAluno;
import studojurata_api.service.gamificacao.PontuacaoAlunoService;
import studojurata_api.service.gamificacao.SkinService;

import java.util.List;

/**
 * Correção 8.1/8.2. Sempre por aluno específico — nunca uma listagem
 * comparativa entre alunos (sem ranking, por decisão explícita).
 */
@RestController
@RequestMapping("/gamificacao")
@RequiredArgsConstructor
public class GamificacaoController {

    private final PontuacaoAlunoService pontuacaoAlunoService;
    private final SkinService skinService;

    @GetMapping("/aluno/{alunoId}/pontuacao")
    public PontuacaoAluno pontuacao(@PathVariable Long alunoId) {
        return pontuacaoAlunoService.buscarOuCriar(alunoId);
    }

    @GetMapping("/skins")
    public List<Skin> skinsDisponiveis() {
        return skinService.listarDisponiveis();
    }

    @GetMapping("/aluno/{alunoId}/skins")
    public List<SkinAluno> skinsDoAluno(@PathVariable Long alunoId) {
        return skinService.skinsDoAluno(alunoId);
    }

    @PostMapping("/aluno/{alunoId}/skins/{skinId}/comprar")
    public SkinAluno comprar(@PathVariable Long alunoId, @PathVariable Long skinId) {
        return skinService.comprar(alunoId, skinId);
    }

    @PostMapping("/aluno/{alunoId}/skins/{skinId}/equipar")
    public SkinAluno equipar(@PathVariable Long alunoId, @PathVariable Long skinId) {
        return skinService.equipar(alunoId, skinId);
    }
}
