package studojurata_api.service.gamificacao;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import studojurata_api.model.Aluno;
import studojurata_api.model.gamificacao.PontuacaoAluno;
import studojurata_api.repository.AlunoRepository;
import studojurata_api.repository.gamificacao.PontuacaoAlunoRepository;

/**
 * Correção 8.1/8.2 da Segunda Análise Crítica: concede moedas/XP por
 * simulado concluído e por reforço registrado (RevisaoConteudo) — nunca só
 * por acerto, para não penalizar quem precisa revisar mais. Não expõe
 * nenhum ranking comparativo entre alunos (decisão explícita: sem
 * comparação entre colegas).
 */
@Service
@RequiredArgsConstructor
public class PontuacaoAlunoService {

    /** Mesma quantidade de moedas por simulado concluído, independente da nota — equidade pedida em 8.1. */
    public static final int MOEDAS_POR_SIMULADO_CONCLUIDO = 10;

    /** Mesma quantidade por reforço registrado — quem revisa é beneficiado tanto quanto quem acerta de primeira. */
    public static final int MOEDAS_POR_REFORCO = 10;

    private final PontuacaoAlunoRepository repository;
    private final AlunoRepository alunoRepository;

    public PontuacaoAluno buscarOuCriar(Long alunoId) {
        return repository.findByAluno_Id(alunoId).orElseGet(() -> {
            PontuacaoAluno nova = new PontuacaoAluno();
            Aluno aluno = alunoRepository.findById(alunoId).orElseThrow();
            nova.setAluno(aluno);
            nova.setMoedas(0);
            nova.setXpTotal(0);
            return repository.save(nova);
        });
    }

    @Transactional
    public PontuacaoAluno concederMoedas(Long alunoId, int quantidade, int xp) {
        PontuacaoAluno pontuacao = buscarOuCriar(alunoId);
        pontuacao.setMoedas(pontuacao.getMoedas() + quantidade);
        pontuacao.setXpTotal(pontuacao.getXpTotal() + xp);
        return repository.save(pontuacao);
    }

    @Transactional
    public PontuacaoAluno debitarMoedas(Long alunoId, int quantidade) {
        PontuacaoAluno pontuacao = buscarOuCriar(alunoId);
        pontuacao.setMoedas(pontuacao.getMoedas() - quantidade);
        return repository.save(pontuacao);
    }
}
