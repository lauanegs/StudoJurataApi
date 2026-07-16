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
 * Correção 8.1/8.2. Sempre por aluno específico — nunca uma listagem
 * comparativa entre alunos (sem ranking, por decisão explícita).
 *
 * Correção 2.1 da Terceira Análise Crítica (IDOR): todo endpoint aqui recebe
 * um alunoId livre na URL, então todos passam por AlunoAccessGuard — sem
 * essa checagem, um aluno podia consultar a pontuação de outro ou comprar
 * skins gastando as moedas de outro aluno só trocando o id na URL.
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
