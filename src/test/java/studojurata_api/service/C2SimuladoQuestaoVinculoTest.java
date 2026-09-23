package studojurata_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Set;
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

import studojurata_api.exception.RegraNegocioException;
import studojurata_api.exception.RequisicaoInvalidaException;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.model.Disciplina;
import studojurata_api.model.PlanoEnsino;
import studojurata_api.model.Questao;
import studojurata_api.model.Simulado;
import studojurata_api.model.SimuladoQuestao;
import studojurata_api.model.Turma;
import studojurata_api.model.TurmaDisciplina;
import studojurata_api.model.enums.OrigemQuestao;
import studojurata_api.model.enums.StatusSimulado;
import studojurata_api.model.enums.StatusSimuladoQuestao;
import studojurata_api.repository.QuestaoConteudoRepository;
import studojurata_api.repository.SimuladoQuestaoRepository;
import studojurata_api.security.EscopoProfessor;
import studojurata_api.security.SimuladoAccessGuard;
import studojurata_api.security.UsuarioAutenticado;
import studojurata_api.support.AuthorizationTestSupport;

/**
 * C2.7d (passo 4) — escrita do vínculo simulado × questão
 * (POST /simulado-questao).
 *
 * <p>O professor precisa das duas pontas no próprio escopo: simulado com turma,
 * disciplina (e plano, quando houver) verificáveis e questão de disciplina que
 * ele leciona. As regras antigas — questão de IA vinculada a conteúdo e limite
 * de 10 questões — continuam valendo e são reexecutadas aqui.
 */
@ExtendWith(MockitoExtension.class)
class C2SimuladoQuestaoVinculoTest {

    private static final long ALUNO_ID = 7L;
    private static final long PROFESSOR_ID = 42L;
    private static final long DISC_A = 10L;
    private static final long DISC_B = 20L;
    private static final long TURMA_ID = 30L;
    private static final long OUTRA_TURMA_ID = 40L;
    private static final long VINCULO_DO_PROFESSOR = 50L;
    private static final long VINCULO_DE_OUTRO = 60L;
    private static final long PLANO_ID = 70L;
    private static final long SIMULADO_ID = 100L;
    private static final long QUESTAO_ID = 200L;

    @Mock private SimuladoQuestaoRepository repository;
    @Mock private QuestaoConteudoRepository questaoConteudoRepository;
    @Mock private SimuladoService simuladoService;
    @Mock private EscopoProfessor escopoProfessor;

    private SimuladoQuestaoService simuladoQuestaoService;

    @BeforeEach
    void setUp() {
        UsuarioAutenticado usuarioAutenticado = new UsuarioAutenticado();
        SimuladoAccessGuard simuladoAccessGuard = new SimuladoAccessGuard(usuarioAutenticado, escopoProfessor);
        simuladoQuestaoService = new SimuladoQuestaoService(repository, questaoConteudoRepository,
                usuarioAutenticado, simuladoService, simuladoAccessGuard);
    }

    @AfterEach
    void limparContexto() {
        AuthorizationTestSupport.limparContexto();
    }

    // --- Caminho permitido -------------------------------------------------

    @Test
    @DisplayName("professor vincula questão própria a simulado das suas turmas")
    void professorVinculaQuestaoPropria() {
        autenticarProfessorComTurmas(TURMA_ID);
        professorTemDisciplinas(DISC_A);
        given(repository.save(any())).willAnswer(chamada -> chamada.getArgument(0));

        SimuladoQuestao salvo = simuladoQuestaoService.salvar(
                vinculo(simulado(SIMULADO_ID, TURMA_ID, DISC_A), questao(QUESTAO_ID, DISC_A)));

        assertThat(salvo.getSimulado().getId()).isEqualTo(SIMULADO_ID);
        assertThat(salvo.getQuestao().getId()).isEqualTo(QUESTAO_ID);
        // Regra antiga preservada: vínculo novo nasce ATIVA.
        assertThat(salvo.getStatus()).isEqualTo(StatusSimuladoQuestao.ATIVA);
        verify(escopoProfessor).turmaIdsDoProfessor(PROFESSOR_ID);
        // Escopo de disciplina resolvido uma única vez para simulado e questão.
        verify(escopoProfessor, times(1)).disciplinaIdsDoProfessor(PROFESSOR_ID);
    }

    // --- Status do simulado (I2) -------------------------------------------

    @Test
    @DisplayName("simulado publicado não recebe questão nova")
    void simuladoPublicadoNaoRecebeQuestao() {
        autenticarProfessorComTurmas(TURMA_ID);
        professorTemDisciplinas(DISC_A);
        Simulado publicado = simulado(SIMULADO_ID, TURMA_ID, DISC_A);
        publicado.setStatus(StatusSimulado.PUBLICADO);

        assertNegocio(() -> simuladoQuestaoService.salvar(
                vinculo(publicado, questao(QUESTAO_ID, DISC_A))), "RASCUNHO");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("simulado encerrado não recebe questão nova")
    void simuladoEncerradoNaoRecebeQuestao() {
        autenticarProfessorComTurmas(TURMA_ID);
        professorTemDisciplinas(DISC_A);
        Simulado encerrado = simulado(SIMULADO_ID, TURMA_ID, DISC_A);
        encerrado.setStatus(StatusSimulado.ENCERRADO);

        assertNegocio(() -> simuladoQuestaoService.salvar(
                vinculo(encerrado, questao(QUESTAO_ID, DISC_A))), "RASCUNHO");

        verify(repository, never()).save(any());
    }

    // --- Desvínculo de questão (I5) -----------------------------------------

    @Test
    @DisplayName("professor desvincula questão do próprio simulado em rascunho")
    void professorDesvinculaQuestaoDoProprioRascunho() {
        autenticarProfessorComTurmas(TURMA_ID);
        professorTemDisciplinas(DISC_A);
        SimuladoQuestao vinculo = vinculo(simulado(SIMULADO_ID, TURMA_ID, DISC_A), questao(QUESTAO_ID, DISC_A));
        dadoVinculoExistente(vinculo);

        simuladoQuestaoService.desvincular(SIMULADO_ID, QUESTAO_ID);

        // Só o vínculo sai; a questão em si não é tocada (não há repositório dela aqui).
        verify(repository).delete(vinculo);
        verify(repository, never()).deleteAll();
        // A quantidade do simulado volta a ser a contagem real de vínculos ativos.
        verify(simuladoService).sincronizarQuantidadeDeQuestoes(vinculo.getSimulado());
    }

    @Test
    @DisplayName("inclusão de questão sincroniza a quantidade com a contagem real")
    void inclusaoSincronizaQuantidade() {
        autenticarProfessorComTurmas(TURMA_ID);
        professorTemDisciplinas(DISC_A);
        given(repository.save(any())).willAnswer(chamada -> chamada.getArgument(0));

        SimuladoQuestao vinculo = vinculo(simulado(SIMULADO_ID, TURMA_ID, DISC_A), questao(QUESTAO_ID, DISC_A));
        SimuladoQuestao salvo = simuladoQuestaoService.salvar(vinculo);

        assertThat(salvo.getStatus()).isEqualTo(StatusSimuladoQuestao.ATIVA);
        verify(simuladoService).sincronizarQuantidadeDeQuestoes(salvo.getSimulado());
    }

    @Test
    @DisplayName("questão já vinculada não entra duas vezes e não mexe na quantidade")
    void questaoDuplicadaNaoEntraDuasVezes() {
        autenticarProfessorComTurmas(TURMA_ID);
        professorTemDisciplinas(DISC_A);
        given(repository.existsBySimuladoIdAndQuestaoIdAndStatus(
                SIMULADO_ID, QUESTAO_ID, StatusSimuladoQuestao.ATIVA)).willReturn(true);

        assertNegocio(() -> simuladoQuestaoService.salvar(
                vinculo(simulado(SIMULADO_ID, TURMA_ID, DISC_A), questao(QUESTAO_ID, DISC_A))), "já está vinculada");

        verify(repository, never()).save(any());
        verify(simuladoService, never()).sincronizarQuantidadeDeQuestoes(any());
    }

    @Test
    @DisplayName("remoção de vínculo inexistente não altera a quantidade")
    void remocaoInexistenteNaoAlteraQuantidade() {
        autenticarProfessorComTurmas(TURMA_ID);
        professorTemDisciplinas(DISC_A);
        given(simuladoService.buscar(SIMULADO_ID)).willReturn(simulado(SIMULADO_ID, TURMA_ID, DISC_A));
        given(repository.findFirstBySimuladoIdAndQuestaoId(SIMULADO_ID, QUESTAO_ID)).willReturn(Optional.empty());

        try {
            simuladoQuestaoService.desvincular(SIMULADO_ID, QUESTAO_ID);
            fail("esperava RecursoNaoEncontradoException");
        } catch (RecursoNaoEncontradoException esperada) {
            assertThat(esperada.getMessage()).contains("não está vinculada");
        }

        verify(simuladoService, never()).sincronizarQuantidadeDeQuestoes(any());
    }

    @Test
    @DisplayName("falha ao apagar o vínculo não sincroniza a quantidade (sem efeito parcial)")
    void falhaAoApagarNaoSincronizaQuantidade() {
        autenticarProfessorComTurmas(TURMA_ID);
        professorTemDisciplinas(DISC_A);
        SimuladoQuestao vinculo = vinculo(simulado(SIMULADO_ID, TURMA_ID, DISC_A), questao(QUESTAO_ID, DISC_A));
        dadoVinculoExistente(vinculo);
        org.mockito.BDDMockito.willThrow(new RuntimeException("falha ao apagar"))
                .given(repository).delete(vinculo);

        try {
            simuladoQuestaoService.desvincular(SIMULADO_ID, QUESTAO_ID);
            fail("esperava a falha do repositório");
        } catch (RuntimeException esperada) {
            assertThat(esperada.getMessage()).contains("falha ao apagar");
        }

        verify(simuladoService, never()).sincronizarQuantidadeDeQuestoes(any());
    }

    @Test
    @DisplayName("professor não desvincula questão de simulado fora do escopo")
    void professorNaoDesvinculaDeSimuladoAlheio() {
        autenticarProfessorComTurmas(TURMA_ID);
        Simulado alheio = simulado(SIMULADO_ID, OUTRA_TURMA_ID, DISC_A);
        given(simuladoService.buscar(SIMULADO_ID)).willReturn(alheio);

        assertForbidden(() -> simuladoQuestaoService.desvincular(SIMULADO_ID, QUESTAO_ID));

        verify(repository, never()).delete(any());
        verify(repository, never()).findFirstBySimuladoIdAndQuestaoId(any(), any());
    }

    @Test
    @DisplayName("professor não desvincula questão de simulado publicado")
    void professorNaoDesvinculaDeSimuladoPublicado() {
        autenticarProfessorComTurmas(TURMA_ID);
        professorTemDisciplinas(DISC_A);
        Simulado publicado = simulado(SIMULADO_ID, TURMA_ID, DISC_A);
        publicado.setStatus(StatusSimulado.PUBLICADO);
        given(simuladoService.buscar(SIMULADO_ID)).willReturn(publicado);

        assertNegocio(() -> simuladoQuestaoService.desvincular(SIMULADO_ID, QUESTAO_ID), "RASCUNHO");

        verify(repository, never()).delete(any());
    }

    @Test
    @DisplayName("professor não desvincula questão de simulado encerrado")
    void professorNaoDesvinculaDeSimuladoEncerrado() {
        autenticarProfessorComTurmas(TURMA_ID);
        professorTemDisciplinas(DISC_A);
        Simulado encerrado = simulado(SIMULADO_ID, TURMA_ID, DISC_A);
        encerrado.setStatus(StatusSimulado.ENCERRADO);
        given(simuladoService.buscar(SIMULADO_ID)).willReturn(encerrado);

        assertNegocio(() -> simuladoQuestaoService.desvincular(SIMULADO_ID, QUESTAO_ID), "RASCUNHO");

        verify(repository, never()).delete(any());
    }

    @Test
    @DisplayName("administrador desvincula questão de simulado em rascunho, sem consultar escopo")
    void administradorDesvinculaQuestao() {
        AuthorizationTestSupport.autenticarComoAdministrador();
        SimuladoQuestao vinculo = vinculo(simulado(SIMULADO_ID, TURMA_ID, DISC_A), questao(QUESTAO_ID, DISC_A));
        dadoVinculoExistente(vinculo);

        simuladoQuestaoService.desvincular(SIMULADO_ID, QUESTAO_ID);

        verify(repository).delete(vinculo);
        verifyNoInteractions(escopoProfessor);
    }

    @Test
    @DisplayName("aluno não desvincula questão e nada é apagado")
    void alunoNaoDesvinculaQuestao() {
        AuthorizationTestSupport.autenticarComoAluno(ALUNO_ID);
        dadoSimuladoEmRascunho();

        assertForbidden(() -> simuladoQuestaoService.desvincular(SIMULADO_ID, QUESTAO_ID));

        verify(repository, never()).delete(any());
    }

    @Test
    @DisplayName("sem autenticação não desvincula questão")
    void semAutenticacaoNaoDesvinculaQuestao() {
        AuthorizationTestSupport.limparContexto();
        dadoSimuladoEmRascunho();

        assertForbidden(() -> simuladoQuestaoService.desvincular(SIMULADO_ID, QUESTAO_ID));

        verify(repository, never()).delete(any());
    }

    @Test
    @DisplayName("vínculo inexistente responde 404 e nada é apagado")
    void vinculoInexistenteResponde404() {
        autenticarProfessorComTurmas(TURMA_ID);
        professorTemDisciplinas(DISC_A);
        given(simuladoService.buscar(SIMULADO_ID)).willReturn(simulado(SIMULADO_ID, TURMA_ID, DISC_A));
        given(repository.findFirstBySimuladoIdAndQuestaoId(SIMULADO_ID, QUESTAO_ID)).willReturn(Optional.empty());

        try {
            simuladoQuestaoService.desvincular(SIMULADO_ID, QUESTAO_ID);
            fail("esperava RecursoNaoEncontradoException");
        } catch (RecursoNaoEncontradoException esperada) {
            assertThat(esperada.getMessage()).contains("não está vinculada");
        }

        verify(repository, never()).delete(any());
    }

    private void dadoVinculoExistente(SimuladoQuestao vinculo) {
        Simulado simulado = vinculo != null ? vinculo.getSimulado() : null;
        given(simuladoService.buscar(SIMULADO_ID)).willReturn(simulado);
        given(repository.findFirstBySimuladoIdAndQuestaoId(SIMULADO_ID, QUESTAO_ID))
                .willReturn(Optional.of(vinculo));
    }

    /** Apenas o simulado carregado: perfis sem permissão param na autorização. */
    private void dadoSimuladoEmRascunho() {
        given(simuladoService.buscar(SIMULADO_ID)).willReturn(simulado(SIMULADO_ID, TURMA_ID, DISC_A));
    }

    @Test
    @DisplayName("professor vincula questão a simulado com plano das suas disciplinas")
    void professorVinculaComPlanoProprio() {
        autenticarProfessorComTurmas(TURMA_ID);
        professorTemDisciplinas(DISC_A);
        professorTemVinculosDeTurmaDisciplina(VINCULO_DO_PROFESSOR);
        given(repository.save(any())).willAnswer(chamada -> chamada.getArgument(0));

        Simulado simulado = simulado(SIMULADO_ID, TURMA_ID, DISC_A);
        simulado.setPlanoEnsino(plano(VINCULO_DO_PROFESSOR));

        SimuladoQuestao salvo = simuladoQuestaoService.salvar(
                vinculo(simulado, questao(QUESTAO_ID, DISC_A)));

        assertThat(salvo.getSimulado().getId()).isEqualTo(SIMULADO_ID);
        verify(repository).save(any());
        verify(escopoProfessor).turmaDisciplinaIdsDoProfessor(PROFESSOR_ID);
    }

    // --- Questão fora do escopo --------------------------------------------

    @Test
    @DisplayName("professor não vincula questão de disciplina alheia")
    void professorNaoVinculaQuestaoDeDisciplinaAlheia() {
        autenticarProfessorComTurmas(TURMA_ID);
        professorTemDisciplinas(DISC_A);

        assertForbidden(() -> simuladoQuestaoService.salvar(
                vinculo(simulado(SIMULADO_ID, TURMA_ID, DISC_A), questao(QUESTAO_ID, DISC_B))));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("professor não vincula questão sem disciplina")
    void professorNaoVinculaQuestaoSemDisciplina() {
        autenticarProfessorComTurmas(TURMA_ID);
        professorTemDisciplinas(DISC_A);

        assertForbidden(() -> simuladoQuestaoService.salvar(
                vinculo(simulado(SIMULADO_ID, TURMA_ID, DISC_A), questao(QUESTAO_ID, null))));

        verify(repository, never()).save(any());
    }

    // --- Simulado fora do escopo -------------------------------------------

    @Test
    @DisplayName("professor não usa simulado de outro professor")
    void professorNaoUsaSimuladoDeOutroProfessor() {
        autenticarProfessorComTurmas(TURMA_ID);

        assertForbidden(() -> simuladoQuestaoService.salvar(
                vinculo(simulado(SIMULADO_ID, OUTRA_TURMA_ID, DISC_B), questao(QUESTAO_ID, DISC_B))));

        verify(repository, never()).save(any());
        // Recusa na turma: nem chega a consultar o escopo de disciplinas.
        verify(escopoProfessor, never()).disciplinaIdsDoProfessor(any());
    }

    @Test
    @DisplayName("professor não usa simulado fora do escopo por turma")
    void professorNaoUsaSimuladoForaDoEscopoPorTurma() {
        autenticarProfessorComTurmas(TURMA_ID);

        assertForbidden(() -> simuladoQuestaoService.salvar(
                vinculo(simulado(SIMULADO_ID, OUTRA_TURMA_ID, DISC_A), questao(QUESTAO_ID, DISC_A))));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("professor não usa simulado fora do escopo por disciplina")
    void professorNaoUsaSimuladoForaDoEscopoPorDisciplina() {
        autenticarProfessorComTurmas(TURMA_ID);
        professorTemDisciplinas(DISC_A);

        assertForbidden(() -> simuladoQuestaoService.salvar(
                vinculo(simulado(SIMULADO_ID, TURMA_ID, DISC_B), questao(QUESTAO_ID, DISC_B))));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("professor não usa simulado com plano de outro professor")
    void professorNaoUsaSimuladoComPlanoDeOutro() {
        autenticarProfessorComTurmas(TURMA_ID);
        professorTemDisciplinas(DISC_A);
        professorTemVinculosDeTurmaDisciplina(VINCULO_DO_PROFESSOR);

        Simulado simulado = simulado(SIMULADO_ID, TURMA_ID, DISC_A);
        simulado.setPlanoEnsino(plano(VINCULO_DE_OUTRO));

        assertForbidden(() -> simuladoQuestaoService.salvar(
                vinculo(simulado, questao(QUESTAO_ID, DISC_A))));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("professor não usa simulado com plano genérico (sem escopo verificável)")
    void professorNaoUsaSimuladoComPlanoGenerico() {
        autenticarProfessorComTurmas(TURMA_ID);
        professorTemDisciplinas(DISC_A);

        Simulado simulado = simulado(SIMULADO_ID, TURMA_ID, DISC_A);
        simulado.setPlanoEnsino(plano(null));

        assertForbidden(() -> simuladoQuestaoService.salvar(
                vinculo(simulado, questao(QUESTAO_ID, DISC_A))));

        verify(repository, never()).save(any());
        // Plano sem vínculo: recusa sem ir ao repositório de vínculos.
        verify(escopoProfessor, never()).turmaDisciplinaIdsDoProfessor(any());
    }

    @Test
    @DisplayName("professor não usa simulado órfão (sem turma)")
    void professorNaoUsaSimuladoOrfao() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);

        assertForbidden(() -> simuladoQuestaoService.salvar(
                vinculo(simulado(SIMULADO_ID, null, DISC_A), questao(QUESTAO_ID, DISC_A))));

        verify(repository, never()).save(any());
        // Sem turma não há escopo verificável: recusa antes de qualquer consulta.
        verifyNoInteractions(escopoProfessor);
    }

    @Test
    @DisplayName("professor não usa simulado sem disciplina")
    void professorNaoUsaSimuladoSemDisciplina() {
        autenticarProfessorComTurmas(TURMA_ID);

        assertForbidden(() -> simuladoQuestaoService.salvar(
                vinculo(simulado(SIMULADO_ID, TURMA_ID, null), questao(QUESTAO_ID, DISC_A))));

        verify(repository, never()).save(any());
        // Recusa na presença da disciplina: não chega a consultar o escopo dela.
        verify(escopoProfessor, never()).disciplinaIdsDoProfessor(any());
    }

    @Test
    @DisplayName("professor sem turmas não vincula nada")
    void professorSemTurmasNaoVincula() {
        autenticarProfessorComTurmas();

        assertForbidden(() -> simuladoQuestaoService.salvar(
                vinculo(simulado(SIMULADO_ID, TURMA_ID, DISC_A), questao(QUESTAO_ID, DISC_A))));

        verify(repository, never()).save(any());
    }

    // --- Coerência entre as duas pontas ------------------------------------

    @Test
    @DisplayName("questão e simulado de disciplinas diferentes são recusados")
    void professorNaoMisturaDisciplinas() {
        // O professor leciona as duas disciplinas: o recorte passa, a coerência não.
        autenticarProfessorComTurmas(TURMA_ID);
        professorTemDisciplinas(DISC_A, DISC_B);

        assertThatThrownBy(() -> simuladoQuestaoService.salvar(
                vinculo(simulado(SIMULADO_ID, TURMA_ID, DISC_A), questao(QUESTAO_ID, DISC_B))))
                .isInstanceOf(RequisicaoInvalidaException.class);

        verify(repository, never()).save(any());
    }

    // --- Regras antigas preservadas ----------------------------------------

    @Test
    @DisplayName("questão de IA sem conteúdo vinculado continua recusada")
    void questaoIaSemConteudoContinuaRecusada() {
        autenticarProfessorComTurmas(TURMA_ID);
        professorTemDisciplinas(DISC_A);
        given(questaoConteudoRepository.existsByQuestaoId(QUESTAO_ID)).willReturn(false);

        SimuladoQuestao enviado = vinculo(
                simulado(SIMULADO_ID, TURMA_ID, DISC_A), questao(QUESTAO_ID, DISC_A, OrigemQuestao.IA));

        assertThatThrownBy(() -> simuladoQuestaoService.salvar(enviado))
                .isInstanceOf(RegraNegocioException.class);

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("questão de IA com conteúdo vinculado continua entrando")
    void questaoIaComConteudoContinuaEntrando() {
        autenticarProfessorComTurmas(TURMA_ID);
        professorTemDisciplinas(DISC_A);
        given(questaoConteudoRepository.existsByQuestaoId(QUESTAO_ID)).willReturn(true);
        given(repository.save(any())).willAnswer(chamada -> chamada.getArgument(0));

        SimuladoQuestao salvo = simuladoQuestaoService.salvar(vinculo(
                simulado(SIMULADO_ID, TURMA_ID, DISC_A), questao(QUESTAO_ID, DISC_A, OrigemQuestao.IA)));

        assertThat(salvo.getStatus()).isEqualTo(StatusSimuladoQuestao.ATIVA);
    }

    @Test
    @DisplayName("limite de 10 questões ativas continua valendo")
    void limiteDeDezQuestoesContinua() {
        autenticarProfessorComTurmas(TURMA_ID);
        professorTemDisciplinas(DISC_A);
        given(repository.countBySimuladoIdAndStatus(SIMULADO_ID, StatusSimuladoQuestao.ATIVA)).willReturn(10L);

        assertThatThrownBy(() -> simuladoQuestaoService.salvar(
                vinculo(simulado(SIMULADO_ID, TURMA_ID, DISC_A), questao(QUESTAO_ID, DISC_A))))
                .isInstanceOf(RegraNegocioException.class);

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("simulado com 9 questões ainda recebe a décima")
    void nonaQuestaoAindaEntra() {
        autenticarProfessorComTurmas(TURMA_ID);
        professorTemDisciplinas(DISC_A);
        given(repository.countBySimuladoIdAndStatus(SIMULADO_ID, StatusSimuladoQuestao.ATIVA)).willReturn(9L);
        given(repository.save(any())).willAnswer(chamada -> chamada.getArgument(0));

        assertThat(simuladoQuestaoService.salvar(
                vinculo(simulado(SIMULADO_ID, TURMA_ID, DISC_A), questao(QUESTAO_ID, DISC_A))).getStatus())
                .isEqualTo(StatusSimuladoQuestao.ATIVA);
    }

    // --- ADMINISTRADOR -----------------------------------------------------

    @Test
    @DisplayName("administrador vincula até em simulado órfão, sem consultar escopo")
    void administradorVinculaEmSimuladoOrfao() {
        AuthorizationTestSupport.autenticarComoAdministrador();
        given(repository.save(any())).willAnswer(chamada -> chamada.getArgument(0));

        Simulado orfao = simulado(SIMULADO_ID, null, DISC_A);
        orfao.setPlanoEnsino(plano(null));

        SimuladoQuestao salvo = simuladoQuestaoService.salvar(vinculo(orfao, questao(QUESTAO_ID, DISC_A)));

        assertThat(salvo.getStatus()).isEqualTo(StatusSimuladoQuestao.ATIVA);
        verifyNoInteractions(escopoProfessor);
    }

    @Test
    @DisplayName("administrador também respeita a mesma disciplina")
    void administradorRespeitaMesmaDisciplina() {
        AuthorizationTestSupport.autenticarComoAdministrador();

        assertThatThrownBy(() -> simuladoQuestaoService.salvar(
                vinculo(simulado(SIMULADO_ID, TURMA_ID, DISC_A), questao(QUESTAO_ID, DISC_B))))
                .isInstanceOf(RequisicaoInvalidaException.class);

        verifyNoInteractions(escopoProfessor);
    }

    @Test
    @DisplayName("administrador não vincula questão sem disciplina")
    void administradorNaoVinculaQuestaoSemDisciplina() {
        AuthorizationTestSupport.autenticarComoAdministrador();

        assertThatThrownBy(() -> simuladoQuestaoService.salvar(
                vinculo(simulado(SIMULADO_ID, TURMA_ID, DISC_A), questao(QUESTAO_ID, null))))
                .isInstanceOf(RequisicaoInvalidaException.class);
    }

    @Test
    @DisplayName("questão continua obrigatória (400) e simulado também")
    void pontasObrigatoriasContinuamValendo() {
        AuthorizationTestSupport.autenticarComoAdministrador();

        assertThatThrownBy(() -> simuladoQuestaoService.salvar(
                vinculo(simulado(SIMULADO_ID, TURMA_ID, DISC_A), null)))
                .isInstanceOf(RequisicaoInvalidaException.class);
        assertThatThrownBy(() -> simuladoQuestaoService.salvar(vinculo(null, questao(QUESTAO_ID, DISC_A))))
                .isInstanceOf(RequisicaoInvalidaException.class);

        verify(repository, never()).save(any());
    }

    // --- Perfis sem permissão ----------------------------------------------

    @Test
    @DisplayName("aluno recebe 403 sem nenhuma consulta ao banco")
    void alunoNaoVincula() {
        AuthorizationTestSupport.autenticarComoAluno(ALUNO_ID);

        assertForbidden(() -> simuladoQuestaoService.salvar(
                vinculo(simulado(SIMULADO_ID, TURMA_ID, DISC_A), questao(QUESTAO_ID, DISC_A))));

        verifyNoInteractions(repository, questaoConteudoRepository, escopoProfessor, simuladoService);
    }

    @Test
    @DisplayName("sem autenticação: 403 sem nenhuma consulta ao banco")
    void semAutenticacaoNaoVincula() {
        AuthorizationTestSupport.limparContexto();

        assertForbidden(() -> simuladoQuestaoService.salvar(
                vinculo(simulado(SIMULADO_ID, TURMA_ID, DISC_A), questao(QUESTAO_ID, DISC_A))));

        verifyNoInteractions(repository, questaoConteudoRepository, escopoProfessor, simuladoService);
    }

    // --- helpers -----------------------------------------------------------

    private void autenticarProfessorComTurmas(Long... turmaIds) {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(escopoProfessor.turmaIdsDoProfessor(PROFESSOR_ID)).willReturn(Set.of(turmaIds));
    }

    private void professorTemDisciplinas(Long... disciplinaIds) {
        given(escopoProfessor.disciplinaIdsDoProfessor(PROFESSOR_ID)).willReturn(Set.of(disciplinaIds));
    }

    private void professorTemVinculosDeTurmaDisciplina(Long... vinculoIds) {
        given(escopoProfessor.turmaDisciplinaIdsDoProfessor(PROFESSOR_ID)).willReturn(Set.of(vinculoIds));
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

    private void assertNegocio(Runnable acao, String trecho) {
        try {
            acao.run();
        } catch (RegraNegocioException esperada) {
            assertThat(esperada.getMessage()).contains(trecho);
            return;
        }
        fail("esperava RegraNegocioException (409)");
    }

    private static SimuladoQuestao vinculo(Simulado simulado, Questao questao) {
        SimuladoQuestao vinculo = new SimuladoQuestao();
        vinculo.setSimulado(simulado);
        vinculo.setQuestao(questao);
        vinculo.setOrdem(1);
        return vinculo;
    }

    private static Simulado simulado(Long id, Long turmaId, Long disciplinaId) {
        Simulado simulado = new Simulado();
        simulado.setId(id);
        simulado.setTitulo("Simulado " + id);
        // Só simulado em RASCUNHO aceita vínculo novo (regra de status).
        simulado.setStatus(StatusSimulado.RASCUNHO);
        if (turmaId != null) {
            Turma turma = new Turma();
            turma.setId(turmaId);
            simulado.setTurma(turma);
        }
        simulado.setDisciplina(disciplina(disciplinaId));
        return simulado;
    }

    /** Plano de ensino ligado a um vínculo turma/disciplina; {@code vinculoId} nulo é o plano genérico. */
    private static PlanoEnsino plano(Long vinculoId) {
        PlanoEnsino plano = new PlanoEnsino();
        plano.setId(PLANO_ID);
        if (vinculoId != null) {
            TurmaDisciplina vinculo = new TurmaDisciplina();
            vinculo.setId(vinculoId);
            plano.setTurmaDisciplina(vinculo);
        }
        return plano;
    }

    private static Questao questao(Long id, Long disciplinaId) {
        return questao(id, disciplinaId, OrigemQuestao.PROFESSOR);
    }

    private static Questao questao(Long id, Long disciplinaId, OrigemQuestao origem) {
        Questao questao = new Questao();
        questao.setId(id);
        questao.setEnunciado("Enunciado " + id);
        questao.setOrigem(origem);
        questao.setDisciplina(disciplina(disciplinaId));
        return questao;
    }

    private static Disciplina disciplina(Long id) {
        if (id == null) {
            return null;
        }
        Disciplina disciplina = new Disciplina();
        disciplina.setId(id);
        return disciplina;
    }
}
