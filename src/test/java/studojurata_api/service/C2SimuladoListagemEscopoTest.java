package studojurata_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import studojurata_api.model.Aluno;
import studojurata_api.model.Simulado;
import studojurata_api.model.SimuladoAluno;
import studojurata_api.model.SimuladoQuestao;
import studojurata_api.model.enums.StatusSimuladoAluno;
import studojurata_api.repository.AlunoRepository;
import studojurata_api.repository.AlternativaRepository;
import studojurata_api.repository.QuestaoAlunoRepository;
import studojurata_api.repository.QuestaoConteudoRepository;
import studojurata_api.repository.SimuladoAlunoRepository;
import studojurata_api.repository.SimuladoQuestaoRepository;
import studojurata_api.repository.SimuladoRepository;
import studojurata_api.security.AlunoAccessGuard;
import studojurata_api.security.EscopoProfessor;
import studojurata_api.security.UsuarioAutenticado;
import studojurata_api.service.gamificacao.PontuacaoAlunoService;
import studojurata_api.ia.service.RevisaoConteudoService;
import studojurata_api.support.AuthorizationTestSupport;

/**
 * C2.4 — escopo das listagens de simulado, de tentativa e de vinculo
 * questao-simulado. Orfaos (simulado sem turma) seguem preservados por D-C1.
 */
@ExtendWith(MockitoExtension.class)
class C2SimuladoListagemEscopoTest {

    private static final long ALUNO_ID = 7L;
    private static final long PROFESSOR_ID = 42L;
    private static final long TURMA_ID = 10L;
    private static final long SIMULADO_ID = 300L;
    private static final long ORFAO_ID = 900L;

    @Mock private SimuladoRepository simuladoRepository;
    @Mock private SimuladoAlunoRepository simuladoAlunoRepository;
    @Mock private SimuladoQuestaoRepository simuladoQuestaoRepository;
    @Mock private AlunoTurmaService alunoTurmaService;
    @Mock private AlunoRepository alunoRepository;
    @Mock private EscopoProfessor escopoProfessor;
    @Mock private SimuladoService simuladoServiceDublado;
    @Mock private QuestaoAlunoRepository questaoAlunoRepository;
    @Mock private AlternativaRepository alternativaRepository;
    @Mock private QuestaoConteudoRepository questaoConteudoRepository;
    @Mock private NotaService notaService;
    @Mock private AuditLogService auditLogService;
    @Mock private PontuacaoAlunoService pontuacaoAlunoService;
    @Mock private RevisaoConteudoService revisaoConteudoService;

    private UsuarioAutenticado usuarioAutenticado;
    private SimuladoService simuladoService;
    private SimuladoAlunoService simuladoAlunoService;
    private SimuladoQuestaoService simuladoQuestaoService;

    @BeforeEach
    void setUp() {
        usuarioAutenticado = new UsuarioAutenticado();
        simuladoService = new SimuladoService(simuladoRepository, simuladoQuestaoRepository,
                simuladoAlunoRepository, alunoTurmaService, alunoRepository, usuarioAutenticado, escopoProfessor);
        simuladoAlunoService = new SimuladoAlunoService(simuladoAlunoRepository, simuladoQuestaoRepository,
                questaoAlunoRepository, alternativaRepository, questaoConteudoRepository, notaService,
                auditLogService, pontuacaoAlunoService, revisaoConteudoService, mock(AlunoAccessGuard.class),
                simuladoServiceDublado, usuarioAutenticado);
        simuladoQuestaoService = new SimuladoQuestaoService(simuladoQuestaoRepository, questaoConteudoRepository,
                usuarioAutenticado, simuladoServiceDublado);
    }

    @AfterEach
    void limparContexto() {
        AuthorizationTestSupport.limparContexto();
    }

    // --- GET /simulados -----------------------------------------------------

    @Test
    @DisplayName("administrador lista todos os simulados")
    void administradorListaTodosOsSimulados() {
        AuthorizationTestSupport.autenticarComoAdministrador();
        given(simuladoRepository.findAll()).willReturn(List.of(simulado(SIMULADO_ID), simulado(ORFAO_ID)));

        assertThat(simuladoService.listar()).hasSize(2);
    }

    @Test
    @DisplayName("professor ve os simulados das suas turmas e preserva os orfaos (D-C1)")
    void professorVeTurmasEOrfaos() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(escopoProfessor.turmaIdsDoProfessor(PROFESSOR_ID)).willReturn(Set.of(TURMA_ID));
        given(simuladoRepository.findByTurma_IdIn(Set.of(TURMA_ID))).willReturn(List.of(simulado(SIMULADO_ID)));
        given(simuladoRepository.findByTurmaIsNull()).willReturn(List.of(simulado(ORFAO_ID)));
        given(simuladoRepository.findAllById(Set.of(SIMULADO_ID, ORFAO_ID)))
                .willReturn(List.of(simulado(SIMULADO_ID), simulado(ORFAO_ID)));

        assertThat(simuladoService.listar()).extracting(Simulado::getId)
                .containsExactlyInAnyOrder(SIMULADO_ID, ORFAO_ID);

        verify(simuladoRepository, never()).findAll();
    }

    @Test
    @DisplayName("professor sem turmas continua vendo os orfaos, sem consultar vinculos")
    void professorSemTurmasVeOrfaos() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(escopoProfessor.turmaIdsDoProfessor(PROFESSOR_ID)).willReturn(Set.of());
        given(simuladoRepository.findByTurmaIsNull()).willReturn(List.of(simulado(ORFAO_ID)));
        given(simuladoRepository.findAllById(Set.of(ORFAO_ID))).willReturn(List.of(simulado(ORFAO_ID)));

        assertThat(simuladoService.listar()).extracting(Simulado::getId).containsExactly(ORFAO_ID);

        verify(simuladoRepository, never()).findByTurma_IdIn(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("aluno ve apenas os simulados em que tem tentativa")
    void alunoVeSimuladosDasSuasTentativas() {
        AuthorizationTestSupport.autenticarComoAluno(ALUNO_ID);
        given(simuladoAlunoRepository.findByAlunoId(ALUNO_ID))
                .willReturn(List.of(tentativa(SIMULADO_ID, StatusSimuladoAluno.PENDENTE)));
        given(simuladoRepository.findAllById(Set.of(SIMULADO_ID))).willReturn(List.of(simulado(SIMULADO_ID)));

        assertThat(simuladoService.listar()).extracting(Simulado::getId).containsExactly(SIMULADO_ID);
    }

    @Test
    @DisplayName("aluno sem tentativa nao consulta o repositorio de simulados")
    void alunoSemTentativaNaoConsultaSimulados() {
        AuthorizationTestSupport.autenticarComoAluno(ALUNO_ID);
        given(simuladoAlunoRepository.findByAlunoId(ALUNO_ID)).willReturn(List.of());

        assertThat(simuladoService.listar()).isEmpty();

        verifyNoInteractions(simuladoRepository);
    }

    @Test
    @DisplayName("sem autenticacao a listagem de simulados falha fechado (403)")
    void semAutenticacaoNaoListaSimulados() {
        AuthorizationTestSupport.limparContexto();

        assertForbidden(() -> simuladoService.listar());
        verifyNoInteractions(simuladoRepository);
    }

    // --- GET /simulado-aluno ------------------------------------------------

    @Test
    @DisplayName("administrador lista todas as tentativas")
    void administradorListaTodasAsTentativas() {
        AuthorizationTestSupport.autenticarComoAdministrador();
        given(simuladoAlunoRepository.findAll()).willReturn(List.of(tentativa(SIMULADO_ID, StatusSimuladoAluno.PENDENTE)));

        assertThat(simuladoAlunoService.listar()).hasSize(1);
    }

    @Test
    @DisplayName("professor ve tentativas dos seus simulados, inclusive PENDENTE")
    void professorVeTentativasInclusivePendentes() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(simuladoServiceDublado.simuladoIdsVisiveis()).willReturn(Set.of(SIMULADO_ID));
        given(simuladoAlunoRepository.findBySimulado_IdIn(Set.of(SIMULADO_ID)))
                .willReturn(List.of(tentativa(SIMULADO_ID, StatusSimuladoAluno.PENDENTE)));

        assertThat(simuladoAlunoService.listar())
                .extracting(SimuladoAluno::getStatus)
                .containsExactly(StatusSimuladoAluno.PENDENTE);

        verify(simuladoAlunoRepository, never()).findAll();
    }

    @Test
    @DisplayName("aluno ve apenas as proprias tentativas")
    void alunoVeApenasPropriasTentativas() {
        AuthorizationTestSupport.autenticarComoAluno(ALUNO_ID);
        given(simuladoAlunoRepository.findByAlunoId(ALUNO_ID))
                .willReturn(List.of(tentativa(SIMULADO_ID, StatusSimuladoAluno.CONCLUIDO)));

        assertThat(simuladoAlunoService.listar()).hasSize(1);
    }

    @Test
    @DisplayName("escopo vazio na listagem de tentativas nao consulta repositorio")
    void escopoVazioNaoConsultaTentativas() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(simuladoServiceDublado.simuladoIdsVisiveis()).willReturn(Set.of());

        assertThat(simuladoAlunoService.listar()).isEmpty();

        verifyNoInteractions(simuladoAlunoRepository);
    }

    @Test
    @DisplayName("sem autenticacao a listagem de tentativas falha fechado (403)")
    void semAutenticacaoNaoListaTentativas() {
        AuthorizationTestSupport.limparContexto();

        assertForbidden(() -> simuladoAlunoService.listar());
        verifyNoInteractions(simuladoAlunoRepository);
    }

    // --- GET /simulado-questao ---------------------------------------------

    @Test
    @DisplayName("administrador lista todos os vinculos questao-simulado")
    void administradorListaTodosOsVinculos() {
        AuthorizationTestSupport.autenticarComoAdministrador();
        given(simuladoQuestaoRepository.findAll()).willReturn(List.of(new SimuladoQuestao()));

        assertThat(simuladoQuestaoService.listar()).hasSize(1);
    }

    @Test
    @DisplayName("professor ve vinculos apenas dos seus simulados")
    void professorVeVinculosDosSeusSimulados() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(simuladoServiceDublado.simuladoIdsVisiveis()).willReturn(Set.of(SIMULADO_ID));
        given(simuladoQuestaoRepository.findBySimulado_IdIn(Set.of(SIMULADO_ID)))
                .willReturn(List.of(new SimuladoQuestao()));

        assertThat(simuladoQuestaoService.listar()).hasSize(1);

        verify(simuladoQuestaoRepository, never()).findAll();
    }

    @Test
    @DisplayName("escopo vazio na listagem de vinculos nao consulta repositorio")
    void escopoVazioNaoConsultaVinculos() {
        AuthorizationTestSupport.autenticarComoAluno(ALUNO_ID);
        given(simuladoServiceDublado.simuladoIdsVisiveis()).willReturn(Set.of());

        assertThat(simuladoQuestaoService.listar()).isEmpty();

        verifyNoInteractions(simuladoQuestaoRepository);
    }

    // --- helpers -----------------------------------------------------------

    private void assertForbidden(Runnable acao) {
        try {
            acao.run();
        } catch (ResponseStatusException ex) {
            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            return;
        }
        fail("esperava ResponseStatusException 403, mas a chamada foi permitida");
    }

    private static Simulado simulado(long id) {
        Simulado simulado = new Simulado();
        simulado.setId(id);
        return simulado;
    }

    private static SimuladoAluno tentativa(long simuladoId, StatusSimuladoAluno status) {
        Aluno aluno = new Aluno();
        aluno.setId(ALUNO_ID);

        SimuladoAluno tentativa = new SimuladoAluno();
        tentativa.setId(500L);
        tentativa.setAluno(aluno);
        tentativa.setSimulado(simulado(simuladoId));
        tentativa.setStatus(status);
        return tentativa;
    }
}
