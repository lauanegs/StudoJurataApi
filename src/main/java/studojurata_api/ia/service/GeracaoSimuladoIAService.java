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
import studojurata_api.model.pessoa.Aluno;
import studojurata_api.model.planejamento.Aula;
import studojurata_api.model.planejamento.AulaConteudo;
import studojurata_api.model.planejamento.ConteudoPlano;
import studojurata_api.model.planejamento.PlanoEnsino;
import studojurata_api.model.simulado.Questao;
import studojurata_api.model.simulado.Simulado;
import studojurata_api.model.simulado.SimuladoQuestao;
import studojurata_api.model.enums.NivelDificuldade;
import studojurata_api.model.enums.StatusSimulado;
import studojurata_api.model.enums.StatusSimuladoQuestao;
import studojurata_api.model.enums.TipoDestinacaoSimulado;
import studojurata_api.model.enums.TipoQuestao;
import studojurata_api.model.enums.TipoUsuario;
import studojurata_api.repository.pessoa.AlunoRepository;
import studojurata_api.repository.planejamento.AulaConteudoRepository;
import studojurata_api.repository.planejamento.ConteudoPlanoRepository;
import studojurata_api.repository.simulado.SimuladoQuestaoRepository;
import studojurata_api.repository.simulado.SimuladoRepository;
import studojurata_api.security.UsuarioAutenticado;
import studojurata_api.security.SimuladoAccessGuard;
import studojurata_api.service.simulado.SimuladoService;

import java.time.LocalDate;
import java.time.Period;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** A geração roda sem intervenção humana, mas o simulado nasce em RASCUNHO com questões possivelmente PENDENTE */
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

    /** Vínculos de simulado gerado por IA visíveis ao usuário logado, com o mesmo escopo da listagem de simulados */
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

        // O reforço precisa nascer dentro de uma oferta: turma, disciplina e plano específico
        PlanoEnsino planoEnsino = exigirContextoDaTurma(conteudo);
        exigirConteudoJaMinistrado(conteudo);

        // Camada de machine learning: decide necessidade de revisão, quantidade, dificuldade, questões candidatas
        RecomendacaoSimuladoDTO recomendacao =
                machineLearningRecomendacaoService.recomendar(alunoId, conteudoPlanoId, nivel, motivos);

        if (recomendacao.isAlertaColetivo()) {
            // Maioria da turma abaixo do limiar
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

        // Fecha o rastro do aprendizado: qual recomendação gerou este simulado
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
            // Sem questão adequada e sem autorização para gerar por IA
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

        // A quantidade planejada passa a ser a quantidade real
        simulado.setQuantidadeQuestoes(questoes.size());
        return simuladoRepository.save(simulado);
    }

    /** Decide de onde saem as questões, seguindo a decisão explícita do ML */
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

    /** O reforço gerado precisa nascer dentro de uma oferta */
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

    /** Conteúdo comprovadamente futuro */
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

    /** Geração pedida por professor precisa cair no escopo dele */
    private void garantirEscopoDoProfessor(Simulado simulado) {
        usuarioAutenticado.opcional()
                .filter(usuario -> usuario.getTipoUsuario() == TipoUsuario.PROFESSOR)
                .ifPresent(usuario -> simuladoAccessGuard.garantirEscrita(simulado));
    }

    /** Usada para adequar a linguagem do enunciado */
    private Integer calcularIdade(Aluno aluno) {
        LocalDate nascimento = aluno.getPessoa() != null ? aluno.getPessoa().getDataNascimento() : null;
        return nascimento != null ? Period.between(nascimento, LocalDate.now()).getYears() : null;
    }

    /** A data em que a repetição espaçada venceu, quando existe (esta geração não a avança); senão, hoje */
    private LocalDate calcularPrazoLancamento(Long alunoId, Long conteudoPlanoId) {
        return revisaoConteudoRepository.findByAlunoIdAndConteudoPlanoId(alunoId, conteudoPlanoId)
                .map(RevisaoConteudo::getDataProximoReforco)
                .filter(data -> data != null && !data.isAfter(LocalDate.now()))
                .orElse(LocalDate.now());
    }
}
