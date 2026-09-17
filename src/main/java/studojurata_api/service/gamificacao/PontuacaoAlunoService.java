package studojurata_api.service.gamificacao;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.model.Aluno;
import studojurata_api.model.gamificacao.PontuacaoAluno;
import studojurata_api.model.gamificacao.Skin;
import studojurata_api.model.gamificacao.SkinAluno;
import studojurata_api.repository.AlunoRepository;
import studojurata_api.repository.gamificacao.PontuacaoAlunoRepository;
import studojurata_api.repository.gamificacao.SkinAlunoRepository;
import studojurata_api.repository.gamificacao.SkinRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PontuacaoAlunoService {

    /** Independente da nota: não é bonificação por acerto. */
    public static final int MOEDAS_POR_SIMULADO_CONCLUIDO = 10;

    /** Mesma quantidade por reforço registrado — quem revisa é beneficiado tanto quanto quem acerta de primeira. */
    public static final int MOEDAS_POR_REFORCO = 10;

    private final PontuacaoAlunoRepository repository;
    private final AlunoRepository alunoRepository;
    private final SkinRepository skinRepository;
    private final SkinAlunoRepository skinAlunoRepository;

    public PontuacaoAluno buscarOuCriar(Long alunoId) {
        return repository.findByAluno_Id(alunoId).orElseGet(() -> {
            PontuacaoAluno nova = new PontuacaoAluno();
            Aluno aluno = alunoRepository.findById(alunoId)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Aluno " + alunoId + " não encontrado."));
            nova.setAluno(aluno);
            nova.setMoedas(0);
            PontuacaoAluno salva = repository.save(nova);
            concederSkinPadrao(aluno);
            return salva;
        });
    }

    /**
     * Na criação da pontuação, concede e equipa a skin gratuita, para o aluno
     * não precisar "comprar" uma skin de custo zero.
     */
    private void concederSkinPadrao(Aluno aluno) {
        skinRepository.findByDisponivelTrue().stream()
                .filter(skin -> skin.getCustoMoedas() != null && skin.getCustoMoedas() == 0)
                .findFirst()
                .ifPresent(skinGratuita -> {
                    if (skinAlunoRepository.existsByAluno_IdAndSkin_Id(aluno.getId(), skinGratuita.getId())) {
                        return;
                    }
                    SkinAluno posse = new SkinAluno();
                    posse.setAluno(aluno);
                    posse.setSkin(skinGratuita);
                    posse.setDataAquisicao(LocalDateTime.now());
                    posse.setAtiva(true);
                    skinAlunoRepository.save(posse);
                });
    }

    @Transactional
    public PontuacaoAluno concederMoedas(Long alunoId, int quantidade) {
        PontuacaoAluno pontuacao = buscarOuCriar(alunoId);
        pontuacao.setMoedas(pontuacao.getMoedas() + quantidade);
        return repository.save(pontuacao);
    }

    @Transactional
    public PontuacaoAluno debitarMoedas(Long alunoId, int quantidade) {
        PontuacaoAluno pontuacao = buscarOuCriar(alunoId);
        pontuacao.setMoedas(pontuacao.getMoedas() - quantidade);
        return repository.save(pontuacao);
    }
}
