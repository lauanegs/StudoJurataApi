package studojurata_api.controller.gamificacao;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.model.gamificacao.PontuacaoAluno;
import studojurata_api.model.gamificacao.Skin;
import studojurata_api.model.gamificacao.SkinAluno;
import studojurata_api.security.AlunoAccessGuard;
import studojurata_api.service.gamificacao.PontuacaoAlunoService;
import studojurata_api.service.gamificacao.SkinService;

import java.util.List;

/**
 * Sempre por aluno específico, sem listagem comparativa (não há ranking).
 * Todo endpoint recebe o alunoId na URL, então todos passam por
 * AlunoAccessGuard: sem isso, um aluno poderia gastar as moedas de outro.
 */
@RestController
@RequestMapping("/gamificacao")
@RequiredArgsConstructor
public class GamificacaoController {

    private final PontuacaoAlunoService pontuacaoAlunoService;
    private final SkinService skinService;
    private final AlunoAccessGuard alunoAccessGuard;

    @GetMapping("/aluno/{alunoId}/pontuacao")
    public PontuacaoAluno pontuacao(@PathVariable Long alunoId) {
        alunoAccessGuard.garantir(alunoId);
        return pontuacaoAlunoService.buscarOuCriar(alunoId);
    }

    @GetMapping("/skins")
    public List<Skin> skinsDisponiveis() {
        return skinService.listarDisponiveis();
    }

    @GetMapping("/aluno/{alunoId}/skins")
    public List<SkinAluno> skinsDoAluno(@PathVariable Long alunoId) {
        alunoAccessGuard.garantir(alunoId);
        return skinService.skinsDoAluno(alunoId);
    }

    @PostMapping("/aluno/{alunoId}/skins/{skinId}/comprar")
    public SkinAluno comprar(@PathVariable Long alunoId, @PathVariable Long skinId) {
        alunoAccessGuard.garantir(alunoId);
        return skinService.comprar(alunoId, skinId);
    }

    @PostMapping("/aluno/{alunoId}/skins/{skinId}/equipar")
    public SkinAluno equipar(@PathVariable Long alunoId, @PathVariable Long skinId) {
        alunoAccessGuard.garantir(alunoId);
        return skinService.equipar(alunoId, skinId);
    }
}
