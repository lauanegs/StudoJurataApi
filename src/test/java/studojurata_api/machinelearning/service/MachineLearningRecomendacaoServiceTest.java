package studojurata_api.machinelearning.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import studojurata_api.ia.model.enums.MotivoRecomendacao;
import studojurata_api.ia.service.RecomendacaoService;
import studojurata_api.machinelearning.dto.AtributosRecomendacao;
import studojurata_api.machinelearning.dto.RecomendacaoSimuladoDTO;
import studojurata_api.machinelearning.model.RecomendacaoSimulado;
import studojurata_api.machinelearning.model.enums.DecisaoGeracao;
import studojurata_api.machinelearning.model.enums.NecessidadeRevisao;
import studojurata_api.machinelearning.model.enums.OrigemDecisao;
import studojurata_api.machinelearning.repository.RecomendacaoSimuladoRepository;
import studojurata_api.model.Aluno;
import studojurata_api.model.Alternativa;
import studojurata_api.model.ConteudoPlano;
import studojurata_api.model.Disciplina;
import studojurata_api.model.PlanoEnsino;
import studojurata_api.model.Questao;
import studojurata_api.model.QuestaoConteudo;
import studojurata_api.model.Simulado;
import studojurata_api.model.TurmaDisciplina;
import studojurata_api.model.enums.NivelDificuldade;
import studojurata_api.model.enums.StatusQuestao;
import studojurata_api.model.enums.TipoQuestao;
import studojurata_api.repository.AlunoRepository;
import studojurata_api.repository.AlternativaRepository;
import studojurata_api.repository.ConteudoPlanoRepository;
import studojurata_api.repository.QuestaoAlunoRepository;
import studojurata_api.repository.QuestaoConteudoRepository;
import studojurata_api.repository.QuestaoRepository;
import studojurata_api.service.AuditLogService;

/**
 * Recomendação de simulado individual: atributos reais → Weka ou regra
 * determinística → seleção de questões → decisão de IA → registro do
 * aprendizado. Os cenários abaixo são os do passo de implementação.
 */
@ExtendWith(MockitoExtension.class)
class MachineLearningRecomendacaoServiceTest {

    private static final long ALUNO_ID = 7L;
    private static final long DISC = 10L;
    private static final long CONTEUDO = 100L;
    private static final long VINCULO = 200L;

    @Mock private AtributosAlunoService atributosAlunoService;
    @Mock private WekaModeloService wekaModeloService;
    @Mock private BaixoAproveitamentoColetivoService baixoAproveitamentoColetivoService;
    @Mock private RecomendacaoService recomendacaoService;
    @Mock private RecomendacaoSimuladoRepository recomendacaoSimuladoRepository;
    @Mock private AlunoRepository alunoRepository;
    @Mock private ConteudoPlanoRepository conteudoPlanoRepository;
    @Mock private QuestaoAlunoRepository questaoAlunoRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private QuestaoRepository questaoRepository;
    @Mock private QuestaoConteudoRepository questaoConteudoRepository;
    @Mock private AlternativaRepository alternativaRepository;

    private MachineLearningRecomendacaoService service;

    @BeforeEach
    void setUp() {
        service = new MachineLearningRecomendacaoService(
                atributosAlunoService,
                wekaModeloService,
                new RepeticaoEspacadaService(),
                new SelecaoQuestoesService(questaoRepository, questaoConteudoRepository, alternativaRepository),
                new DecisaoGeracaoIAService(),
                baixoAproveitamentoColetivoService,
                recomendacaoService,
                recomendacaoSimuladoRepository,
                alunoRepository,
                conteudoPlanoRepository,
                questaoAlunoRepository,
                auditLogService);
    }

    @Test
    @DisplayName("aluno sem histórico: fallback, sem IA e com dados insuficientes sinalizados")
    void semHistorico() {
        dadoAlunoEConteudo();
        dadoAtributos(atributos(null, null, null, 0, 0, null));
        dadoBancoVazio();

        RecomendacaoSimuladoDTO dto = service.recomendar(ALUNO_ID, CONTEUDO, null, null);

        assertThat(dto.getOrigemDecisao()).isEqualTo(OrigemDecisao.FALLBACK_SEM_HISTORICO);
        assertThat(dto.isDadosInsuficientes()).isTrue();
        assertThat(dto.isModeloIndisponivel()).isTrue();
        assertThat(dto.getNecessidadeRevisao()).isEqualTo(NecessidadeRevisao.REVISAR_EM_BREVE);
        assertThat(dto.getQuantidadeRecomendada()).isEqualTo(6);
        assertThat(dto.isNecessitaGeracaoIA()).isFalse();
    }

    @Test
    @DisplayName("aluno com poucos dados: decisão pela regra determinística")
    void poucosDados() {
        dadoAlunoEConteudo();
        dadoAtributos(atributos(0.7, 0.7, 5, 1, 0, 2.0));
        dadoBancoVazio();

        RecomendacaoSimuladoDTO dto = service.recomendar(ALUNO_ID, CONTEUDO, null, null);

        assertThat(dto.getOrigemDecisao()).isEqualTo(OrigemDecisao.REGRA_DETERMINISTICA);
        assertThat(dto.isDadosInsuficientes()).isFalse();
        assertThat(dto.getNecessidadeRevisao()).isEqualTo(NecessidadeRevisao.REVISAR_EM_BREVE);
    }

    @Test
    @DisplayName("aluno com baixo desempenho: revisar agora, 10 questões e geração por IA")
    void baixoDesempenho() {
        dadoAlunoEConteudo();
        dadoAtributos(atributos(0.3, 0.35, 3, 6, 1, 2.0));
        dadoBancoVazio();

        RecomendacaoSimuladoDTO dto = service.recomendar(
                ALUNO_ID, CONTEUDO, null, Set.of(MotivoRecomendacao.BAIXO_APROVEITAMENTO));

        assertThat(dto.getNecessidadeRevisao()).isEqualTo(NecessidadeRevisao.REVISAR_AGORA);
        assertThat(dto.getQuantidadeRecomendada()).isEqualTo(10);
        assertThat(dto.getQuantidadeRecomendada()).isLessThanOrEqualTo(SelecaoQuestoesService.MAXIMO_QUESTOES);
        assertThat(dto.getDecisaoGeracao()).isEqualTo(DecisaoGeracao.GERAR_POR_IA);
        assertThat(dto.isNecessitaGeracaoIA()).isTrue();
        assertThat(dto.getIntervaloRevisaoDias()).isEqualTo(1);
        assertThat(dto.getQuestoesCandidatas()).isEmpty();
    }

    @Test
    @DisplayName("aluno com bom desempenho: domínio estável e intervalo maior")
    void bomDesempenho() {
        dadoAlunoEConteudo();
        dadoAtributos(atributos(0.9, 0.9, 3, 10, 3, 2.5));
        dadoBancoVazio();

        RecomendacaoSimuladoDTO estavel = service.recomendar(ALUNO_ID, CONTEUDO, null, null);

        assertThat(estavel.getNecessidadeRevisao()).isEqualTo(NecessidadeRevisao.DOMINIO_ESTAVEL);
        assertThat(estavel.getQuantidadeRecomendada()).isEqualTo(3);

        // Mesmo aluno, muito tempo sem contato: vira revisão agora, com intervalo curto.
        dadoAtributos(atributos(0.9, 0.9, 40, 10, 3, 2.5));
        RecomendacaoSimuladoDTO esquecido = service.recomendar(ALUNO_ID, CONTEUDO, null, null);

        assertThat(esquecido.getNecessidadeRevisao()).isEqualTo(NecessidadeRevisao.REVISAR_AGORA);
        assertThat(esquecido.getIntervaloRevisaoDias()).isLessThan(estavel.getIntervaloRevisaoDias());
    }

    @Test
    @DisplayName("banco com questões adequadas: reutiliza as existentes")
    void reutilizaBanco() {
        dadoAlunoEConteudo();
        dadoAtributos(atributos(0.3, 0.3, 2, 8, 1, 2.0));
        dadoBanco(dezQuestoes(), Map.of(
                1L, 3, 2L, 3, 3L, 3, 4L, 3, 5L, 3, 6L, 3, 7L, 3, 8L, 3, 9L, 3, 10L, 3));

        RecomendacaoSimuladoDTO dto = service.recomendar(
                ALUNO_ID, CONTEUDO, null, Set.of(MotivoRecomendacao.BAIXO_APROVEITAMENTO));

        assertThat(dto.getDecisaoGeracao()).isEqualTo(DecisaoGeracao.REUTILIZAR_BANCO);
        assertThat(dto.isNecessitaGeracaoIA()).isFalse();
        assertThat(dto.getQuestoesCandidatas()).hasSize(10);
        assertThat(dto.getQuestoesCandidatas()).extracting(Questao::getId).doesNotHaveDuplicates();
        assertThat(dto.getQuestoesCandidatas().size()).isLessThanOrEqualTo(SelecaoQuestoesService.MAXIMO_QUESTOES);
    }

    @Test
    @DisplayName("questão com 4 alternativas é descartada e o banco fica insuficiente")
    void respeitaLimiteDeAlternativas() {
        dadoAlunoEConteudo();
        dadoAtributos(atributos(0.3, 0.3, 2, 8, 1, 2.0));
        dadoBanco(List.of(questao(1L, NivelDificuldade.MEDIA)), Map.of(1L, 4));

        RecomendacaoSimuladoDTO dto = service.recomendar(
                ALUNO_ID, CONTEUDO, null, Set.of(MotivoRecomendacao.BAIXO_APROVEITAMENTO));

        assertThat(dto.getQuestoesCandidatas()).isEmpty();
        assertThat(dto.getDecisaoGeracao()).isEqualTo(DecisaoGeracao.GERAR_POR_IA);
    }

    @Test
    @DisplayName("modelo Weka disponível define a origem da decisão")
    void wekaDisponivel() {
        dadoAlunoEConteudo();
        dadoAtributos(atributos(0.3, 0.3, 2, 8, 1, 2.0));
        dadoBancoVazio();
        given(wekaModeloService.classificar(any())).willReturn(Optional.of(NecessidadeRevisao.REVISAR_AGORA));

        RecomendacaoSimuladoDTO dto = service.recomendar(ALUNO_ID, CONTEUDO, null, null);

        assertThat(dto.getOrigemDecisao()).isEqualTo(OrigemDecisao.WEKA);
        assertThat(dto.isModeloIndisponivel()).isFalse();
        assertThat(dto.getNecessidadeRevisao()).isEqualTo(NecessidadeRevisao.REVISAR_AGORA);
    }

    @Test
    @DisplayName("baixo aproveitamento coletivo bloqueia a geração individual e alerta o professor")
    void alertaColetivo() {
        dadoAlunoEConteudo();
        dadoAtributos(atributos(0.3, 0.3, 2, 8, 1, 2.0));
        dadoBancoVazio();
        given(baixoAproveitamentoColetivoService.avaliar(any()))
                .willReturn("Baixo aproveitamento coletivo: 6 de 8 alunos abaixo de 60%. Recomendamos revisão em sala.");

        RecomendacaoSimuladoDTO dto = service.recomendar(
                ALUNO_ID, CONTEUDO, null, Set.of(MotivoRecomendacao.BAIXO_APROVEITAMENTO));

        assertThat(dto.isAlertaColetivo()).isTrue();
        assertThat(dto.getAlertaProfessor()).contains("revisão em sala").contains("6 de 8");
        assertThat(dto.isNecessitaGeracaoIA()).isFalse();
        verify(auditLogService).registrar(eq("RecomendacaoSimulado"), eq(ALUNO_ID), any(), any());
    }

    @Test
    @DisplayName("a recomendação é registrada com atributos, decisão e questões selecionadas")
    void registraAprendizado() {
        dadoAlunoEConteudo();
        dadoAtributos(atributos(0.3, 0.3, 2, 8, 1, 2.0));
        dadoBanco(dezQuestoes(), Map.of(
                1L, 3, 2L, 3, 3L, 3, 4L, 3, 5L, 3, 6L, 3, 7L, 3, 8L, 3, 9L, 3, 10L, 3));

        service.recomendar(ALUNO_ID, CONTEUDO, null, Set.of(MotivoRecomendacao.BAIXO_APROVEITAMENTO));

        ArgumentCaptor<RecomendacaoSimulado> capturado = ArgumentCaptor.forClass(RecomendacaoSimulado.class);
        verify(recomendacaoSimuladoRepository).save(capturado.capture());

        RecomendacaoSimulado registro = capturado.getValue();
        assertThat(registro.getAluno().getId()).isEqualTo(ALUNO_ID);
        assertThat(registro.getDisciplina().getId()).isEqualTo(DISC);
        assertThat(registro.getPercentualAcertoConteudo()).isEqualTo(0.3);
        assertThat(registro.getOrigemDecisao()).isEqualTo(OrigemDecisao.REGRA_DETERMINISTICA);
        assertThat(registro.getNecessidadeRevisao()).isEqualTo(NecessidadeRevisao.REVISAR_AGORA);
        assertThat(registro.getQuantidadeRecomendada()).isEqualTo(10);
        assertThat(registro.getQuestoesSelecionadas()).hasSize(10);
        assertThat(registro.getMotivos()).contains(MotivoRecomendacao.BAIXO_APROVEITAMENTO);
        assertThat(registro.getMotivo()).contains("REVISAR_AGORA");
    }

    @Test
    @DisplayName("desfecho é gravado na recomendação que gerou o simulado, não em outra que tocou as mesmas questões")
    void registraDesfechoDaRecomendacaoDoSimulado() {
        Simulado simulado = simulado(77L);
        RecomendacaoSimulado ligadaAoTentativa = registroPendente(1L, List.of(questao(1L, NivelDificuldade.MEDIA)));
        ligadaAoTentativa.setSimulado(simulado);
        RecomendacaoSimulado outra = registroPendente(2L, List.of(questao(1L, NivelDificuldade.MEDIA)));
        given(recomendacaoSimuladoRepository.findByAluno_IdAndPercentualAcertoPosteriorIsNullOrderByCreatedAtDesc(ALUNO_ID))
                .willReturn(List.of(outra, ligadaAoTentativa));

        int registrados = service.registrarResultado(ALUNO_ID, 77L, 0.42, List.of(1L));

        assertThat(registrados).isEqualTo(1);
        assertThat(ligadaAoTentativa.getPercentualAcertoPosterior()).isEqualTo(0.42);
        assertThat(ligadaAoTentativa.getDataResultado()).isEqualTo(LocalDate.now());
        assertThat(outra.getPercentualAcertoPosterior()).isNull();
        verify(recomendacaoSimuladoRepository, never()).save(outra);
        verify(wekaModeloService).invalidarModelo();
    }

    @Test
    @DisplayName("sem vínculo com o simulado, uma única recomendação recebe o desfecho")
    void registraDesfechoEmUmaUnicaRecomendacao() {
        RecomendacaoSimulado maisRecente = registroPendente(1L, List.of(questao(1L, NivelDificuldade.MEDIA)));
        RecomendacaoSimulado maisAntiga = registroPendente(2L, List.of(questao(1L, NivelDificuldade.MEDIA)));
        given(recomendacaoSimuladoRepository.findByAluno_IdAndPercentualAcertoPosteriorIsNullOrderByCreatedAtDesc(ALUNO_ID))
                .willReturn(List.of(maisRecente, maisAntiga));

        int registrados = service.registrarResultado(ALUNO_ID, null, 0.8, List.of(1L));

        assertThat(registrados).isEqualTo(1);
        assertThat(maisRecente.getPercentualAcertoPosterior()).isEqualTo(0.8);
        assertThat(maisAntiga.getPercentualAcertoPosterior()).isNull();
        verify(recomendacaoSimuladoRepository, never()).save(maisAntiga);
    }

    @Test
    @DisplayName("tentativa sem recomendação aberta (ou sem tocar as questões) não rotula nada")
    void naoRotulaSemRecomendacaoCorrespondente() {
        RecomendacaoSimulado outra = registroPendente(2L, List.of(questao(99L, NivelDificuldade.MEDIA)));
        given(recomendacaoSimuladoRepository.findByAluno_IdAndPercentualAcertoPosteriorIsNullOrderByCreatedAtDesc(ALUNO_ID))
                .willReturn(List.of(outra));

        assertThat(service.registrarResultado(ALUNO_ID, 77L, 0.9, List.of(1L))).isZero();
        assertThat(outra.getPercentualAcertoPosterior()).isNull();
        verify(wekaModeloService, never()).invalidarModelo();
    }

    @Test
    @DisplayName("vincular o simulado gerado fecha o rastro da recomendação")
    void vinculaSimulado() {
        RecomendacaoSimulado registro = new RecomendacaoSimulado();
        registro.setId(5L);
        given(recomendacaoSimuladoRepository.findById(5L)).willReturn(Optional.of(registro));

        Simulado simulado = new Simulado();
        simulado.setId(77L);
        service.vincularSimulado(5L, simulado);

        assertThat(registro.getSimulado()).isSameAs(simulado);
        verify(recomendacaoSimuladoRepository).save(registro);
    }

    // --- helpers -----------------------------------------------------------

    private void dadoAlunoEConteudo() {
        Aluno aluno = new Aluno();
        aluno.setId(ALUNO_ID);
        given(alunoRepository.findById(ALUNO_ID)).willReturn(Optional.of(aluno));

        given(conteudoPlanoRepository.findById(CONTEUDO)).willReturn(Optional.of(conteudo()));
        given(conteudoPlanoRepository.findByPlanoEnsino_TurmaDisciplina_IdIn(Set.of(VINCULO)))
                .willReturn(List.of(conteudo()));
        given(recomendacaoSimuladoRepository.save(any())).willAnswer(chamada -> chamada.getArgument(0));
    }

    private void dadoAtributos(AtributosRecomendacao atributos) {
        given(atributosAlunoService.montar(ALUNO_ID, CONTEUDO, DISC)).willReturn(atributos);
    }

    private void dadoBancoVazio() {
        given(questaoRepository.findByDisciplina_IdIn(any())).willReturn(List.of());
    }

    private void dadoBanco(List<Questao> questoes, Map<Long, Integer> alternativasPorQuestao) {
        List<Long> ids = questoes.stream().map(Questao::getId).toList();
        given(questaoRepository.findByDisciplina_IdIn(any())).willReturn(questoes);
        given(questaoConteudoRepository.findByQuestao_IdIn(ids)).willReturn(vinculos(questoes));
        given(alternativaRepository.findByQuestao_IdIn(ids)).willReturn(alternativas(alternativasPorQuestao));
    }

    /** 5 fáceis + 5 médias no conteúdo recomendado (bate com a distribuição de REVISAR_AGORA). */
    private static List<Questao> dezQuestoes() {
        List<Questao> questoes = new ArrayList<>();
        for (long id = 1; id <= 5; id++) {
            questoes.add(questao(id, NivelDificuldade.FACIL));
        }
        for (long id = 6; id <= 10; id++) {
            questoes.add(questao(id, NivelDificuldade.MEDIA));
        }
        return questoes;
    }

    private static List<QuestaoConteudo> vinculos(List<Questao> questoes) {
        List<QuestaoConteudo> vinculos = new ArrayList<>();
        for (Questao questao : questoes) {
            QuestaoConteudo vinculo = new QuestaoConteudo();
            vinculo.setQuestao(questao);
            ConteudoPlano plano = new ConteudoPlano();
            plano.setId(CONTEUDO);
            vinculo.setConteudoPlano(plano);
            vinculos.add(vinculo);
        }
        return vinculos;
    }

    private static List<Alternativa> alternativas(Map<Long, Integer> quantidades) {
        List<Alternativa> alternativas = new ArrayList<>();
        quantidades.forEach((questaoId, quantidade) -> {
            for (int i = 0; i < quantidade; i++) {
                Alternativa alternativa = new Alternativa();
                alternativa.setQuestao(questao(questaoId, NivelDificuldade.MEDIA));
                alternativas.add(alternativa);
            }
        });
        return alternativas;
    }

    private static AtributosRecomendacao atributos(Double conteudo, Double recente, Integer diasResposta,
            Integer respostas, Integer revisoes, Double dificuldade) {
        return new AtributosRecomendacao(ALUNO_ID, CONTEUDO, DISC, conteudo, recente, conteudo, 2, revisoes,
                diasResposta, null, dificuldade, respostas);
    }

    private static Questao questao(long id, NivelDificuldade nivel) {
        Questao questao = new Questao();
        questao.setId(id);
        questao.setTipo(TipoQuestao.ALTERNATIVAS);
        questao.setStatus(StatusQuestao.APROVADA);
        questao.setNivelDificuldade(nivel);

        Disciplina disciplina = new Disciplina();
        disciplina.setId(DISC);
        questao.setDisciplina(disciplina);
        return questao;
    }

    private static ConteudoPlano conteudo() {
        ConteudoPlano conteudo = new ConteudoPlano();
        conteudo.setId(CONTEUDO);
        conteudo.setTitulo("Montagem de Circuitos");

        Disciplina disciplina = new Disciplina();
        disciplina.setId(DISC);

        TurmaDisciplina vinculo = new TurmaDisciplina();
        vinculo.setId(VINCULO);
        vinculo.setDisciplina(disciplina);

        PlanoEnsino planoEnsino = new PlanoEnsino();
        planoEnsino.setId(VINCULO);
        planoEnsino.setTurmaDisciplina(vinculo);
        conteudo.setPlanoEnsino(planoEnsino);
        return conteudo;
    }

    private static RecomendacaoSimulado registroPendente(Long id, List<Questao> questoes) {
        RecomendacaoSimulado registro = new RecomendacaoSimulado();
        registro.setId(id);
        registro.setQuestoesSelecionadas(new ArrayList<>(questoes));
        return registro;
    }

    private static Simulado simulado(Long id) {
        Simulado simulado = new Simulado();
        simulado.setId(id);
        return simulado;
    }
}
