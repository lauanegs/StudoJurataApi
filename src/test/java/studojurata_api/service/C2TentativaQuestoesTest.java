package studojurata_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
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

import com.fasterxml.jackson.databind.ObjectMapper;

import studojurata_api.dto.QuestaoDaTentativaDTO;
import studojurata_api.exception.RecursoNaoEncontradoException;
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
 * C2.7b + C2.7c — conteudo da prova da propria tentativa, sem gabarito durante
 * a realizacao e com gabarito apenas depois de concluida.
 */
@ExtendWith(MockitoExtension.class)
class C2TentativaQuestoesTest {

    private static final long TENTATIVA_ID = 500L;
    private static final long SIMULADO_ID = 300L;
    private static final long OUTRO_SIMULADO_ID = 999L;
    private static final long ALUNO_ID = 7L;
    private static final long OUTRO_ALUNO_ID = 8L;
    private static final long PROFESSOR_ID = 42L;

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

    private final ObjectMapper objectMapper = new ObjectMapper();
    private SimuladoAlunoService service;

    @BeforeEach
    void setUp() {
        AlunoAccessGuard guard = new AlunoAccessGuard(mock(EscopoProfessor.class));
        service = new SimuladoAlunoService(repository, simuladoQuestaoRepository, questaoAlunoRepository,
                alternativaRepository, questaoConteudoRepository, notaService, auditLogService,
                pontuacaoAlunoService, revisaoConteudoService, guard, simuladoService, new UsuarioAutenticado(),
                mock(MachineLearningRecomendacaoService.class));
    }

    @AfterEach
    void limparContexto() {
        AuthorizationTestSupport.limparContexto();
    }

    @Test
    @DisplayName("tentativa PENDENTE: alternativas na ordem, sem correta e sem gabarito (nem no JSON)")
    void pendenteNaoExpoeGabarito() throws Exception {
        AuthorizationTestSupport.autenticarComoAluno(ALUNO_ID);
        prepararTentativa(StatusSimuladoAluno.PENDENTE);

        QuestaoDaTentativaDTO dto = service.questoesDaTentativa(TENTATIVA_ID);

        assertThat(dto.getStatus()).isEqualTo(StatusSimuladoAluno.PENDENTE);
        assertThat(dto.getGabarito()).isNull();
        assertThat(dto.getQuestoes()).hasSize(1);
        assertThat(dto.getQuestoes().get(0).getAlternativas())
                .extracting(alternativa -> alternativa.getOrdem())
                .containsExactly(1, 2, 3);

        String json = objectMapper.writeValueAsString(dto);
        assertThat(json).doesNotContain("correta");
        assertThat(json).doesNotContain("gabarito");
        assertThat(json).contains("enunciado", "alternativas");
    }

    @Test
    @DisplayName("tentativa CONCLUIDA: gabarito separado, sem reabrir o campo correta na alternativa")
    void concluidaExpoeGabaritoSeparado() throws Exception {
        AuthorizationTestSupport.autenticarComoAluno(ALUNO_ID);
        prepararTentativa(StatusSimuladoAluno.CONCLUIDO);

        QuestaoDaTentativaDTO dto = service.questoesDaTentativa(TENTATIVA_ID);

        assertThat(dto.getGabarito()).containsEntry(91L, true).containsEntry(92L, false).containsEntry(93L, false);

        String json = objectMapper.writeValueAsString(dto);
        assertThat(json).contains("gabarito");
        assertThat(json).doesNotContain("correta");
    }

    @Test
    @DisplayName("outro aluno recebe 403 e nada e consultado")
    void outroAlunoRecehe403() {
        AuthorizationTestSupport.autenticarComoAluno(OUTRO_ALUNO_ID);
        given(repository.findById(TENTATIVA_ID)).willReturn(Optional.of(tentativa(StatusSimuladoAluno.PENDENTE)));

        assertForbidden(() -> service.questoesDaTentativa(TENTATIVA_ID));
        verifyNoInteractions(simuladoQuestaoRepository, alternativaRepository);
    }

    @Test
    @DisplayName("professor recebe 403 neste endpoint de prova")
    void professorRecehe403() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(repository.findById(TENTATIVA_ID)).willReturn(Optional.of(tentativa(StatusSimuladoAluno.CONCLUIDO)));

        assertForbidden(() -> service.questoesDaTentativa(TENTATIVA_ID));
        verifyNoInteractions(simuladoQuestaoRepository, alternativaRepository);
    }

    @Test
    @DisplayName("administrador acessa conforme a regra administrativa")
    void administradorAcessa() {
        AuthorizationTestSupport.autenticarComoAdministrador();
        prepararTentativa(StatusSimuladoAluno.PENDENTE);

        assertThat(service.questoesDaTentativa(TENTATIVA_ID).getQuestoes()).hasSize(1);
    }

    @Test
    @DisplayName("sem autenticacao recebe 403")
    void semAutenticacaoRecehe403() {
        AuthorizationTestSupport.limparContexto();
        given(repository.findById(TENTATIVA_ID)).willReturn(Optional.of(tentativa(StatusSimuladoAluno.PENDENTE)));

        assertForbidden(() -> service.questoesDaTentativa(TENTATIVA_ID));
    }

    @Test
    @DisplayName("tentativa inexistente responde 404")
    void tentativaInexistenteResponde404() {
        AuthorizationTestSupport.autenticarComoAluno(ALUNO_ID);
        given(repository.findById(TENTATIVA_ID)).willReturn(Optional.empty());

        try {
            service.questoesDaTentativa(TENTATIVA_ID);
            fail("esperava RecursoNaoEncontradoException");
        } catch (RecursoNaoEncontradoException esperado) {
            verifyNoInteractions(alternativaRepository);
        }
    }

    @Test
    @DisplayName("somente vinculos ATIVA do simulado da tentativa entram na prova")
    void somenteVinculosAtivosDoSimuladoDaTentativa() {
        AuthorizationTestSupport.autenticarComoAluno(ALUNO_ID);
        prepararTentativa(StatusSimuladoAluno.PENDENTE);

        assertThat(service.questoesDaTentativa(TENTATIVA_ID).getQuestoes())
                .extracting(item -> item.getQuestaoId())
                .containsExactly(1L);

        // A consulta e do simulado da tentativa e apenas com status ATIVA.
        verify(simuladoQuestaoRepository)
                .findBySimuladoIdAndStatusOrderByOrdem(SIMULADO_ID, StatusSimuladoQuestao.ATIVA);
    }

    @Test
    @DisplayName("alternativas das questoes sao carregadas em lote (una consulta)")
    void alternativasCarregadasEmLote() {
        AuthorizationTestSupport.autenticarComoAluno(ALUNO_ID);
        prepararTentativa(StatusSimuladoAluno.PENDENTE);

        service.questoesDaTentativa(TENTATIVA_ID);

        verify(alternativaRepository, times(1)).findByQuestao_IdIn(any());
    }

    @Test
    @DisplayName("tentativa sem questoes ativas devolve lista vazia sem consultar alternativas")
    void semQuestoesNaoConsultaAlternativas() {
        AuthorizationTestSupport.autenticarComoAluno(ALUNO_ID);
        given(repository.findById(TENTATIVA_ID)).willReturn(Optional.of(tentativa(StatusSimuladoAluno.PENDENTE)));
        given(simuladoQuestaoRepository.findBySimuladoIdAndStatusOrderByOrdem(SIMULADO_ID, StatusSimuladoQuestao.ATIVA))
                .willReturn(List.of());

        QuestaoDaTentativaDTO dto = service.questoesDaTentativa(TENTATIVA_ID);

        assertThat(dto.getQuestoes()).isEmpty();
        assertThat(dto.getGabarito()).isNull();
        verifyNoInteractions(alternativaRepository);
    }

    @Test
    @DisplayName("a consulta da prova nao escreve nada: correcao e efeitos ficam em finalizar")
    void consultaNaoProduzEfeitos() {
        AuthorizationTestSupport.autenticarComoAluno(ALUNO_ID);
        prepararTentativa(StatusSimuladoAluno.CONCLUIDO);

        service.questoesDaTentativa(TENTATIVA_ID);

        verifyNoInteractions(questaoAlunoRepository, notaService, pontuacaoAlunoService,
                revisaoConteudoService, auditLogService);
        verify(repository, org.mockito.Mockito.never()).save(any());
    }

    // --- helpers -----------------------------------------------------------

    private void prepararTentativa(StatusSimuladoAluno status) {
        given(repository.findById(TENTATIVA_ID)).willReturn(Optional.of(tentativa(status)));
        given(simuladoQuestaoRepository.findBySimuladoIdAndStatusOrderByOrdem(SIMULADO_ID, StatusSimuladoQuestao.ATIVA))
                .willReturn(List.of(vinculo(1L, 1)));
        // Ordem proposital fora de sequencia: o servico ordena por `ordem`.
        given(alternativaRepository.findByQuestao_IdIn(List.of(1L)))
                .willReturn(List.of(alternativa(93L, 3, false), alternativa(91L, 1, true), alternativa(92L, 2, false)));
    }

    private void assertForbidden(Runnable acao) {
        try {
            acao.run();
        } catch (ResponseStatusException ex) {
            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            return;
        }
        fail("esperava ResponseStatusException 403, mas a chamada foi permitida");
    }

    private static SimuladoAluno tentativa(StatusSimuladoAluno status) {
        Aluno aluno = new Aluno();
        aluno.setId(ALUNO_ID);

        Simulado simulado = new Simulado();
        simulado.setId(SIMULADO_ID);
        simulado.setStatus(StatusSimulado.PUBLICADO);

        SimuladoAluno tentativa = new SimuladoAluno();
        tentativa.setId(TENTATIVA_ID);
        tentativa.setAluno(aluno);
        tentativa.setSimulado(simulado);
        tentativa.setStatus(status);
        return tentativa;
    }

    private static SimuladoQuestao vinculo(long questaoId, int ordem) {
        Questao questao = new Questao();
        questao.setId(questaoId);
        questao.setEnunciado("Enunciado " + questaoId);
        questao.setTipo(TipoQuestao.ALTERNATIVAS);

        SimuladoQuestao vinculo = new SimuladoQuestao();
        vinculo.setSimulado(tentativa(StatusSimuladoAluno.PENDENTE).getSimulado());
        vinculo.setQuestao(questao);
        vinculo.setOrdem(ordem);
        vinculo.setStatus(StatusSimuladoQuestao.ATIVA);
        return vinculo;
    }

    private static Alternativa alternativa(long id, int ordem, boolean correta) {
        Questao questao = new Questao();
        questao.setId(1L);

        Alternativa alternativa = new Alternativa();
        alternativa.setId(id);
        alternativa.setQuestao(questao);
        alternativa.setTexto("Alternativa " + id);
        alternativa.setOrdem(ordem);
        alternativa.setCorreta(correta);
        return alternativa;
    }
}
