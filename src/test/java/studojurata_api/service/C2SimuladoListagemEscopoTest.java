package studojurata_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.Optional;
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
import studojurata_api.model.Turma;
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
import studojurata_api.security.SimuladoAccessGuard;
import studojurata_api.security.UsuarioAutenticado;
import studojurata_api.service.gamificacao.PontuacaoAlunoService;
import studojurata_api.ia.service.RevisaoConteudoService;
import studojurata_api.machinelearning.service.MachineLearningRecomendacaoService;
import studojurata_api.support.AuthorizationTestSupport;

/**
 * C2.4 — escopo das listagens de simulado, de tentativa e de vinculo
 * questao-simulado.
 *
 * <p>Simulado sem turma (orfao) fica fora do alcance de professor e aluno: sem
 * vinculo verificavel nao ha como atribuir posse, entao so o ADMINISTRADOR le —
 * os orfaos continuam no banco, apenas nao entram nas listagens deles.
 */
@ExtendWith(MockitoExtension.class)
class C2SimuladoListagemEscopoTest {

    private static final long ALUNO_ID = 7L;
    private static final long PROFESSOR_ID = 42L;
    private static final long TURMA_ID = 10L;
    private static final long OUTRA_TURMA_ID = 99L;
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
        SimuladoAccessGuard simuladoAccessGuard = new SimuladoAccessGuard(usuarioAutenticado, escopoProfessor);
        simuladoService = new SimuladoService(simuladoRepository, simuladoQuestaoRepository,
                simuladoAlunoRepository, mock(studojurata_api.ia.repository.SimuladoGeradoIARepository.class),
                alunoTurmaService, alunoRepository, usuarioAutenticado, escopoProfessor, simuladoAccessGuard);
        simuladoAlunoService = new SimuladoAlunoService(simuladoAlunoRepository, simuladoQuestaoRepository,
                questaoAlunoRepository, alternativaRepository, questaoConteudoRepository, notaService,
                auditLogService, pontuacaoAlunoService, revisaoConteudoService, mock(AlunoAccessGuard.class),
                simuladoServiceDublado, usuarioAutenticado, mock(MachineLearningRecomendacaoService.class));
        simuladoQuestaoService = new SimuladoQuestaoService(simuladoQuestaoRepository, questaoConteudoRepository,
                usuarioAutenticado, simuladoServiceDublado, simuladoAccessGuard);
    }

    @AfterEach
    void limparContexto() {
        AuthorizationTestSupport.limparContexto();
    }

    // --- GET /simulados -----------------------------------------------------

    @Test
    @DisplayName("administrador lista todos os simulados, orfao inclusive")
    void administradorListaTodosOsSimulados() {
        AuthorizationTestSupport.autenticarComoAdministrador();
        given(simuladoRepository.findAll()).willReturn(List.of(simulado(SIMULADO_ID), simulado(ORFAO_ID)));

        assertThat(simuladoService.listar()).extracting(Simulado::getId)
                .containsExactlyInAnyOrder(SIMULADO_ID, ORFAO_ID);
    }

    @Test
    @DisplayName("professor ve apenas os simulados das suas turmas (orfao fica com o administrador)")
    void professorVeApenasAsSuasTurmas() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(escopoProfessor.turmaIdsDoProfessor(PROFESSOR_ID)).willReturn(Set.of(TURMA_ID));
        given(simuladoRepository.findByTurma_IdIn(Set.of(TURMA_ID))).willReturn(List.of(simulado(SIMULADO_ID)));
        given(simuladoRepository.findAllById(Set.of(SIMULADO_ID))).willReturn(List.of(simulado(SIMULADO_ID)));

        assertThat(simuladoService.listar()).extracting(Simulado::getId)
                .containsExactly(SIMULADO_ID);

        verify(simuladoRepository, never()).findAll();
    }

    @Test
    @DisplayName("professor sem turmas recebe lista vazia, sem consultar vinculos nem orfaos")
    void professorSemTurmasRecebeListaVazia() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(escopoProfessor.turmaIdsDoProfessor(PROFESSOR_ID)).willReturn(Set.of());

        assertThat(simuladoService.listar()).isEmpty();

        verify(simuladoRepository, never()).findByTurma_IdIn(org.mockito.ArgumentMatchers.any());
        verify(simuladoRepository, never()).findAll();
        verify(simuladoRepository, never()).findAllById(org.mockito.ArgumentMatchers.any());
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

    // --- GET /simulados/{id} (leitura individual) ---------------------------

    @Test
    @DisplayName("leitura individual: professor le o simulado da sua turma")
    void professorLeSimuladoDaPropriaTurma() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(simuladoRepository.findById(SIMULADO_ID)).willReturn(Optional.of(simulado(SIMULADO_ID, TURMA_ID)));
        given(escopoProfessor.turmaIdsDoProfessor(PROFESSOR_ID)).willReturn(Set.of(TURMA_ID));
        given(simuladoRepository.findByTurma_IdIn(Set.of(TURMA_ID)))
                .willReturn(List.of(simulado(SIMULADO_ID, TURMA_ID)));

        assertThat(simuladoService.buscarParaLeitura(SIMULADO_ID).getId()).isEqualTo(SIMULADO_ID);
    }

    @Test
    @DisplayName("leitura individual: professor nao le simulado de turma alheia")
    void professorNaoLeSimuladoDeTurmaAlheia() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(simuladoRepository.findById(SIMULADO_ID)).willReturn(Optional.of(simulado(SIMULADO_ID, OUTRA_TURMA_ID)));
        given(escopoProfessor.turmaIdsDoProfessor(PROFESSOR_ID)).willReturn(Set.of(TURMA_ID));
        given(simuladoRepository.findByTurma_IdIn(Set.of(TURMA_ID))).willReturn(List.of());

        assertForbidden(() -> simuladoService.buscarParaLeitura(SIMULADO_ID));
    }

    @Test
    @DisplayName("leitura individual: simulado orfao nao e visivel ao professor")
    void professorNaoLeSimuladoOrfao() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(simuladoRepository.findById(ORFAO_ID)).willReturn(Optional.of(simulado(ORFAO_ID)));
        given(escopoProfessor.turmaIdsDoProfessor(PROFESSOR_ID)).willReturn(Set.of());

        assertForbidden(() -> simuladoService.buscarParaLeitura(ORFAO_ID));

        verify(simuladoRepository, never()).findByTurma_IdIn(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("leitura individual: aluno le o simulado em que tem tentativa")
    void alunoLeSimuladoDaPropriaTentativa() {
        AuthorizationTestSupport.autenticarComoAluno(ALUNO_ID);
        given(simuladoRepository.findById(SIMULADO_ID)).willReturn(Optional.of(simulado(SIMULADO_ID, TURMA_ID)));
        given(simuladoAlunoRepository.findByAlunoId(ALUNO_ID))
                .willReturn(List.of(tentativa(SIMULADO_ID, StatusSimuladoAluno.PENDENTE)));

        assertThat(simuladoService.buscarParaLeitura(SIMULADO_ID).getId()).isEqualTo(SIMULADO_ID);
    }

    @Test
    @DisplayName("leitura individual: aluno nao le simulado em que nao tem tentativa")
    void alunoNaoLeSimuladoSemTentativa() {
        AuthorizationTestSupport.autenticarComoAluno(ALUNO_ID);
        given(simuladoRepository.findById(SIMULADO_ID)).willReturn(Optional.of(simulado(SIMULADO_ID, TURMA_ID)));
        given(simuladoAlunoRepository.findByAlunoId(ALUNO_ID)).willReturn(List.of());

        assertForbidden(() -> simuladoService.buscarParaLeitura(SIMULADO_ID));
    }

    @Test
    @DisplayName("leitura individual: administrador le qualquer simulado sem consultar escopo")
    void administradorLeQualquerSimulado() {
        AuthorizationTestSupport.autenticarComoAdministrador();
        given(simuladoRepository.findById(SIMULADO_ID)).willReturn(Optional.of(simulado(SIMULADO_ID, OUTRA_TURMA_ID)));

        assertThat(simuladoService.buscarParaLeitura(SIMULADO_ID).getId()).isEqualTo(SIMULADO_ID);

        verifyNoInteractions(escopoProfessor);
    }

    @Test
    @DisplayName("leitura individual: administrador le simulado orfao; aluno sem tentativa nao le")
    void administradorLeSimuladoOrfao() {
        given(simuladoRepository.findById(ORFAO_ID)).willReturn(Optional.of(simulado(ORFAO_ID)));

        AuthorizationTestSupport.autenticarComoAdministrador();
        assertThat(simuladoService.buscarParaLeitura(ORFAO_ID).getId()).isEqualTo(ORFAO_ID);

        AuthorizationTestSupport.autenticarComoAluno(ALUNO_ID);
        given(simuladoAlunoRepository.findByAlunoId(ALUNO_ID)).willReturn(List.of());
        assertForbidden(() -> simuladoService.buscarParaLeitura(ORFAO_ID));
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

    private static Simulado simulado(long id, long turmaId) {
        Simulado simulado = simulado(id);
        Turma turma = new Turma();
        turma.setId(turmaId);
        simulado.setTurma(turma);
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
