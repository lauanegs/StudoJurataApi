package studojurata_api.ia.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.exception.RegraNegocioException;
import studojurata_api.ia.model.RevisaoConteudo;
import studojurata_api.ia.model.SimuladoGeradoIA;
import studojurata_api.ia.model.enums.MotivoRecomendacao;
import studojurata_api.ia.repository.RevisaoConteudoRepository;
import studojurata_api.ia.repository.SimuladoGeradoIARepository;
import studojurata_api.machinelearning.dto.RecomendacaoSimuladoDTO;
import studojurata_api.machinelearning.model.enums.DecisaoGeracao;
import studojurata_api.machinelearning.service.MachineLearningRecomendacaoService;
import studojurata_api.model.Aluno;
import studojurata_api.model.Aula;
import studojurata_api.model.AulaConteudo;
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
import studojurata_api.model.enums.TipoUsuario;
import studojurata_api.repository.AlunoRepository;
import studojurata_api.repository.AulaConteudoRepository;
import studojurata_api.repository.ConteudoPlanoRepository;
import studojurata_api.repository.SimuladoQuestaoRepository;
import studojurata_api.repository.SimuladoRepository;
import studojurata_api.security.UsuarioAutenticado;
import studojurata_api.security.SimuladoAccessGuard;
import studojurata_api.service.SimuladoService;

import java.time.LocalDate;
import java.time.Period;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
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
    private final MachineLearningRecomendacaoService machineLearningRecomendacaoService;
    private final SimuladoService simuladoService;
    private final UsuarioAutenticado usuarioAutenticado;
    private final SimuladoAccessGuard simuladoAccessGuard;
    private final AulaConteudoRepository aulaConteudoRepository;

    /**
     * Vínculos de simulado gerado por IA visíveis ao usuário logado, com o
     * <b>mesmo escopo da listagem de simulados</b> ({@link SimuladoService}):
     * ADMIN recebe tudo; PROFESSOR só os vínculos cujos simulados estão nas
     * turmas dele (vínculo de simulado órfão fica de fora); professor sem turmas
     * recebe lista vazia. O DTO devolvido ao cliente não muda — muda só o
     * conjunto de registros.
     */
    public List<SimuladoGeradoIA> listarGerados() {
        if (usuarioAutenticado.ehAdministrador()) {
            return simuladoGeradoIARepository.findAll();
        }

        var simuladoIds = simuladoService.simuladoIdsVisiveis();
        return simuladoIds.isEmpty() ? List.of() : simuladoGeradoIARepository.findBySimulado_IdIn(simuladoIds);
    }

    @Transactional
    public Simulado gerarParaAluno(
            Long alunoId, Long conteudoPlanoId, NivelDificuldade nivel, Set<MotivoRecomendacao> motivos) {
        Aluno aluno = alunoRepository.findById(alunoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aluno não encontrado."));
        ConteudoPlano conteudo = conteudoPlanoRepository.findById(conteudoPlanoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Conteúdo não encontrado."));

        // O reforço precisa nascer dentro de uma oferta: turma, disciplina e plano
        // específico. Sem isso o simulado ficaria órfão — fora do escopo de qualquer
        // professor (visível só ao administrador) e sem poder ser lançado.
        PlanoEnsino planoEnsino = exigirContextoDaTurma(conteudo);
        exigirConteudoJaMinistrado(conteudo);

        // Camada de machine learning: decide necessidade de revisão, quantidade,
        // dificuldade, questões candidatas e se precisa gerar por IA. Ela reaproveita
        // a regra pedagógica existente (motivo e nível prioritário) e cai para o
        // determinístico quando não há histórico ou modelo treinável.
        RecomendacaoSimuladoDTO recomendacao =
                machineLearningRecomendacaoService.recomendar(alunoId, conteudoPlanoId, nivel, motivos);

        if (recomendacao.isAlertaColetivo()) {
            // Maioria da turma abaixo do limiar: o caso é de revisão em sala, não de
            // simulado individual por IA.
            throw new RegraNegocioException(recomendacao.getAlertaProfessor());
        }

        NivelDificuldade nivelEfetivo = recomendacao.getNivelPrioritario() != null
                ? recomendacao.getNivelPrioritario()
                : NIVEL_PADRAO;
        int quantidadeQuestoes = recomendacao.getQuantidadeRecomendada() > 0
                ? recomendacao.getQuantidadeRecomendada()
                : QUANTIDADE_QUESTOES;

        Simulado simulado = new Simulado();
        simulado.setTitulo("Reforço automático — " + conteudo.getTitulo() + " — " + descricaoAluno(aluno));
        simulado.setTipoDestinacao(TipoDestinacaoSimulado.ESPECIFICO);
        simulado.setQuantidadeQuestoes(quantidadeQuestoes);
        simulado.setStatus(StatusSimulado.RASCUNHO);

        simulado.setPlanoEnsino(planoEnsino);
        simulado.setDisciplina(planoEnsino.getTurmaDisciplina().getDisciplina());
        simulado.setTurma(planoEnsino.getTurmaDisciplina().getTurma());

        garantirEscopoDoProfessor(simulado);

        simulado = simuladoRepository.save(simulado);

        // Fecha o rastro do aprendizado: qual recomendação gerou este simulado.
        machineLearningRecomendacaoService.vincularSimulado(recomendacao.getId(), simulado);

        SimuladoGeradoIA vinculo = new SimuladoGeradoIA();
        vinculo.setSimulado(simulado);
        vinculo.setAluno(aluno);
        vinculo.setConteudoPlano(conteudo);
        vinculo.setMotivos(motivos != null ? motivos : new LinkedHashSet<>());
        vinculo.setPrazoLancamento(calcularPrazoLancamento(alunoId, conteudoPlanoId));
        simuladoGeradoIARepository.save(vinculo);

        List<Questao> questoes = comporQuestoes(
                recomendacao, conteudoPlanoId, nivelEfetivo, quantidadeQuestoes, simulado, aluno);

        if (questoes.isEmpty()) {
            // Sem questão adequada e sem autorização para gerar por IA: um rascunho
            // vazio seria pior que recusar (a transação desfaz o que já foi gravado).
            throw new RegraNegocioException(
                    "Não há questões adequadas no banco para este reforço e a geração por IA exige gatilho de "
                            + "repetição espaçada vencida ou baixo aproveitamento.");
        }

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

        // A quantidade planejada passa a ser a quantidade real: a geração por IA pode
        // devolver menos que o pedido, e o simulado não pode anunciar questão que não tem.
        simulado.setQuantidadeQuestoes(questoes.size());
        return simuladoRepository.save(simulado);
    }

    /**
     * Decide de onde saem as questões, seguindo a decisão explícita do ML:
     * <ul>
     *   <li>{@code REUTILIZAR_BANCO} — usa as candidatas já selecionadas do banco
     *       (conteúdos prioritários, dificuldade e limite de 10 já aplicados na
     *       seleção) e <b>não</b> chama a IA;</li>
     *   <li>{@code GERAR_POR_IA} — segue com a geração existente (cache do
     *       conteúdo → IA → fallback de banco), que é o único caminho que aciona
     *       a API de IA, uma vez por simulado.</li>
     * </ul>
     */
    private List<Questao> comporQuestoes(RecomendacaoSimuladoDTO recomendacao, Long conteudoPlanoId,
            NivelDificuldade nivelEfetivo, int quantidadeQuestoes, Simulado simulado, Aluno aluno) {

        if (recomendacao.getDecisaoGeracao() != DecisaoGeracao.GERAR_POR_IA) {
            return recomendacao.getQuestoesCandidatas();
        }

        return geracaoQuestaoIAService.gerar(
                conteudoPlanoId, nivelEfetivo, TipoQuestao.ALTERNATIVAS, quantidadeQuestoes, simulado,
                calcularIdade(aluno));
    }

    private String descricaoAluno(Aluno aluno) {
        return aluno.getMatricula() != null ? "matrícula " + aluno.getMatricula() : "aluno #" + aluno.getId();
    }

    /**
     * O reforço gerado precisa nascer dentro de uma oferta: plano de ensino com
     * turma e disciplina. Conteúdo de plano genérico (D-C2) não gera simulado por
     * IA — não há turma para atribuir e o simulado ficaria órfão.
     */
    private PlanoEnsino exigirContextoDaTurma(ConteudoPlano conteudo) {
        PlanoEnsino planoEnsino = conteudo.getPlanoEnsino();
        var vinculo = planoEnsino != null ? planoEnsino.getTurmaDisciplina() : null;

        if (vinculo == null || vinculo.getTurma() == null || vinculo.getDisciplina() == null) {
            throw new RegraNegocioException(
                    "Este conteúdo não está em um plano de ensino com turma e disciplina: "
                            + "o reforço por IA precisa ser gerado dentro de uma turma.");
        }
        return planoEnsino;
    }

    /**
     * Conteúdo comprovadamente futuro — tem aulas vinculadas e nenhuma delas foi
     * realizada — não gera reforço por IA: o simulado cobraria assunto que a turma
     * ainda não viu.
     *
     * <p>Conteúdo sem nenhuma aula vinculada é inconclusivo (a escola pode não
     * registrar aulas) e continua liberado. Uma única aula com data de publicação
     * não futura libera o conteúdo para sempre, inclusive nas revisões espaçadas.
     */
    private void exigirConteudoJaMinistrado(ConteudoPlano conteudo) {
        List<AulaConteudo> aulasDoConteudo = aulaConteudoRepository.findByConteudoPlano_Id(conteudo.getId());
        if (aulasDoConteudo.isEmpty()) {
            return;
        }

        LocalDate hoje = LocalDate.now();
        boolean algumaMinistrada = aulasDoConteudo.stream()
                .map(AulaConteudo::getAula)
                .filter(Objects::nonNull)
                .map(Aula::getDataPublicacao)
                .filter(Objects::nonNull)
                .anyMatch(dataPublicacao -> !dataPublicacao.isAfter(hoje));

        if (!algumaMinistrada) {
            throw new RegraNegocioException(
                    "Este conteúdo está planejado para o futuro e ainda não foi ministrado. "
                            + "O reforço por IA será disponibilizado após o registro de uma aula realizada.");
        }
    }

    /**
     * Geração pedida por professor precisa cair no escopo dele. Reusa o guard de
     * escrita do simulado (turma, disciplina e plano do professor). ADMIN e
     * fluxos sem sessão — os jobs agendados, que rodam sem autenticação — não
     * passam por esta checagem.
     */
    private void garantirEscopoDoProfessor(Simulado simulado) {
        usuarioAutenticado.opcional()
                .filter(usuario -> usuario.getTipoUsuario() == TipoUsuario.PROFESSOR)
                .ifPresent(usuario -> simuladoAccessGuard.garantirEscrita(simulado));
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
