package studojurata_api.ia.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import studojurata_api.exception.RegraNegocioException;
import studojurata_api.ia.model.enums.MotivoRecomendacao;
import studojurata_api.ia.repository.RevisaoConteudoRepository;
import studojurata_api.ia.repository.SimuladoGeradoIARepository;
import studojurata_api.machinelearning.dto.RecomendacaoSimuladoDTO;
import studojurata_api.machinelearning.model.enums.DecisaoGeracao;
import studojurata_api.machinelearning.model.enums.NecessidadeRevisao;
import studojurata_api.machinelearning.model.enums.OrigemDecisao;
import studojurata_api.machinelearning.service.MachineLearningRecomendacaoService;
import studojurata_api.model.Aluno;
import studojurata_api.model.Aula;
import studojurata_api.model.AulaConteudo;
import studojurata_api.model.ConteudoPlano;
import studojurata_api.model.PlanoEnsino;
import studojurata_api.model.Questao;
import studojurata_api.model.Simulado;
import studojurata_api.model.Turma;
import studojurata_api.model.TurmaDisciplina;
import studojurata_api.model.Disciplina;
import studojurata_api.model.enums.NivelDificuldade;
import studojurata_api.model.enums.StatusSimulado;
import studojurata_api.repository.AlunoRepository;
import studojurata_api.repository.ConteudoPlanoRepository;
import studojurata_api.repository.SimuladoQuestaoRepository;
import studojurata_api.repository.SimuladoRepository;
import studojurata_api.support.AuthorizationTestSupport;

/**
 * Integração da camada de ML com o fluxo que já existia de simulado individual
 * por IA: a recomendação define quantidade/dificuldade, é vinculada ao simulado
 * gerado e o alerta coletivo bloqueia a geração individual.
 */
@ExtendWith(MockitoExtension.class)
class GeracaoSimuladoIAServiceMlTest {

    private static final long ALUNO_ID = 7L;
    private static final long CONTEUDO = 100L;
    private static final long SIMULADO_ID = 500L;

    @Mock private SimuladoRepository simuladoRepository;
    @Mock private SimuladoQuestaoRepository simuladoQuestaoRepository;
    @Mock private ConteudoPlanoRepository conteudoPlanoRepository;
    @Mock private AlunoRepository alunoRepository;
    @Mock private GeracaoQuestaoIAService geracaoQuestaoIAService;
    @Mock private SimuladoGeradoIARepository simuladoGeradoIARepository;
    @Mock private RevisaoConteudoRepository revisaoConteudoRepository;
    @Mock private MachineLearningRecomendacaoService machineLearningRecomendacaoService;
    @Mock private studojurata_api.service.SimuladoService simuladoService;
    @Mock private studojurata_api.security.SimuladoAccessGuard simuladoAccessGuard;
    @Mock private studojurata_api.repository.AulaConteudoRepository aulaConteudoRepository;

    private GeracaoSimuladoIAService service;

    @BeforeEach
    void setUp() {
        service = new GeracaoSimuladoIAService(simuladoRepository, simuladoQuestaoRepository, conteudoPlanoRepository,
                alunoRepository, geracaoQuestaoIAService, simuladoGeradoIARepository, revisaoConteudoRepository,
                machineLearningRecomendacaoService, simuladoService, new studojurata_api.security.UsuarioAutenticado(),
                simuladoAccessGuard, aulaConteudoRepository);
    }

    @Test
    @DisplayName("decisão GERAR_POR_IA: usa quantidade e nível recomendados e vincula a recomendação")
    void geraPorIaComRecomendacao() {
        dadoAlunoEConteudo();
        given(simuladoRepository.save(any())).willAnswer(chamada -> {
            Simulado simulado = chamada.getArgument(0);
            simulado.setId(SIMULADO_ID);
            return simulado;
        });
        given(geracaoQuestaoIAService.gerar(anyLong(), any(), any(), anyInt(), any(), any()))
                .willReturn(questoes(7));
        given(machineLearningRecomendacaoService.recomendar(eq(ALUNO_ID), eq(CONTEUDO), any(), any()))
                .willReturn(recomendacao(7, NivelDificuldade.DIFICIL, DecisaoGeracao.GERAR_POR_IA, List.of()));

        Simulado gerado = service.gerarParaAluno(ALUNO_ID, CONTEUDO, null, Set.of(MotivoRecomendacao.BAIXO_APROVEITAMENTO));

        assertThat(gerado.getId()).isEqualTo(SIMULADO_ID);
        assertThat(gerado.getQuantidadeQuestoes()).isEqualTo(7);
        assertThat(gerado.getStatus()).isEqualTo(StatusSimulado.RASCUNHO);

        // A quantidade recomendada (7) é a que a geração de questões recebe.
        verify(geracaoQuestaoIAService).gerar(eq(CONTEUDO), eq(NivelDificuldade.DIFICIL), any(), eq(7), any(), any());
        verify(machineLearningRecomendacaoService).vincularSimulado(any(), any());
        verify(simuladoQuestaoRepository, times(7)).save(any());
    }

    @Test
    @DisplayName("decisão REUTILIZAR_BANCO: monta o simulado com as candidatas e não chama a IA")
    void reutilizaBancoSemChamarIa() {
        dadoAlunoEConteudo();
        given(simuladoRepository.save(any())).willAnswer(chamada -> {
            Simulado simulado = chamada.getArgument(0);
            simulado.setId(SIMULADO_ID);
            return simulado;
        });
        given(machineLearningRecomendacaoService.recomendar(eq(ALUNO_ID), eq(CONTEUDO), any(), any()))
                .willReturn(recomendacao(6, NivelDificuldade.MEDIA, DecisaoGeracao.REUTILIZAR_BANCO, questoes(3)));

        Simulado gerado = service.gerarParaAluno(ALUNO_ID, CONTEUDO, null, Set.of(MotivoRecomendacao.REPETICAO_ESPACADA));

        // Quantidade declarada passa a ser a quantidade real de questões vinculadas.
        assertThat(gerado.getQuantidadeQuestoes()).isEqualTo(3);
        verify(simuladoQuestaoRepository, times(3)).save(any());
        verify(geracaoQuestaoIAService, never()).gerar(any(), any(), any(), anyInt(), any(), any());
    }

    @Test
    @DisplayName("sem questões no banco e sem gatilho: recusa em vez de chamar a IA")
    void semQuestoesNemGatilhoRecusa() {
        dadoAlunoEConteudo();
        given(simuladoRepository.save(any())).willAnswer(chamada -> {
            Simulado simulado = chamada.getArgument(0);
            simulado.setId(SIMULADO_ID);
            return simulado;
        });
        given(machineLearningRecomendacaoService.recomendar(eq(ALUNO_ID), eq(CONTEUDO), any(), any()))
                .willReturn(recomendacao(6, NivelDificuldade.MEDIA, DecisaoGeracao.REUTILIZAR_BANCO, List.of()));

        assertThatThrownBy(() -> service.gerarParaAluno(ALUNO_ID, CONTEUDO, null, Set.of()))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("gatilho");

        verify(geracaoQuestaoIAService, never()).gerar(any(), any(), any(), anyInt(), any(), any());
    }

    @Test
    @DisplayName("alerta de baixo aproveitamento coletivo impede o simulado individual")
    void alertaColetivoImpedeGeracao() {
        dadoAlunoEConteudo();
        RecomendacaoSimuladoDTO recomendacao =
                recomendacao(6, NivelDificuldade.MEDIA, DecisaoGeracao.GERAR_POR_IA, questoes(6));
        recomendacao.setAlertaProfessor("Maioria da turma abaixo do limiar: recomendamos revisão em sala.");
        given(machineLearningRecomendacaoService.recomendar(eq(ALUNO_ID), eq(CONTEUDO), any(), any()))
                .willReturn(recomendacao);

        assertThatThrownBy(() -> service.gerarParaAluno(
                ALUNO_ID, CONTEUDO, null, Set.of(MotivoRecomendacao.BAIXO_APROVEITAMENTO)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("revisão em sala");

        verify(simuladoRepository, never()).save(any());
        verify(geracaoQuestaoIAService, never()).gerar(any(), any(), any(), anyInt(), any(), any());
        verify(machineLearningRecomendacaoService, never()).vincularSimulado(any(), any());
    }

    // --- contexto da turma e escopo do professor (M9) -----------------------

    @Test
    @DisplayName("geração usa turma, disciplina e plano do conteúdo")
    void geracaoUsaTurmaDisciplinaEPlanoDoConteudo() {
        dadoAlunoEConteudo();
        dadoCaminoFelizDaGeracao();

        Simulado gerado = service.gerarParaAluno(ALUNO_ID, CONTEUDO, null, Set.of());

        assertThat(gerado.getTurma().getId()).isEqualTo(20L);
        assertThat(gerado.getDisciplina().getId()).isEqualTo(10L);
        assertThat(gerado.getPlanoEnsino().getId()).isEqualTo(40L);
    }

    @Test
    @DisplayName("professor gera dentro do próprio escopo e o guard é consultado")
    void professorGeraDentroDoEscopo() {
        AuthorizationTestSupport.autenticarComoProfessor(42L);
        dadoAlunoEConteudo();
        dadoCaminoFelizDaGeracao();

        Simulado gerado = service.gerarParaAluno(ALUNO_ID, CONTEUDO, null, Set.of());

        assertThat(gerado.getStatus()).isEqualTo(StatusSimulado.RASCUNHO);
        verify(simuladoAccessGuard).garantirEscrita(gerado);
    }

    @Test
    @DisplayName("professor não gera para contexto fora do escopo e nada é persistido")
    void professorNaoGeraForaDoEscopo() {
        AuthorizationTestSupport.autenticarComoProfessor(42L);
        dadoAlunoEConteudo();
        dadoRecomendacaoGerarPorIa(2);
        willThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "fora do escopo"))
                .given(simuladoAccessGuard).garantirEscrita(any());

        assertThatThrownBy(() -> service.gerarParaAluno(ALUNO_ID, CONTEUDO, null, Set.of()))
                .isInstanceOf(ResponseStatusException.class);

        verify(simuladoRepository, never()).save(any());
        verify(simuladoGeradoIARepository, never()).save(any());
        verify(geracaoQuestaoIAService, never()).gerar(any(), any(), any(), anyInt(), any(), any());
    }

    @Test
    @DisplayName("conteúdo sem plano de ensino não gera reforço (nada é persistido)")
    void conteudoSemPlanoNaoGera() {
        dadoAlunoEConteudo();
        dadoConteudoSemPlano();

        assertThatThrownBy(() -> service.gerarParaAluno(ALUNO_ID, CONTEUDO, null, Set.of()))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("turma e disciplina");

        assertThatNoSimuladoPersistido();
    }

    @Test
    @DisplayName("conteúdo de plano genérico não gera reforço (nada é persistido)")
    void conteudoDePlanoGenericoNaoGera() {
        dadoAlunoEConteudo();
        dadoConteudoDePlanoGenerico();

        assertThatThrownBy(() -> service.gerarParaAluno(ALUNO_ID, CONTEUDO, null, Set.of()))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("turma e disciplina");

        assertThatNoSimuladoPersistido();
    }

    @Test
    @DisplayName("administrador não passa pelo guard de escopo")
    void administradorNaoPassaPeloGuard() {
        AuthorizationTestSupport.autenticarComoAdministrador();
        dadoAlunoEConteudo();
        dadoCaminoFelizDaGeracao();

        service.gerarParaAluno(ALUNO_ID, CONTEUDO, null, Set.of());

        verifyNoInteractions(simuladoAccessGuard);
    }

    @Test
    @DisplayName("job sem sessão continua gerando (não passa pelo guard nem pela autenticação)")
    void jobSemSessaoContinuaGerando() {
        AuthorizationTestSupport.limparContexto();
        dadoAlunoEConteudo();
        dadoCaminoFelizDaGeracao();

        Simulado gerado = service.gerarParaAluno(ALUNO_ID, CONTEUDO, null, Set.of());

        assertThat(gerado.getId()).isEqualTo(SIMULADO_ID);
        verifyNoInteractions(simuladoAccessGuard);
    }

    private void dadoCaminoFelizDaGeracao() {
        dadoRecomendacaoGerarPorIa(2);
        given(simuladoRepository.save(any())).willAnswer(chamada -> {
            Simulado simulado = chamada.getArgument(0);
            simulado.setId(SIMULADO_ID);
            return simulado;
        });
    }

    private void dadoRecomendacaoGerarPorIa(int quantidade) {
        // Lenient: quando o fluxo para antes (escopo recusado, por exemplo) a
        // geração de questões não é alcançada.
        lenient().when(geracaoQuestaoIAService.gerar(anyLong(), any(), any(), anyInt(), any(), any()))
                .thenReturn(questoes(quantidade));
        given(machineLearningRecomendacaoService.recomendar(eq(ALUNO_ID), eq(CONTEUDO), any(), any()))
                .willReturn(recomendacao(quantidade, NivelDificuldade.MEDIA, DecisaoGeracao.GERAR_POR_IA, List.of()));
    }

    private void dadoConteudoSemPlano() {
        ConteudoPlano semPlano = new ConteudoPlano();
        semPlano.setId(CONTEUDO);
        semPlano.setTitulo("Conteúdo avulso");
        given(conteudoPlanoRepository.findById(CONTEUDO)).willReturn(Optional.of(semPlano));
    }

    private void dadoConteudoDePlanoGenerico() {
        PlanoEnsino generico = new PlanoEnsino();
        generico.setId(41L);
        generico.setTurmaDisciplina(null);

        ConteudoPlano conteudo = new ConteudoPlano();
        conteudo.setId(CONTEUDO);
        conteudo.setTitulo("Conteúdo de plano genérico");
        conteudo.setPlanoEnsino(generico);
        given(conteudoPlanoRepository.findById(CONTEUDO)).willReturn(Optional.of(conteudo));
    }

    private void assertThatNoSimuladoPersistido() {
        verify(simuladoRepository, never()).save(any());
        verify(simuladoGeradoIARepository, never()).save(any());
        verify(geracaoQuestaoIAService, never()).gerar(any(), any(), any(), anyInt(), any(), any());
    }

    // --- conteúdo futuro não gera reforço (Fase 1 do M8) --------------------

    @Test
    @DisplayName("conteúdo sem aulas vinculadas continua gerando")
    void conteudoSemAulasContinuaGerando() {
        dadoAlunoEConteudo();
        dadoCaminoFelizDaGeracao();
        given(aulaConteudoRepository.findByConteudoPlano_Id(CONTEUDO)).willReturn(List.of());

        Simulado gerado = service.gerarParaAluno(ALUNO_ID, CONTEUDO, null, Set.of());

        assertThat(gerado.getId()).isEqualTo(SIMULADO_ID);
    }

    @Test
    @DisplayName("conteúdo com aulas todas sem data de publicação não gera")
    void conteudoComAulasSemDataNaoGera() {
        dadoAlunoEConteudo();
        given(aulaConteudoRepository.findByConteudoPlano_Id(CONTEUDO))
                .willReturn(List.of(vinculoDeAula(null), vinculoDeAula(null)));

        assertThatThrownBy(() -> service.gerarParaAluno(ALUNO_ID, CONTEUDO, null, Set.of()))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("planejado para o futuro");

        assertThatNoSimuladoPersistido();
    }

    @Test
    @DisplayName("conteúdo com aulas apenas futuras não gera")
    void conteudoComAulasFuturasNaoGera() {
        dadoAlunoEConteudo();
        LocalDate futuro = LocalDate.now().plusDays(7);
        given(aulaConteudoRepository.findByConteudoPlano_Id(CONTEUDO))
                .willReturn(List.of(vinculoDeAula(futuro), vinculoDeAula(futuro.plusDays(1))));

        assertThatThrownBy(() -> service.gerarParaAluno(ALUNO_ID, CONTEUDO, null, Set.of()))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("planejado para o futuro");

        assertThatNoSimuladoPersistido();
    }

    @Test
    @DisplayName("uma aula futura e outra já realizada liberam a geração")
    void aulaRealizadaLiberaGeracao() {
        dadoAlunoEConteudo();
        dadoCaminoFelizDaGeracao();
        given(aulaConteudoRepository.findByConteudoPlano_Id(CONTEUDO))
                .willReturn(List.of(vinculoDeAula(LocalDate.now()), vinculoDeAula(LocalDate.now().plusDays(3))));

        Simulado gerado = service.gerarParaAluno(ALUNO_ID, CONTEUDO, null, Set.of());

        assertThat(gerado.getId()).isEqualTo(SIMULADO_ID);
    }

    @Test
    @DisplayName("conteúdo já ministrado continua valendo para a revisão espaçada")
    void conteudoJaMinistradoPermiteRevisao() {
        dadoAlunoEConteudo();
        dadoCaminoFelizDaGeracao();
        given(aulaConteudoRepository.findByConteudoPlano_Id(CONTEUDO))
                .willReturn(List.of(vinculoDeAula(LocalDate.now().minusDays(30))));

        service.gerarParaAluno(ALUNO_ID, CONTEUDO, null, Set.of(MotivoRecomendacao.REPETICAO_ESPACADA));

        // O fluxo da IA grava o rascunho e depois reescreve a quantidade real.
        verify(simuladoRepository, times(2)).save(any());
        verify(simuladoGeradoIARepository).save(any());
    }

    private static AulaConteudo vinculoDeAula(LocalDate dataPublicacao) {
        Aula aula = new Aula();
        aula.setDataPublicacao(dataPublicacao);

        AulaConteudo vinculo = new AulaConteudo();
        vinculo.setId(1L);
        vinculo.setAula(aula);
        return vinculo;
    }

    // --- helpers -----------------------------------------------------------

    private void dadoAlunoEConteudo() {
        Aluno aluno = new Aluno();
        aluno.setId(ALUNO_ID);
        aluno.setPessoa(new studojurata_api.model.Pessoa());
        aluno.getPessoa().setNome("Aluno Teste");
        given(alunoRepository.findById(ALUNO_ID)).willReturn(Optional.of(aluno));

        Disciplina disciplina = new Disciplina();
        disciplina.setId(10L);
        Turma turma = new Turma();
        turma.setId(20L);
        TurmaDisciplina vinculo = new TurmaDisciplina();
        vinculo.setId(30L);
        vinculo.setDisciplina(disciplina);
        vinculo.setTurma(turma);
        PlanoEnsino planoEnsino = new PlanoEnsino();
        planoEnsino.setId(40L);
        planoEnsino.setTurmaDisciplina(vinculo);
        ConteudoPlano conteudo = new ConteudoPlano();
        conteudo.setId(CONTEUDO);
        conteudo.setTitulo("Montagem de Circuitos");
        conteudo.setPlanoEnsino(planoEnsino);

        given(conteudoPlanoRepository.findById(CONTEUDO)).willReturn(Optional.of(conteudo));
    }

    private static RecomendacaoSimuladoDTO recomendacao(
            int quantidade, NivelDificuldade nivel, DecisaoGeracao decisao, List<Questao> candidatas) {
        RecomendacaoSimuladoDTO dto = new RecomendacaoSimuladoDTO();
        dto.setId(1L);
        dto.setAlunoId(ALUNO_ID);
        dto.setConteudoPlanoId(CONTEUDO);
        dto.setQuantidadeRecomendada(quantidade);
        dto.setNivelPrioritario(nivel);
        dto.setQuestoesCandidatas(candidatas);
        dto.setNecessidadeRevisao(NecessidadeRevisao.REVISAR_AGORA);
        dto.setOrigemDecisao(OrigemDecisao.REGRA_DETERMINISTICA);
        dto.setDecisaoGeracao(decisao);
        dto.setNecessitaGeracaoIA(decisao == DecisaoGeracao.GERAR_POR_IA);
        return dto;
    }

    private static List<Questao> questoes(int quantidade) {
        List<Questao> questoes = new java.util.ArrayList<>();
        for (long id = 1; id <= quantidade; id++) {
            Questao questao = new Questao();
            questao.setId(id);
            questoes.add(questao);
        }
        return questoes;
    }
}
