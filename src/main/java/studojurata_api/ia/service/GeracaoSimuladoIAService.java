package studojurata_api.ia.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.ia.model.RevisaoConteudo;
import studojurata_api.ia.model.SimuladoGeradoIA;
import studojurata_api.ia.model.enums.MotivoRecomendacao;
import studojurata_api.ia.repository.RevisaoConteudoRepository;
import studojurata_api.ia.repository.SimuladoGeradoIARepository;
import studojurata_api.model.Aluno;
import studojurata_api.model.ConteudoPlano;
import studojurata_api.model.PlanoEnsino;
import studojurata_api.model.Questao;
import studojurata_api.model.Simulado;
import studojurata_api.model.SimuladoQuestao;
import studojurata_api.model.enums.NivelDificuldade;
import studojurata_api.model.enums.StatusSimulado;
import studojurata_api.model.enums.StatusSimuladoQuestao;
import studojurata_api.model.enums.TipoDestinacaoSimulado;
import studojurata_api.model.enums.TipoQuestao;
import studojurata_api.repository.AlunoRepository;
import studojurata_api.repository.ConteudoPlanoRepository;
import studojurata_api.repository.SimuladoQuestaoRepository;
import studojurata_api.repository.SimuladoRepository;

import java.time.LocalDate;
import java.time.Period;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A geração roda sem intervenção humana, mas o simulado nasce em RASCUNHO com
 * questões possivelmente PENDENTE: só chega ao aluno depois que o professor
 * aprova as questões, porque SimuladoService.lancar recusa questões não
 * aprovadas.
 *
 * `motivos` só é registrado em SimuladoGeradoIA, para a tela de aprovação
 * mostrar por que cada simulado foi gerado.
 */
@Service
@RequiredArgsConstructor
public class GeracaoSimuladoIAService {

    public static final int QUANTIDADE_QUESTOES = 5;

    private static final NivelDificuldade NIVEL_PADRAO = NivelDificuldade.MEDIA;

    private final SimuladoRepository simuladoRepository;
    private final SimuladoQuestaoRepository simuladoQuestaoRepository;
    private final ConteudoPlanoRepository conteudoPlanoRepository;
    private final AlunoRepository alunoRepository;
    private final GeracaoQuestaoIAService geracaoQuestaoIAService;
    private final SimuladoGeradoIARepository simuladoGeradoIARepository;
    private final RevisaoConteudoRepository revisaoConteudoRepository;
    private final RecomendacaoService recomendacaoService;

    public List<SimuladoGeradoIA> listarGerados() {
        return simuladoGeradoIARepository.findAll();
    }

    @Transactional
    public Simulado gerarParaAluno(
            Long alunoId, Long conteudoPlanoId, NivelDificuldade nivel, Set<MotivoRecomendacao> motivos) {
        Aluno aluno = alunoRepository.findById(alunoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aluno não encontrado."));
        ConteudoPlano conteudo = conteudoPlanoRepository.findById(conteudoPlanoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Conteúdo não encontrado."));

        // Sem nível informado, reforça o nível mais baixo em que o aluno está
        // fraco neste conteúdo; sem dados suficientes, usa o nível padrão.
        NivelDificuldade nivelSugerido = nivel == null ? recomendacaoService.sugerirNivelReforco(alunoId, conteudoPlanoId) : null;
        NivelDificuldade nivelEfetivo = nivel != null ? nivel : nivelSugerido != null ? nivelSugerido : NIVEL_PADRAO;

        Simulado simulado = new Simulado();
        simulado.setTitulo("Reforço automático — " + conteudo.getTitulo() + " — " + descricaoAluno(aluno));
        simulado.setTipoDestinacao(TipoDestinacaoSimulado.ESPECIFICO);
        simulado.setQuantidadeQuestoes(QUANTIDADE_QUESTOES);
        simulado.setStatus(StatusSimulado.RASCUNHO);

        PlanoEnsino planoEnsino = conteudo.getPlanoEnsino();
        simulado.setPlanoEnsino(planoEnsino);
        if (planoEnsino != null && planoEnsino.getTurmaDisciplina() != null) {
            simulado.setDisciplina(planoEnsino.getTurmaDisciplina().getDisciplina());
            simulado.setTurma(planoEnsino.getTurmaDisciplina().getTurma());
        }

        simulado = simuladoRepository.save(simulado);

        SimuladoGeradoIA vinculo = new SimuladoGeradoIA();
        vinculo.setSimulado(simulado);
        vinculo.setAluno(aluno);
        vinculo.setConteudoPlano(conteudo);
        vinculo.setMotivos(motivos != null ? motivos : new LinkedHashSet<>());
        vinculo.setPrazoLancamento(calcularPrazoLancamento(alunoId, conteudoPlanoId));
        simuladoGeradoIARepository.save(vinculo);

        List<Questao> questoes = geracaoQuestaoIAService.gerar(
                conteudoPlanoId, nivelEfetivo, TipoQuestao.ALTERNATIVAS, QUANTIDADE_QUESTOES, simulado, calcularIdade(aluno));

        int ordem = 1;
        for (Questao questao : questoes) {
            SimuladoQuestao simuladoQuestao = new SimuladoQuestao();
            simuladoQuestao.setSimulado(simulado);
            simuladoQuestao.setQuestao(questao);
            simuladoQuestao.setOrdem(ordem++);
            simuladoQuestao.setPontuacao(1d);
            simuladoQuestao.setStatus(StatusSimuladoQuestao.ATIVA);
            simuladoQuestaoRepository.save(simuladoQuestao);
        }

        return simulado;
    }

    private String descricaoAluno(Aluno aluno) {
        return aluno.getMatricula() != null ? "matrícula " + aluno.getMatricula() : "aluno #" + aluno.getId();
    }

    /**
     * Usada para adequar a linguagem do enunciado. Null sem data de nascimento:
     * o prompt fica genérico em vez de travar a geração.
     */
    private Integer calcularIdade(Aluno aluno) {
        LocalDate nascimento = aluno.getPessoa() != null ? aluno.getPessoa().getDataNascimento() : null;
        return nascimento != null ? Period.between(nascimento, LocalDate.now()).getYears() : null;
    }

    /**
     * A data em que a repetição espaçada venceu, quando existe (esta geração
     * não a avança); senão, hoje.
     */
    private LocalDate calcularPrazoLancamento(Long alunoId, Long conteudoPlanoId) {
        return revisaoConteudoRepository.findByAlunoIdAndConteudoPlanoId(alunoId, conteudoPlanoId)
                .map(RevisaoConteudo::getDataProximoReforco)
                .filter(data -> data != null && !data.isAfter(LocalDate.now()))
                .orElse(LocalDate.now());
    }
}
