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
 * Orquestra a geração automática de um simulado de reforço para um aluno
 * (item 1.4 da Análise Crítica): "os simulados gerados pela plataforma a
 * partir da IA não precisarão de autorização do professor [para SEREM
 * GERADOS]... talvez precise de uma revisão humana exigida para o professor
 * ... ele aprovaria os simulados questão por questão... para assim o
 * simulado ser liberado para o aluno".
 *
 * Ou seja: a geração em si (este método) roda sem intervenção humana — pode
 * ser acionada por uma recomendação (RecomendacaoService: repetição espaçada
 * devida ou baixo aproveitamento) — mas o Simulado resultante nasce em
 * RASCUNHO com questões potencialmente PENDENTE (origem IA). Ele só chega ao
 * aluno depois que o professor revisa e aprova as questões pendentes
 * (QuestaoController: /questoes/pendentes, /aprovar, /rejeitar — já
 * existentes no módulo de simulados) e então chama
 * SimuladoService.lancar — que já recusa lançar um simulado com questões
 * não aprovadas. Nenhuma alteração foi necessária nesses arquivos: a trava
 * de revisão humana já existia e é reaproveitada aqui integralmente.
 *
 * `motivos` é opcional e só serve pra registrar, em SimuladoGeradoIA, por que
 * a geração foi acionada (motivos da RecomendacaoDTO de origem, quando
 * houver) — a tela de aprovação do professor usa isso pra mostrar aluno e
 * motivo ao lado de cada simulado pendente, sem precisar adivinhar a partir
 * do título.
 */
@Service
@RequiredArgsConstructor
public class GeracaoSimuladoIAService {

    /**
     * Confirmado pelo usuário: todo simulado gerado por IA é padronizado
     * nessa quantidade — não existe (nem existiu no front) um jeito de pedir
     * um valor diferente, então isso deixou de ser parâmetro do método.
     */
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

    @Transactional
    public Simulado gerarParaAluno(
            Long alunoId, Long conteudoPlanoId, NivelDificuldade nivel, Set<MotivoRecomendacao> motivos) {
        Aluno aluno = alunoRepository.findById(alunoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aluno não encontrado."));
        ConteudoPlano conteudo = conteudoPlanoRepository.findById(conteudoPlanoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Conteúdo não encontrado."));

        // Sem nível forçado explicitamente: usa o nível mais baixo em que o
        // aluno está fraco NESTE conteúdo (RecomendacaoService — ex.: erra
        // fácil mas acerta difícil → reforça fácil; acerta fácil mas erra
        // difícil → reforça difícil). Sem dado suficiente pra decidir (aluno
        // novo, questões sem nível registrado), cai no nível padrão.
        NivelDificuldade nivelSugerido = nivel == null ? recomendacaoService.sugerirNivelReforco(alunoId, conteudoPlanoId) : null;
        NivelDificuldade nivelEfetivo = nivel != null ? nivel : nivelSugerido != null ? nivelSugerido : NIVEL_PADRAO;

        Simulado simulado = new Simulado();
        simulado.setTitulo("Reforço automático — " + conteudo.getTitulo() + " — " + descricaoAluno(aluno));
        simulado.setTipoDestinacao(TipoDestinacaoSimulado.ESPECIFICO);
        simulado.setQuantidadeQuestoes(QUANTIDADE_QUESTOES);
        // Nasce em RASCUNHO: só é lançado ao aluno após revisão humana das questões (ver item 1.4).
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
     * Idade do aluno em anos, pra GeracaoQuestaoIAService adequar a linguagem
     * do enunciado no prompt do Gemini (confirmado pelo usuário — questões
     * genéricas de "ensino médio" hardcoded não fazem sentido pro público
     * infantil/teen dos cursos daqui). Null quando o aluno não tem data de
     * nascimento cadastrada — GeracaoQuestaoIAService cai num texto genérico
     * nesse caso, sem travar a geração por causa de um cadastro incompleto.
     */
    private Integer calcularIdade(Aluno aluno) {
        LocalDate nascimento = aluno.getPessoa() != null ? aluno.getPessoa().getDataNascimento() : null;
        return nascimento != null ? Period.between(nascimento, LocalDate.now()).getYears() : null;
    }

    /**
     * Prazo pra revisar/lançar o simulado gerado: a data em que a repetição
     * espaçada deste aluno+conteúdo ficou devida, quando existe uma
     * (RevisaoConteudo.dataProximoReforco — ainda não avançada por esta
     * geração, que não chama RevisaoConteudoService.registrarReforco);
     * senão, hoje (baixo aproveitamento não tem agenda própria).
     */
    private LocalDate calcularPrazoLancamento(Long alunoId, Long conteudoPlanoId) {
        return revisaoConteudoRepository.findByAlunoIdAndConteudoPlanoId(alunoId, conteudoPlanoId)
                .map(RevisaoConteudo::getDataProximoReforco)
                .filter(data -> data != null && !data.isAfter(LocalDate.now()))
                .orElse(LocalDate.now());
    }
}
