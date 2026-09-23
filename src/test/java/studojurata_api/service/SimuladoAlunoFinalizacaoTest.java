package studojurata_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import studojurata_api.dto.FinalizarSimuladoRequest;
import studojurata_api.ia.service.RevisaoConteudoService;
import studojurata_api.machinelearning.service.MachineLearningRecomendacaoService;
import studojurata_api.model.Aluno;
import studojurata_api.model.Alternativa;
import studojurata_api.model.Questao;
import studojurata_api.model.Simulado;
import studojurata_api.model.SimuladoAluno;
import studojurata_api.model.SimuladoQuestao;
import studojurata_api.model.enums.StatusSimulado;
import studojurata_api.model.enums.StatusSimuladoAluno;
import studojurata_api.model.enums.StatusSimuladoQuestao;
import studojurata_api.model.enums.TipoQuestao;
import studojurata_api.repository.AlternativaRepository;
import studojurata_api.repository.QuestaoAlunoRepository;
import studojurata_api.repository.QuestaoConteudoRepository;
import studojurata_api.repository.SimuladoAlunoRepository;
import studojurata_api.repository.SimuladoQuestaoRepository;
import studojurata_api.security.AlunoAccessGuard;
import studojurata_api.security.EscopoProfessor;
import studojurata_api.security.UsuarioAutenticado;
import studojurata_api.service.gamificacao.PontuacaoAlunoService;
import studojurata_api.support.AuthorizationTestSupport;

/**
 * C1 — a autorização de escrita em {@code finalizar} acontece <b>antes</b> de
 * qualquer efeito: tentativa alheia (ou escrita de professor) não grava
 * respostas, não recalcula nota, não concede moedas, não registra revisão e não
 * escreve auditoria.
 *
 * <p>O guard é real, com o escopo dublado, para que o 403 venha da regra e não
 * de um mock.
 */
@ExtendWith(MockitoExtension.class)
class SimuladoAlunoFinalizacaoTest {

    private static final long TENTATIVA_ID = 500L;
    private static final long SIMULADO_ID = 300L;
    private static final long ALUNO_ID = 7L;
    private static final long OUTRO_ALUNO_ID = 8L;
    private static final long PROFESSOR_ID = 42L;
    private static final long QUESTAO_ID = 900L;
    private static final long ALTERNATIVA_ID = 901L;

    @Mock private SimuladoAlunoRepository repository;
    @Mock private SimuladoQuestaoRepository simuladoQuestaoRepository;
    @Mock private QuestaoAlunoRepository questaoAlunoRepository;
    @Mock private AlternativaRepository alternativaRepository;
    @Mock private QuestaoConteudoRepository questaoConteudoRepository;
    @Mock private NotaService notaService;
    @Mock private AuditLogService auditLogService;
    @Mock private PontuacaoAlunoService pontuacaoAlunoService;
    @Mock private RevisaoConteudoService revisaoConteudoService;
    @Mock private SimuladoService simuladoService;
    @Mock private MachineLearningRecomendacaoService machineLearningRecomendacaoService;

    private EscopoProfessor escopoProfessor;
    private SimuladoAlunoService service;

    @BeforeEach
    void setUp() {
        escopoProfessor = mock(EscopoProfessor.class);
        AlunoAccessGuard guard = new AlunoAccessGuard(escopoProfessor);
        service = new SimuladoAlunoService(repository, simuladoQuestaoRepository, questaoAlunoRepository,
                alternativaRepository, questaoConteudoRepository, notaService, auditLogService,
                pontuacaoAlunoService, revisaoConteudoService, guard, simuladoService, new UsuarioAutenticado(),
                machineLearningRecomendacaoService);
    }

    @AfterEach
    void limparContexto() {
        AuthorizationTestSupport.limparContexto();
    }

    @Test
    @DisplayName("aluno dono finaliza a própria tentativa e os efeitos acontecem")
    void alunoDonoFinaliza() {
        AuthorizationTestSupport.autenticarComoAluno(ALUNO_ID);
        prepararFluxoLegitimo(ALUNO_ID);

        SimuladoAlunoService.ResultadoFinalizacao resultado =
                service.finalizar(TENTATIVA_ID, new FinalizarSimuladoRequest());

        assertThat(resultado.simuladoAluno().getStatus()).isEqualTo(StatusSimuladoAluno.CONCLUIDO);
        verify(repository).save(any(SimuladoAluno.class));
        verify(pontuacaoAlunoService).concederMoedas(ALUNO_ID, PontuacaoAlunoService.MOEDAS_POR_SIMULADO_CONCLUIDO);
    }

    @Test
    @DisplayName("tentativa de outro aluno: 403 e nenhum efeito colateral")
    void outroAlunoNaoFinalizaNemPersiste() {
        AuthorizationTestSupport.autenticarComoAluno(OUTRO_ALUNO_ID);
        given(repository.findById(TENTATIVA_ID)).willReturn(Optional.of(tentativaDoAluno(ALUNO_ID)));

        assertForbidden(() -> service.finalizar(TENTATIVA_ID, new FinalizarSimuladoRequest()));
        assertNadaPersistido();
    }

    @Test
    @DisplayName("tentativa sem nenhuma resposta não gera rótulo de treino")
    void tentativaEmBrancoNaoGeraRotulo() {
        AuthorizationTestSupport.autenticarComoAluno(ALUNO_ID);
        prepararFluxoLegitimo(ALUNO_ID);

        service.finalizar(TENTATIVA_ID, new FinalizarSimuladoRequest());

        // Tudo em branco não é desempenho observável: a recomendação continua aberta.
        verify(machineLearningRecomendacaoService, never()).registrarResultado(any(), any(), anyDouble(), any());
    }

    @Test
    @DisplayName("tentativa respondida registra o desfecho uma vez, com o simulado correto")
    void tentativaRespondidaRegistraDesfecho() {
        AuthorizationTestSupport.autenticarComoAluno(ALUNO_ID);
        prepararFluxoLegitimo(ALUNO_ID, questaoComAlternativaCorreta());

        FinalizarSimuladoRequest request = new FinalizarSimuladoRequest();
        FinalizarSimuladoRequest.Item resposta = new FinalizarSimuladoRequest.Item();
        resposta.setQuestaoId(QUESTAO_ID);
        resposta.setAlternativaId(ALTERNATIVA_ID);
        resposta.setTempoResposta(30);
        request.setRespostas(List.of(resposta));
        request.setTempoGastoTotal(60);

        service.finalizar(TENTATIVA_ID, request);

        // Percentual real da tentativa (acertou a única questão) e vínculo com o simulado.
        verify(machineLearningRecomendacaoService)
                .registrarResultado(ALUNO_ID, SIMULADO_ID, 1.0, List.of(QUESTAO_ID));
    }

    @Test
    @DisplayName("professor não finaliza tentativa de aluno nem consulta escopo de escrita")
    void professorNaoFinalizaTentativaDeAluno() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(repository.findById(TENTATIVA_ID)).willReturn(Optional.of(tentativaDoAluno(ALUNO_ID)));

        assertForbidden(() -> service.finalizar(TENTATIVA_ID, new FinalizarSimuladoRequest()));
        assertNadaPersistido();

        verify(escopoProfessor, never()).lecionaPara(any(), any());
    }

    @Test
    @DisplayName("administrador finaliza tentativa de qualquer aluno")
    void administradorFinalizaTentativaDeAluno() {
        AuthorizationTestSupport.autenticarComoAdministrador();
        prepararFluxoLegitimo(ALUNO_ID);

        service.finalizar(TENTATIVA_ID, new FinalizarSimuladoRequest());

        verify(repository).save(any(SimuladoAluno.class));
    }

    @Test
    @DisplayName("sem autenticação: 403 e nenhum efeito colateral")
    void semAutenticacaoNaoFinalizaNemPersiste() {
        given(repository.findById(TENTATIVA_ID)).willReturn(Optional.of(tentativaDoAluno(ALUNO_ID)));

        assertForbidden(() -> service.finalizar(TENTATIVA_ID, new FinalizarSimuladoRequest()));
        assertNadaPersistido();
    }

    /** Fluxo legítimo: tentativa do dono, simulado aberto e sem questões ativas. */
    private void prepararFluxoLegitimo(long alunoId) {
        prepararFluxoLegitimo(alunoId, null);
    }

    private void prepararFluxoLegitimo(long alunoId, SimuladoQuestao vinculo) {
        given(repository.findById(TENTATIVA_ID)).willReturn(Optional.of(tentativaDoAluno(alunoId)));
        given(repository.save(any(SimuladoAluno.class))).willAnswer(invocacao -> invocacao.getArgument(0));
        given(simuladoQuestaoRepository.findBySimuladoIdAndStatusOrderByOrdem(SIMULADO_ID, StatusSimuladoQuestao.ATIVA))
                .willReturn(vinculo == null ? List.of() : List.of(vinculo));
    }

    /** Questão ALTERNATIVAS com alternativa correta, para a tentativa ter resposta real. */
    private SimuladoQuestao questaoComAlternativaCorreta() {
        Questao questao = new Questao();
        questao.setId(QUESTAO_ID);
        questao.setTipo(TipoQuestao.ALTERNATIVAS);

        Alternativa alternativa = new Alternativa();
        alternativa.setId(ALTERNATIVA_ID);
        alternativa.setQuestao(questao);
        alternativa.setCorreta(true);
        given(alternativaRepository.findById(ALTERNATIVA_ID)).willReturn(Optional.of(alternativa));

        SimuladoQuestao vinculo = new SimuladoQuestao();
        vinculo.setSimulado(tentativaDoAluno(ALUNO_ID).getSimulado());
        vinculo.setQuestao(questao);
        vinculo.setStatus(StatusSimuladoQuestao.ATIVA);
        return vinculo;
    }

    private void assertNadaPersistido() {
        verify(repository, never()).save(any(SimuladoAluno.class));
        verifyNoInteractions(questaoAlunoRepository, alternativaRepository, simuladoQuestaoRepository,
                notaService, pontuacaoAlunoService, revisaoConteudoService, auditLogService);
    }

    private void assertForbidden(Runnable acao) {
        try {
            acao.run();
        } catch (ResponseStatusException ex) {
            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            return;
        }
        fail("esperava ResponseStatusException 403, mas a finalização foi permitida");
    }

    private static SimuladoAluno tentativaDoAluno(long alunoId) {
        Aluno aluno = new Aluno();
        aluno.setId(alunoId);

        Simulado simulado = new Simulado();
        simulado.setId(SIMULADO_ID);
        simulado.setStatus(StatusSimulado.PUBLICADO);

        SimuladoAluno tentativa = new SimuladoAluno();
        tentativa.setId(TENTATIVA_ID);
        tentativa.setAluno(aluno);
        tentativa.setSimulado(simulado);
        tentativa.setStatus(StatusSimuladoAluno.PENDENTE);
        return tentativa;
    }
}
