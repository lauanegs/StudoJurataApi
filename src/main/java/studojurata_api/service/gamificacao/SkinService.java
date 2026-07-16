package studojurata_api.service.gamificacao;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.exception.RegraNegocioException;
import studojurata_api.model.Aluno;
import studojurata_api.model.gamificacao.PontuacaoAluno;
import studojurata_api.model.gamificacao.Skin;
import studojurata_api.model.gamificacao.SkinAluno;
import studojurata_api.repository.AlunoRepository;
import studojurata_api.repository.gamificacao.SkinAlunoRepository;
import studojurata_api.repository.gamificacao.SkinRepository;

import java.time.LocalDateTime;
import java.util.List;

/** Correção 8.2: moedas compram skins do personagem/mascote. */
@Service
@RequiredArgsConstructor
public class SkinService {

    private final SkinRepository skinRepository;
    private final SkinAlunoRepository skinAlunoRepository;
    private final AlunoRepository alunoRepository;
    private final PontuacaoAlunoService pontuacaoAlunoService;

    public List<Skin> listarDisponiveis() { return skinRepository.findByDisponivelTrue(); }

    public List<SkinAluno> skinsDoAluno(Long alunoId) { return skinAlunoRepository.findByAluno_Id(alunoId); }

    /** Compra a skin com as moedas do aluno; falha se ele já não tem moedas suficientes. */
    @Transactional
    public SkinAluno comprar(Long alunoId, Long skinId) {
        if (skinAlunoRepository.existsByAluno_IdAndSkin_Id(alunoId, skinId)) {
            throw new RegraNegocioException("Aluno já possui esta skin.");
        }

        Skin skin = skinRepository.findById(skinId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Skin não encontrada."));
        if (!Boolean.TRUE.equals(skin.getDisponivel())) {
            throw new RegraNegocioException("Esta skin não está disponível para compra.");
        }

        PontuacaoAluno pontuacao = pontuacaoAlunoService.buscarOuCriar(alunoId);
        if (pontuacao.getMoedas() < skin.getCustoMoedas()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Moedas insuficientes: necessário " + skin.getCustoMoedas() + ", disponível " + pontuacao.getMoedas() + ".");
        }

        pontuacaoAlunoService.debitarMoedas(alunoId, skin.getCustoMoedas());

        Aluno aluno = alunoRepository.findById(alunoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aluno não encontrado."));

        SkinAluno skinAluno = new SkinAluno();
        skinAluno.setAluno(aluno);
        skinAluno.setSkin(skin);
        skinAluno.setDataAquisicao(LocalDateTime.now());
        skinAluno.setAtiva(false);
        return skinAlunoRepository.save(skinAluno);
    }

    /** Equipa uma skin já comprada, desmarcando a anteriormente ativa. */
    @Transactional
    public SkinAluno equipar(Long alunoId, Long skinId) {
        SkinAluno alvo = skinAlunoRepository.findByAluno_IdAndSkin_Id(alunoId, skinId)
                .orElseThrow(() -> new RegraNegocioException("Aluno ainda não possui esta skin."));

        skinAlunoRepository.findByAluno_IdAndAtivaTrue(alunoId).ifPresent(ativa -> {
            ativa.setAtiva(false);
            skinAlunoRepository.save(ativa);
        });

        alvo.setAtiva(true);
        return skinAlunoRepository.save(alvo);
    }
}
