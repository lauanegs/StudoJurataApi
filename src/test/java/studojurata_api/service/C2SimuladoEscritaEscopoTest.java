package studojurata_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.LocalDateTime;
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

import studojurata_api.dto.LancarSimuladoRequest;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.exception.RegraNegocioException;
import studojurata_api.exception.RequisicaoInvalidaException;
import studojurata_api.model.Aluno;
import studojurata_api.model.AlunoTurma;
import studojurata_api.model.Disciplina;
import studojurata_api.model.PlanoEnsino;
import studojurata_api.model.Questao;
import studojurata_api.model.Simulado;
import studojurata_api.model.SimuladoQuestao;
import studojurata_api.model.Turma;
import studojurata_api.model.TurmaDisciplina;
import studojurata_api.model.enums.StatusQuestao;
import studojurata_api.model.enums.StatusAtivoInativo;
import studojurata_api.model.enums.StatusSimulado;
import studojurata_api.model.enums.StatusSimuladoQuestao;
import studojurata_api.model.enums.TipoDestinacaoSimulado;
import studojurata_api.repository.AlunoRepository;
import studojurata_api.repository.SimuladoAlunoRepository;
import studojurata_api.repository.SimuladoQuestaoRepository;
import studojurata_api.repository.SimuladoRepository;
import studojurata_api.security.EscopoProfessor;
import studojurata_api.security.SimuladoAccessGuard;
import studojurata_api.security.UsuarioAutenticado;
import studojurata_api.support.AuthorizationTestSupport;

/**
 * C2.7d (passo 5) — escrita de simulado: criação, edição, lançamento,
 * encerramento e disponibilidade.
 *
 * <p>A regra de escopo é compartilhada com o vínculo simulado × questão
 * ({@link SimuladoAccessGuard}), então os cenários de turma/disciplina/plano
 * valem para as duas pontas. Cada teste cobre também a ausência de efeito
 * colateral quando a escrita é recusada.
 */
@ExtendWith(MockitoExtension.class)
class C2SimuladoEscritaEscopoTest {

    private static final long ALUNO_ID = 7L;
    private static final long OUTRO_ALUNO_ID = 8L;
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

    @Mock private SimuladoRepository repository;
    @Mock private SimuladoQuestaoRepository simuladoQuestaoRepository;
    @Mock private SimuladoAlunoRepository simuladoAlunoRepository;
    @Mock private AlunoTurmaService alunoTurmaService;
    @Mock private AlunoRepository alunoRepository;
    @Mock private EscopoProfessor escopoProfessor;

    private SimuladoService simuladoService;

    @BeforeEach
    void setUp() {
        UsuarioAutenticado usuarioAutenticado = new UsuarioAutenticado();
        SimuladoAccessGuard simuladoAccessGuard = new SimuladoAccessGuard(usuarioAutenticado, escopoProfessor);
        simuladoService = new SimuladoService(repository, simuladoQuestaoRepository, simuladoAlunoRepository,
                mock(studojurata_api.ia.repository.SimuladoGeradoIARepository.class), alunoTurmaService,
                alunoRepository, usuarioAutenticado, escopoProfessor, simuladoAccessGuard);
    }

    @AfterEach
    void limparContexto() {
        AuthorizationTestSupport.limparContexto();
    }

    // --- POST /simulados ----------------------------------------------------

    @Test
    @DisplayName("professor cria simulado na turma e disciplina que leciona")
    void professorCriaSimuladoProprio() {
        autenticarProfessorComTurmas(TURMA_ID);
        professorTemDisciplinas(DISC_A);
        professorTemVinculosDeTurmaDisciplina(VINCULO_DO_PROFESSOR);
        given(repository.save(any())).willAnswer(chamada -> chamada.getArgument(0));

        Simulado enviado = simulado(null, TURMA_ID, DISC_A);
        enviado.setPlanoEnsino(plano(VINCULO_DO_PROFESSOR, TURMA_ID, DISC_A));

        Simulado salvo = simuladoService.salvar(enviado);

        assertThat(salvo.getTurma().getId()).isEqualTo(TURMA_ID);
        assertThat(salvo.getStatus()).isEqualTo(StatusSimulado.RASCUNHO);
        verify(repository).save(any());
    }

    @Test
    @DisplayName("professor cria simulado com plano de um vínculo seu")
    void professorCriaSimuladoComPlanoAcessivel() {
        autenticarProfessorComTurmas(TURMA_ID);
        professorTemDisciplinas(DISC_A);
        professorTemVinculosDeTurmaDisciplina(VINCULO_DO_PROFESSOR);
        given(repository.save(any())).willAnswer(chamada -> chamada.getArgument(0));

        Simulado enviado = simulado(null, TURMA_ID, DISC_A);
        enviado.setPlanoEnsino(plano(VINCULO_DO_PROFESSOR));

        assertThat(simuladoService.salvar(enviado).getPlanoEnsino().getId()).isEqualTo(PLANO_ID);
        verify(escopoProfessor).turmaDisciplinaIdsDoProfessor(PROFESSOR_ID);
    }

    @Test
    @DisplayName("professor não cria simulado em turma alheia")
    void professorNaoCriaSimuladoEmTurmaAlheia() {
        autenticarProfessorComTurmas(TURMA_ID);

        assertForbidden(() -> simuladoService.salvar(simulado(null, OUTRA_TURMA_ID, DISC_A)));

        verify(repository, never()).save(any());
        verify(escopoProfessor, never()).disciplinaIdsDoProfessor(any());
    }

    @Test
    @DisplayName("professor não cria simulado em disciplina alheia")
    void professorNaoCriaSimuladoEmDisciplinaAlheia() {
        autenticarProfessorComTurmas(TURMA_ID);
        professorTemDisciplinas(DISC_A);

        assertForbidden(() -> simuladoService.salvar(simulado(null, TURMA_ID, DISC_B)));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("professor não cria simulado sem turma")
    void professorNaoCriaSimuladoSemTurma() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);

        assertForbidden(() -> simuladoService.salvar(simulado(null, null, DISC_A)));

        verify(repository, never()).save(any());
        // Sem turma não há escopo verificável: recusa antes de consultar.
        verifyNoInteractions(escopoProfessor);
    }

    @Test
    @DisplayName("professor não cria simulado sem disciplina")
    void professorNaoCriaSimuladoSemDisciplina() {
        autenticarProfessorComTurmas(TURMA_ID);

        assertForbidden(() -> simuladoService.salvar(simulado(null, TURMA_ID, null)));

        verify(repository, never()).save(any());
        verify(escopoProfessor, never()).disciplinaIdsDoProfessor(any());
    }

    @Test
    @DisplayName("professor não cria simulado com plano fora do escopo")
    void professorNaoCriaSimuladoComPlanoAlheio() {
        autenticarProfessorComTurmas(TURMA_ID);
        professorTemDisciplinas(DISC_A);
        professorTemVinculosDeTurmaDisciplina(VINCULO_DO_PROFESSOR);

        Simulado enviado = simulado(null, TURMA_ID, DISC_A);
        enviado.setPlanoEnsino(plano(VINCULO_DE_OUTRO));

        assertForbidden(() -> simuladoService.salvar(enviado));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("professor não cria simulado com plano genérico")
    void professorNaoCriaSimuladoComPlanoGenerico() {
        autenticarProfessorComTurmas(TURMA_ID);
        professorTemDisciplinas(DISC_A);

        Simulado enviado = simulado(null, TURMA_ID, DISC_A);
        enviado.setPlanoEnsino(plano(null));

        assertForbidden(() -> simuladoService.salvar(enviado));

        verify(repository, never()).save(any());
        verify(escopoProfessor, never()).turmaDisciplinaIdsDoProfessor(any());
    }

    @Test
    @DisplayName("administrador cria simulado órfão, sem consultar escopo")
    void administradorCriaSimuladoOrfao() {
        AuthorizationTestSupport.autenticarComoAdministrador();
        given(repository.save(any())).willAnswer(chamada -> chamada.getArgument(0));

        Simulado enviado = simulado(null, null, null);
        enviado.setPlanoEnsino(plano(null));

        assertThat(simuladoService.salvar(enviado).getStatus()).isEqualTo(StatusSimulado.RASCUNHO);
        verifyNoInteractions(escopoProfessor);
    }

    // --- PUT /simulados/{id} ------------------------------------------------

    @Test
    @DisplayName("professor edita o próprio rascunho e o escopo é resolvido uma única vez")
    void professorAtualizaSimuladoProprio() {
        autenticarProfessorComTurmas(TURMA_ID);
        professorTemDisciplinas(DISC_A);
        professorTemVinculosDeTurmaDisciplina(VINCULO_DO_PROFESSOR);
        given(repository.findById(SIMULADO_ID)).willReturn(Optional.of(simulado(SIMULADO_ID, TURMA_ID, DISC_A)));
        given(repository.save(any())).willAnswer(chamada -> chamada.getArgument(0));

        Simulado enviado = simulado(null, TURMA_ID, DISC_A);
        enviado.setTitulo("Título revisado");
        enviado.setPlanoEnsino(plano(VINCULO_DO_PROFESSOR, TURMA_ID, DISC_A));
        enviado.setStatus(StatusSimulado.PUBLICADO);

        Simulado salvo = simuladoService.atualizar(SIMULADO_ID, enviado);

        assertThat(salvo.getId()).isEqualTo(SIMULADO_ID);
        assertThat(salvo.getTitulo()).isEqualTo("Título revisado");
        // O status continua vindo do registro atual.
        assertThat(salvo.getStatus()).isEqualTo(StatusSimulado.RASCUNHO);

        // Simulado atual e novos valores na mesma resolução de escopo.
        verify(escopoProfessor, times(1)).turmaIdsDoProfessor(PROFESSOR_ID);
        verify(escopoProfessor, times(1)).disciplinaIdsDoProfessor(PROFESSOR_ID);
    }

    @Test
    @DisplayName("professor não edita rascunho de outro professor")
    void professorNaoAtualizaSimuladoAlheio() {
        autenticarProfessorComTurmas(TURMA_ID);
        given(repository.findById(SIMULADO_ID))
                .willReturn(Optional.of(simulado(SIMULADO_ID, OUTRA_TURMA_ID, DISC_A)));

        assertForbidden(() -> simuladoService.atualizar(SIMULADO_ID, simulado(null, OUTRA_TURMA_ID, DISC_A)));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("professor não move simulado para turma alheia")
    void professorNaoMoveSimuladoParaTurmaAlheia() {
        autenticarProfessorComTurmas(TURMA_ID);
        given(repository.findById(SIMULADO_ID)).willReturn(Optional.of(simulado(SIMULADO_ID, TURMA_ID, DISC_A)));

        assertForbidden(() -> simuladoService.atualizar(SIMULADO_ID, simulado(null, OUTRA_TURMA_ID, DISC_A)));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("professor não move simulado para disciplina alheia")
    void professorNaoMoveSimuladoParaDisciplinaAlheia() {
        autenticarProfessorComTurmas(TURMA_ID);
        professorTemDisciplinas(DISC_A);
        given(repository.findById(SIMULADO_ID)).willReturn(Optional.of(simulado(SIMULADO_ID, TURMA_ID, DISC_A)));

        assertForbidden(() -> simuladoService.atualizar(SIMULADO_ID, simulado(null, TURMA_ID, DISC_B)));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("professor não desvincula o simulado da turma pela edição")
    void professorNaoMoveSimuladoParaSemTurma() {
        autenticarProfessorComTurmas(TURMA_ID);
        given(repository.findById(SIMULADO_ID)).willReturn(Optional.of(simulado(SIMULADO_ID, TURMA_ID, DISC_A)));

        assertForbidden(() -> simuladoService.atualizar(SIMULADO_ID, simulado(null, null, DISC_A)));

        verify(repository, never()).save(any());
        // Recusa na presença da turma dos novos valores: não consulta o escopo de disciplinas.
        verify(escopoProfessor, never()).disciplinaIdsDoProfessor(any());
    }

    @Test
    @DisplayName("edição de simulado publicado continua recusada (regra preservada)")
    void professorNaoEditaSimuladoPublicado() {
        autenticarProfessorComTurmas(TURMA_ID);
        professorTemDisciplinas(DISC_A);
        Simulado publicado = simulado(SIMULADO_ID, TURMA_ID, DISC_A);
        publicado.setStatus(StatusSimulado.PUBLICADO);
        given(repository.findById(SIMULADO_ID)).willReturn(Optional.of(publicado));

        assertThatThrownBy(() -> simuladoService.atualizar(SIMULADO_ID, simulado(null, TURMA_ID, DISC_A)))
                .isInstanceOf(RegraNegocioException.class);

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("simulado inexistente responde 404 sem consultar escopo")
    void simuladoInexistenteResponde404() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(repository.findById(SIMULADO_ID)).willReturn(Optional.empty());

        try {
            simuladoService.atualizar(SIMULADO_ID, simulado(null, TURMA_ID, DISC_A));
            fail("esperava RecursoNaoEncontradoException");
        } catch (RecursoNaoEncontradoException esperado) {
            verifyNoInteractions(escopoProfessor);
            verify(repository, never()).save(any());
        }
    }

    @Test
    @DisplayName("administrador edita simulado órfão")
    void administradorAtualizaSimuladoOrfao() {
        AuthorizationTestSupport.autenticarComoAdministrador();
        given(repository.findById(SIMULADO_ID)).willReturn(Optional.of(simulado(SIMULADO_ID, null, null)));
        given(repository.save(any())).willAnswer(chamada -> chamada.getArgument(0));

        assertThat(simuladoService.atualizar(SIMULADO_ID, simulado(null, null, null)).getId())
                .isEqualTo(SIMULADO_ID);
        verifyNoInteractions(escopoProfessor);
    }

    // --- Plano de ensino obrigatório e compatível (I4) -----------------------

    @Test
    @DisplayName("professor não cria simulado sem plano de ensino")
    void professorNaoCriaSimuladoSemPlano() {
        autenticarProfessorComTurmas(TURMA_ID);
        professorTemDisciplinas(DISC_A);

        assertInvalida(() -> simuladoService.salvar(simulado(null, TURMA_ID, DISC_A)), "Plano de ensino");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("professor não cria simulado com plano de outra turma")
    void professorNaoCriaSimuladoComPlanoDeOutraTurma() {
        autenticarProfessorComTurmas(TURMA_ID);
        professorTemDisciplinas(DISC_A);
        professorTemVinculosDeTurmaDisciplina(VINCULO_DO_PROFESSOR);

        Simulado enviado = simulado(null, TURMA_ID, DISC_A);
        enviado.setPlanoEnsino(plano(VINCULO_DO_PROFESSOR, OUTRA_TURMA_ID, DISC_A));

        assertNegocio(() -> simuladoService.salvar(enviado), "não pertence à turma");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("professor não cria simulado com plano de outra disciplina")
    void professorNaoCriaSimuladoComPlanoDeOutraDisciplina() {
        autenticarProfessorComTurmas(TURMA_ID);
        professorTemDisciplinas(DISC_A);
        professorTemVinculosDeTurmaDisciplina(VINCULO_DO_PROFESSOR);

        Simulado enviado = simulado(null, TURMA_ID, DISC_A);
        enviado.setPlanoEnsino(plano(VINCULO_DO_PROFESSOR, TURMA_ID, DISC_B));

        assertNegocio(() -> simuladoService.salvar(enviado), "não pertence à turma");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("professor não torna o plano incompatível pela edição")
    void professorNaoEditaTornandoPlanoIncompativel() {
        autenticarProfessorComTurmas(TURMA_ID);
        professorTemDisciplinas(DISC_A);
        professorTemVinculosDeTurmaDisciplina(VINCULO_DO_PROFESSOR);
        given(repository.findById(SIMULADO_ID)).willReturn(Optional.of(simulado(SIMULADO_ID, TURMA_ID, DISC_A)));

        Simulado enviado = simulado(null, TURMA_ID, DISC_A);
        enviado.setPlanoEnsino(plano(VINCULO_DO_PROFESSOR, OUTRA_TURMA_ID, DISC_A));

        assertNegocio(() -> simuladoService.atualizar(SIMULADO_ID, enviado), "não pertence à turma");

        verify(repository, never()).save(any());
    }

    // --- Disciplina: precisa ser ofertada na turma (M7) ---------------------

    @Test
    @DisplayName("professor não cria simulado com disciplina ofertada só em outra turma")
    void professorNaoCriaSimuladoComDisciplinaDeOutraTurma() {
        autenticarProfessorComTurmas(TURMA_ID);
        professorTemDisciplinas(DISC_A, DISC_B);
        professorTemVinculosDeTurmaDisciplina(VINCULO_DO_PROFESSOR);

        // Turma da oferta real da DISC_B é OUTRA_TURMA: não existe plano do par
        // (TURMA_ID, DISC_B) para acompanhar este simulado.
        Simulado enviado = simulado(null, TURMA_ID, DISC_B);
        enviado.setPlanoEnsino(plano(VINCULO_DO_PROFESSOR, OUTRA_TURMA_ID, DISC_B));

        assertNegocio(() -> simuladoService.salvar(enviado), "não pertence à turma");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("professor não cria simulado com disciplina inativada")
    void professorNaoCriaSimuladoComDisciplinaInativada() {
        autenticarProfessorComTurmas(TURMA_ID);
        professorTemDisciplinas(DISC_A);
        professorTemVinculosDeTurmaDisciplina(VINCULO_DO_PROFESSOR);

        Simulado enviado = simuladoComPlano();
        enviado.setDisciplina(disciplina(StatusAtivoInativo.INATIVO));

        assertNegocio(() -> simuladoService.salvar(enviado), "inativa");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("professor não edita simulado para disciplina inativada")
    void professorNaoEditaParaDisciplinaInativada() {
        autenticarProfessorComTurmas(TURMA_ID);
        professorTemDisciplinas(DISC_A);
        professorTemVinculosDeTurmaDisciplina(VINCULO_DO_PROFESSOR);
        given(repository.findById(SIMULADO_ID)).willReturn(Optional.of(simulado(SIMULADO_ID, TURMA_ID, DISC_A)));

        Simulado enviado = simuladoComPlano();
        enviado.setDisciplina(disciplina(StatusAtivoInativo.INATIVO));

        assertNegocio(() -> simuladoService.atualizar(SIMULADO_ID, enviado), "inativa");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("administrador mantém a liberdade com disciplina inativada")
    void administradorCriaSimuladoComDisciplinaInativada() {
        AuthorizationTestSupport.autenticarComoAdministrador();
        given(repository.save(any())).willAnswer(chamada -> chamada.getArgument(0));

        Simulado enviado = simulado(null, TURMA_ID, null);
        enviado.setDisciplina(disciplina(StatusAtivoInativo.INATIVO));

        assertThat(simuladoService.salvar(enviado).getStatus()).isEqualTo(StatusSimulado.RASCUNHO);
        verifyNoInteractions(escopoProfessor);
    }

    private static Disciplina disciplina(StatusAtivoInativo status) {
        Disciplina disciplina = new Disciplina();
        disciplina.setId(DISC_A);
        disciplina.setTitulo("Disciplina " + DISC_A);
        disciplina.setStatus(status);
        return disciplina;
    }

    @Test
    @DisplayName("professor cria simulado com plano da própria turma e disciplina")
    void professorCriaSimuladoComPlanoCompativel() {
        autenticarProfessorComTurmas(TURMA_ID);
        professorTemDisciplinas(DISC_A);
        professorTemVinculosDeTurmaDisciplina(VINCULO_DO_PROFESSOR);
        given(repository.save(any())).willAnswer(chamada -> chamada.getArgument(0));

        Simulado enviado = simulado(null, TURMA_ID, DISC_A);
        enviado.setPlanoEnsino(plano(VINCULO_DO_PROFESSOR, TURMA_ID, DISC_A));

        assertThat(simuladoService.salvar(enviado).getStatus()).isEqualTo(StatusSimulado.RASCUNHO);

        verify(repository).save(any());
    }

    // --- POST /simulados/{id}/lancar ---------------------------------------

    @Test
    @DisplayName("professor lança o próprio rascunho (regras de questões preservadas)")
    void professorLancaSimuladoProprio() {
        autenticarProfessorComTurmas(TURMA_ID);
        professorTemDisciplinas(DISC_A);
        Simulado rascunho = simulado(SIMULADO_ID, TURMA_ID, DISC_A);
        rascunho.setTipoDestinacao(TipoDestinacaoSimulado.ESPECIFICO);
        given(repository.findById(SIMULADO_ID)).willReturn(Optional.of(rascunho));
        given(simuladoQuestaoRepository.findBySimuladoIdAndStatusOrderByOrdem(SIMULADO_ID, StatusSimuladoQuestao.ATIVA))
                .willReturn(List.of(vinculo(questao(QUESTAO_ID, StatusQuestao.APROVADA))));
        given(alunoRepository.findById(ALUNO_ID)).willReturn(Optional.of(aluno(ALUNO_ID)));
        // I3: aluno precisa ter matrícula na turma do simulado.
        given(alunoTurmaService.historicoPorTurma(TURMA_ID)).willReturn(List.of(matriculaDoAluno(ALUNO_ID)));
        given(repository.save(any())).willAnswer(chamada -> chamada.getArgument(0));

        Simulado lancado = simuladoService.lancar(SIMULADO_ID, requestComAluno());

        assertThat(lancado.getStatus()).isEqualTo(StatusSimulado.PUBLICADO);
        verify(simuladoAlunoRepository).save(any());
    }

    @Test
    @DisplayName("professor não lança simulado de outro professor, sem efeito colateral")
    void professorNaoLancaSimuladoAlheio() {
        autenticarProfessorComTurmas(TURMA_ID);
        given(repository.findById(SIMULADO_ID))
                .willReturn(Optional.of(simulado(SIMULADO_ID, OUTRA_TURMA_ID, DISC_A)));

        assertForbidden(() -> simuladoService.lancar(SIMULADO_ID, requestComAluno()));

        verify(repository, never()).save(any());
        verify(simuladoAlunoRepository, never()).save(any());
        verifyNoInteractions(simuladoQuestaoRepository);
    }

    @Test
    @DisplayName("professor não lança simulado órfão")
    void professorNaoLancaSimuladoOrfao() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(repository.findById(SIMULADO_ID)).willReturn(Optional.of(simulado(SIMULADO_ID, null, DISC_A)));

        assertForbidden(() -> simuladoService.lancar(SIMULADO_ID, requestComAluno()));

        verify(simuladoAlunoRepository, never()).save(any());
        verifyNoInteractions(escopoProfessor);
    }

    // --- Lançamento específico: alunos no escopo da turma (I3) --------------

    @Test
    @DisplayName("professor lança específico para aluno de outra turma: recusado sem efeito")
    void professorNaoLancaParaAlunoDeOutraTurma() {
        autenticarProfessorComTurmas(TURMA_ID);
        professorTemDisciplinas(DISC_A);
        dadoRascunhoEspecificoComQuestao();
        given(alunoRepository.findById(OUTRO_ALUNO_ID)).willReturn(Optional.of(aluno(OUTRO_ALUNO_ID)));
        // A turma do simulado só tem ALUNO_ID matriculado.
        given(alunoTurmaService.historicoPorTurma(TURMA_ID)).willReturn(List.of(matriculaDoAluno(ALUNO_ID)));

        LancarSimuladoRequest pedido = new LancarSimuladoRequest();
        pedido.setAlunoIds(List.of(OUTRO_ALUNO_ID));

        assertNegocio(() -> simuladoService.lancar(SIMULADO_ID, pedido), "não têm matrícula");

        verify(simuladoAlunoRepository, never()).save(any());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("professor lança específico para aluno sem matrícula: recusado sem efeito")
    void professorNaoLancaParaAlunoSemMatricula() {
        autenticarProfessorComTurmas(TURMA_ID);
        professorTemDisciplinas(DISC_A);
        dadoRascunhoEspecificoComQuestao();
        given(alunoRepository.findById(ALUNO_ID)).willReturn(Optional.of(aluno(ALUNO_ID)));
        given(alunoTurmaService.historicoPorTurma(TURMA_ID)).willReturn(List.of());

        assertNegocio(() -> simuladoService.lancar(SIMULADO_ID, requestComAluno()), "não têm matrícula");

        verify(simuladoAlunoRepository, never()).save(any());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("lista parcialmente inválida não cria tentativa para o aluno válido")
    void listaParcialmenteInvalidaNaoTemEfeitoParcial() {
        autenticarProfessorComTurmas(TURMA_ID);
        professorTemDisciplinas(DISC_A);
        dadoRascunhoEspecificoComQuestao();
        given(alunoRepository.findById(ALUNO_ID)).willReturn(Optional.of(aluno(ALUNO_ID)));
        given(alunoRepository.findById(OUTRO_ALUNO_ID)).willReturn(Optional.of(aluno(OUTRO_ALUNO_ID)));
        given(alunoTurmaService.historicoPorTurma(TURMA_ID)).willReturn(List.of(matriculaDoAluno(ALUNO_ID)));

        LancarSimuladoRequest pedido = new LancarSimuladoRequest();
        pedido.setAlunoIds(List.of(ALUNO_ID, OUTRO_ALUNO_ID));

        assertNegocio(() -> simuladoService.lancar(SIMULADO_ID, pedido), "não têm matrícula");

        verify(simuladoAlunoRepository, never()).save(any());
    }

    @Test
    @DisplayName("administrador lança específico para aluno sem matrícula (fluxo administrativo preservado)")
    void administradorLancaParaAlunoSemMatricula() {
        AuthorizationTestSupport.autenticarComoAdministrador();
        dadoRascunhoEspecificoComQuestao();
        given(alunoRepository.findById(ALUNO_ID)).willReturn(Optional.of(aluno(ALUNO_ID)));
        given(repository.save(any())).willAnswer(chamada -> chamada.getArgument(0));

        Simulado lancado = simuladoService.lancar(SIMULADO_ID, requestComAluno());

        assertThat(lancado.getStatus()).isEqualTo(StatusSimulado.PUBLICADO);
        verify(simuladoAlunoRepository).save(any());
        verifyNoInteractions(alunoTurmaService);
    }

    private void dadoRascunhoEspecificoComQuestao() {
        Simulado rascunho = simulado(SIMULADO_ID, TURMA_ID, DISC_A);
        rascunho.setTipoDestinacao(TipoDestinacaoSimulado.ESPECIFICO);
        given(repository.findById(SIMULADO_ID)).willReturn(Optional.of(rascunho));
        given(simuladoQuestaoRepository.findBySimuladoIdAndStatusOrderByOrdem(SIMULADO_ID, StatusSimuladoQuestao.ATIVA))
                .willReturn(List.of(vinculo(questao(QUESTAO_ID, StatusQuestao.APROVADA))));
    }

    // --- Datas, duração e nota máxima (I6) ----------------------------------

    @Test
    @DisplayName("criação sem data de início é recusada")
    void criacaoSemDataInicioEhRecusada() {
        autenticarProfessorComTurmas(TURMA_ID);
        professorTemDisciplinas(DISC_A);
        professorTemVinculosDeTurmaDisciplina(VINCULO_DO_PROFESSOR);

        Simulado enviado = simuladoComPlano();
        enviado.setDataInicio(null);

        assertInvalida(() -> simuladoService.salvar(enviado), "Data de início");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("criação com data final anterior ou igual à inicial é recusada")
    void criacaoComPeriodoInvalidoEhRecusada() {
        autenticarProfessorComTurmas(TURMA_ID);
        professorTemDisciplinas(DISC_A);
        professorTemVinculosDeTurmaDisciplina(VINCULO_DO_PROFESSOR);

        Simulado anterior = simuladoComPlano();
        anterior.setDataFim(anterior.getDataInicio().minusMinutes(1));
        assertInvalida(() -> simuladoService.salvar(anterior), "posterior");

        Simulado igual = simuladoComPlano();
        igual.setDataFim(igual.getDataInicio());
        assertInvalida(() -> simuladoService.salvar(igual), "posterior");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("criação com tempo limite ou nota máxima não positivos é recusada")
    void criacaoComTempoOuNotaInvalidosEhRecusada() {
        autenticarProfessorComTurmas(TURMA_ID);
        professorTemDisciplinas(DISC_A);
        professorTemVinculosDeTurmaDisciplina(VINCULO_DO_PROFESSOR);

        Simulado tempoZero = simuladoComPlano();
        tempoZero.setTempoLimite(0);
        assertInvalida(() -> simuladoService.salvar(tempoZero), "tempo limite");

        Simulado tempoNegativo = simuladoComPlano();
        tempoNegativo.setTempoLimite(-10);
        assertInvalida(() -> simuladoService.salvar(tempoNegativo), "tempo limite");

        Simulado notaZero = simuladoComPlano();
        notaZero.setNotaMaxima(0.0);
        assertInvalida(() -> simuladoService.salvar(notaZero), "nota máxima");

        Simulado notaNegativa = simuladoComPlano();
        notaNegativa.setNotaMaxima(-1.0);
        assertInvalida(() -> simuladoService.salvar(notaNegativa), "nota máxima");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("edição com período, tempo ou nota inválidos é recusada")
    void edicaoComCamposInvalidosEhRecusada() {
        autenticarProfessorComTurmas(TURMA_ID);
        professorTemDisciplinas(DISC_A);
        professorTemVinculosDeTurmaDisciplina(VINCULO_DO_PROFESSOR);
        given(repository.findById(SIMULADO_ID)).willReturn(Optional.of(simulado(SIMULADO_ID, TURMA_ID, DISC_A)));

        Simulado semInicio = simuladoComPlano();
        semInicio.setDataInicio(null);
        assertInvalida(() -> simuladoService.atualizar(SIMULADO_ID, semInicio), "Data de início");

        Simulado periodoInvertido = simuladoComPlano();
        periodoInvertido.setDataFim(periodoInvertido.getDataInicio().minusHours(1));
        assertInvalida(() -> simuladoService.atualizar(SIMULADO_ID, periodoInvertido), "posterior");

        Simulado tempoInvalido = simuladoComPlano();
        tempoInvalido.setTempoLimite(0);
        assertInvalida(() -> simuladoService.atualizar(SIMULADO_ID, tempoInvalido), "tempo limite");

        Simulado notaInvalida = simuladoComPlano();
        notaInvalida.setNotaMaxima(0.0);
        assertInvalida(() -> simuladoService.atualizar(SIMULADO_ID, notaInvalida), "nota máxima");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("criação com período, tempo e nota válidos continua funcionando")
    void criacaoComCamposValidosContinuaFuncionando() {
        autenticarProfessorComTurmas(TURMA_ID);
        professorTemDisciplinas(DISC_A);
        professorTemVinculosDeTurmaDisciplina(VINCULO_DO_PROFESSOR);
        given(repository.save(any())).willAnswer(chamada -> chamada.getArgument(0));

        Simulado enviado = simuladoComPlano();
        enviado.setDataFim(enviado.getDataInicio().plusHours(2));
        enviado.setTempoLimite(60);
        enviado.setNotaMaxima(10.0);

        assertThat(simuladoService.salvar(enviado).getStatus()).isEqualTo(StatusSimulado.RASCUNHO);

        verify(repository).save(any());
    }

    private static Simulado simuladoComPlano() {
        Simulado enviado = simulado(null, TURMA_ID, DISC_A);
        enviado.setPlanoEnsino(plano(VINCULO_DO_PROFESSOR, TURMA_ID, DISC_A));
        return enviado;
    }

    // --- quantidadeQuestoes = contagem real de vínculos (M10) ---------------

    @Test
    @DisplayName("simulado novo nasce com quantidade zero")
    void simuladoNovoNasceComQuantidadeZero() {
        autenticarProfessorComTurmas(TURMA_ID);
        professorTemDisciplinas(DISC_A);
        professorTemVinculosDeTurmaDisciplina(VINCULO_DO_PROFESSOR);
        given(repository.save(any())).willAnswer(chamada -> chamada.getArgument(0));

        Simulado enviado = simuladoComPlano();
        enviado.setQuantidadeQuestoes(9); // valor do cliente é ignorado

        assertThat(simuladoService.salvar(enviado).getQuantidadeQuestoes()).isZero();
    }

    @Test
    @DisplayName("edição recalcula a quantidade com a contagem real do banco")
    void edicaoRecalculaQuantidadeComContagemReal() {
        autenticarProfessorComTurmas(TURMA_ID);
        professorTemDisciplinas(DISC_A);
        professorTemVinculosDeTurmaDisciplina(VINCULO_DO_PROFESSOR);
        given(repository.findById(SIMULADO_ID)).willReturn(Optional.of(simulado(SIMULADO_ID, TURMA_ID, DISC_A)));
        given(simuladoQuestaoRepository.countBySimuladoIdAndStatus(SIMULADO_ID, StatusSimuladoQuestao.ATIVA))
                .willReturn(4L);
        given(repository.save(any())).willAnswer(chamada -> chamada.getArgument(0));

        Simulado enviado = simuladoComPlano();
        enviado.setQuantidadeQuestoes(99); // valor do cliente é ignorado

        assertThat(simuladoService.atualizar(SIMULADO_ID, enviado).getQuantidadeQuestoes()).isEqualTo(4);
        verify(simuladoQuestaoRepository).countBySimuladoIdAndStatus(SIMULADO_ID, StatusSimuladoQuestao.ATIVA);
    }

    @Test
    @DisplayName("sincronização usa a contagem real ATIVA e nunca fica negativa")
    void sincronizacaoUsaContagemRealAtiva() {
        Simulado simulado = simulado(SIMULADO_ID, TURMA_ID, DISC_A);
        simulado.setQuantidadeQuestoes(7);

        given(simuladoQuestaoRepository.countBySimuladoIdAndStatus(SIMULADO_ID, StatusSimuladoQuestao.ATIVA))
                .willReturn(3L, 0L, 5L);
        given(repository.save(any())).willAnswer(chamada -> chamada.getArgument(0));

        assertThat(simuladoService.sincronizarQuantidadeDeQuestoes(simulado).getQuantidadeQuestoes()).isEqualTo(3);
        assertThat(simuladoService.sincronizarQuantidadeDeQuestoes(simulado).getQuantidadeQuestoes()).isZero();
        assertThat(simuladoService.sincronizarQuantidadeDeQuestoes(simulado).getQuantidadeQuestoes()).isEqualTo(5);

        verify(simuladoQuestaoRepository, times(3))
                .countBySimuladoIdAndStatus(SIMULADO_ID, StatusSimuladoQuestao.ATIVA);
    }

    @Test
    @DisplayName("sincronização não regrava o simulado quando a quantidade já está correta")
    void sincronizacaoNaoRegravaQuandoJaEstaCorreta() {
        Simulado simulado = simulado(SIMULADO_ID, TURMA_ID, DISC_A);
        simulado.setQuantidadeQuestoes(2);
        given(simuladoQuestaoRepository.countBySimuladoIdAndStatus(SIMULADO_ID, StatusSimuladoQuestao.ATIVA))
                .willReturn(2L);

        assertThat(simuladoService.sincronizarQuantidadeDeQuestoes(simulado).getQuantidadeQuestoes()).isEqualTo(2);

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("lançar sem questões ativas continua recusado (regra preservada)")
    void lancarSemQuestoesContinuaRecusado() {
        autenticarProfessorComTurmas(TURMA_ID);
        professorTemDisciplinas(DISC_A);
        given(repository.findById(SIMULADO_ID)).willReturn(Optional.of(simulado(SIMULADO_ID, TURMA_ID, DISC_A)));
        given(simuladoQuestaoRepository.findBySimuladoIdAndStatusOrderByOrdem(SIMULADO_ID, StatusSimuladoQuestao.ATIVA))
                .willReturn(List.of());

        assertThatThrownBy(() -> simuladoService.lancar(SIMULADO_ID, requestComAluno()))
                .isInstanceOf(RegraNegocioException.class);

        verify(repository, never()).save(any());
    }

    // --- POST /simulados/{id}/encerrar -------------------------------------

    @Test
    @DisplayName("professor encerra o próprio simulado publicado")
    void professorEncerraSimuladoProprio() {
        autenticarProfessorComTurmas(TURMA_ID);
        professorTemDisciplinas(DISC_A);
        Simulado publicado = simulado(SIMULADO_ID, TURMA_ID, DISC_A);
        publicado.setStatus(StatusSimulado.PUBLICADO);
        given(repository.findById(SIMULADO_ID)).willReturn(Optional.of(publicado));
        given(repository.save(any())).willAnswer(chamada -> chamada.getArgument(0));

        assertThat(simuladoService.encerrar(SIMULADO_ID).getStatus()).isEqualTo(StatusSimulado.ENCERRADO);
    }

    @Test
    @DisplayName("professor não encerra simulado de outro professor")
    void professorNaoEncerraSimuladoAlheio() {
        autenticarProfessorComTurmas(TURMA_ID);
        Simulado alheio = simulado(SIMULADO_ID, OUTRA_TURMA_ID, DISC_A);
        alheio.setStatus(StatusSimulado.PUBLICADO);
        given(repository.findById(SIMULADO_ID)).willReturn(Optional.of(alheio));

        assertForbidden(() -> simuladoService.encerrar(SIMULADO_ID));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("encerrar só vale para simulado publicado (regra preservada)")
    void encerrarSoValeParaPublicado() {
        autenticarProfessorComTurmas(TURMA_ID);
        professorTemDisciplinas(DISC_A);
        given(repository.findById(SIMULADO_ID)).willReturn(Optional.of(simulado(SIMULADO_ID, TURMA_ID, DISC_A)));

        assertThatThrownBy(() -> simuladoService.encerrar(SIMULADO_ID))
                .isInstanceOf(RegraNegocioException.class);

        verify(repository, never()).save(any());
    }

    // --- PATCH /simulados/{id}/disponibilidade ------------------------------

    @Test
    @DisplayName("professor estende a disponibilidade do próprio simulado")
    void professorEstendeDisponibilidadePropria() {
        autenticarProfessorComTurmas(TURMA_ID);
        professorTemDisciplinas(DISC_A);
        Simulado publicado = publicadoComInicio(SIMULADO_ID, TURMA_ID, DISC_A);
        given(repository.findById(SIMULADO_ID)).willReturn(Optional.of(publicado));
        given(repository.save(any())).willAnswer(chamada -> chamada.getArgument(0));

        LocalDateTime novaDataFim = LocalDateTime.of(2027, 1, 15, 23, 59);
        Simulado salvo = simuladoService.estenderDisponibilidade(SIMULADO_ID, novaDataFim);

        assertThat(salvo.getDataFim()).isEqualTo(novaDataFim);
    }

    @Test
    @DisplayName("professor não estende a disponibilidade de simulado alheio")
    void professorNaoEstendeDisponibilidadeAlheia() {
        autenticarProfessorComTurmas(TURMA_ID);
        given(repository.findById(SIMULADO_ID))
                .willReturn(Optional.of(publicadoComInicio(SIMULADO_ID, OUTRA_TURMA_ID, DISC_A)));

        assertForbidden(() -> simuladoService.estenderDisponibilidade(
                SIMULADO_ID, LocalDateTime.of(2027, 1, 15, 23, 59)));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("data final anterior à inicial continua recusada (regra preservada)")
    void dataFinalAnteriorContinuaRecusada() {
        autenticarProfessorComTurmas(TURMA_ID);
        professorTemDisciplinas(DISC_A);
        given(repository.findById(SIMULADO_ID)).willReturn(Optional.of(publicadoComInicio(SIMULADO_ID, TURMA_ID, DISC_A)));

        assertThatThrownBy(() -> simuladoService.estenderDisponibilidade(
                SIMULADO_ID, LocalDateTime.of(2026, 1, 1, 8, 0)))
                .isInstanceOf(RequisicaoInvalidaException.class);

        verify(repository, never()).save(any());
    }

    // --- ADMINISTRADOR e perfis sem permissão ------------------------------

    @Test
    @DisplayName("administrador encerra simulado órfão sem consultar escopo")
    void administradorEncerraSimuladoOrfao() {
        AuthorizationTestSupport.autenticarComoAdministrador();
        Simulado orfao = publicadoComInicio(SIMULADO_ID, null, null);
        given(repository.findById(SIMULADO_ID)).willReturn(Optional.of(orfao));
        given(repository.save(any())).willAnswer(chamada -> chamada.getArgument(0));

        assertThat(simuladoService.encerrar(SIMULADO_ID).getStatus()).isEqualTo(StatusSimulado.ENCERRADO);
        verifyNoInteractions(escopoProfessor);
    }

    @Test
    @DisplayName("administrador continua sujeito às regras estruturais")
    void administradorSujeitoARegraEstrutural() {
        AuthorizationTestSupport.autenticarComoAdministrador();
        given(repository.findById(SIMULADO_ID)).willReturn(Optional.of(simulado(SIMULADO_ID, null, null)));

        assertThatThrownBy(() -> simuladoService.encerrar(SIMULADO_ID))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    @DisplayName("aluno recebe 403 nas cinco operações, sem efeitos")
    void alunoNaoEscreveSimulado() {
        AuthorizationTestSupport.autenticarComoAluno(ALUNO_ID);

        assertForbidden(() -> simuladoService.salvar(simulado(null, TURMA_ID, DISC_A)));
        assertForbidden(() -> simuladoService.atualizar(SIMULADO_ID, simulado(null, TURMA_ID, DISC_A)));
        assertForbidden(() -> simuladoService.lancar(SIMULADO_ID, requestComAluno()));
        assertForbidden(() -> simuladoService.encerrar(SIMULADO_ID));
        assertForbidden(() -> simuladoService.estenderDisponibilidade(SIMULADO_ID, LocalDateTime.now().plusDays(1)));

        // O perfil é negado antes de qualquer consulta.
        verifyNoInteractions(repository, simuladoAlunoRepository, simuladoQuestaoRepository, escopoProfessor);
    }

    @Test
    @DisplayName("sem autenticação: 403 nas cinco operações, sem efeitos")
    void semAutenticacaoNaoEscreveSimulado() {
        AuthorizationTestSupport.limparContexto();

        assertForbidden(() -> simuladoService.salvar(simulado(null, TURMA_ID, DISC_A)));
        assertForbidden(() -> simuladoService.atualizar(SIMULADO_ID, simulado(null, TURMA_ID, DISC_A)));
        assertForbidden(() -> simuladoService.lancar(SIMULADO_ID, requestComAluno()));
        assertForbidden(() -> simuladoService.encerrar(SIMULADO_ID));
        assertForbidden(() -> simuladoService.estenderDisponibilidade(SIMULADO_ID, LocalDateTime.now().plusDays(1)));

        verifyNoInteractions(repository, simuladoAlunoRepository, simuladoQuestaoRepository, escopoProfessor);
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

    private void assertInvalida(Runnable acao, String trecho) {
        try {
            acao.run();
        } catch (RequisicaoInvalidaException esperada) {
            assertThat(esperada.getMessage()).contains(trecho);
            return;
        }
        fail("esperava RequisicaoInvalidaException (400)");
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

    private static LancarSimuladoRequest requestComAluno() {
        LancarSimuladoRequest request = new LancarSimuladoRequest();
        request.setAlunoIds(List.of(ALUNO_ID));
        return request;
    }

    private static Simulado simulado(Long id, Long turmaId, Long disciplinaId) {
        Simulado simulado = new Simulado();
        simulado.setId(id);
        simulado.setTitulo("Simulado " + id);
        simulado.setTipoDestinacao(TipoDestinacaoSimulado.TODOS);
        simulado.setStatus(StatusSimulado.RASCUNHO);
        // Janela obrigatória na criação e na edição (I6).
        simulado.setDataInicio(LocalDateTime.of(2026, 8, 20, 8, 0));
        if (turmaId != null) {
            Turma turma = new Turma();
            turma.setId(turmaId);
            simulado.setTurma(turma);
        }
        simulado.setDisciplina(disciplina(disciplinaId));
        return simulado;
    }

    private static AlunoTurma matriculaDoAluno(long alunoId) {
        AlunoTurma matricula = new AlunoTurma();
        matricula.setAluno(aluno(alunoId));
        return matricula;
    }

    private static Simulado publicadoComInicio(Long id, Long turmaId, Long disciplinaId) {
        Simulado simulado = simulado(id, turmaId, disciplinaId);
        simulado.setStatus(StatusSimulado.PUBLICADO);
        simulado.setDataInicio(LocalDateTime.of(2026, 8, 20, 8, 0));
        return simulado;
    }

    /** Plano de ensino ligado a um vínculo; {@code vinculoId} nulo é o plano genérico. */
    private static PlanoEnsino plano(Long vinculoId) {
        return plano(vinculoId, TURMA_ID, DISC_A);
    }

    /** Plano do vínculo informado, apontando a turma e a disciplina dadas. */
    private static PlanoEnsino plano(Long vinculoId, Long turmaId, Long disciplinaId) {
        PlanoEnsino plano = new PlanoEnsino();
        plano.setId(PLANO_ID);
        if (vinculoId != null) {
            TurmaDisciplina vinculo = new TurmaDisciplina();
            vinculo.setId(vinculoId);

            if (turmaId != null) {
                Turma turma = new Turma();
                turma.setId(turmaId);
                vinculo.setTurma(turma);
            }
            vinculo.setDisciplina(disciplina(disciplinaId));
            plano.setTurmaDisciplina(vinculo);
        }
        return plano;
    }

    private static SimuladoQuestao vinculo(Questao questao) {
        SimuladoQuestao vinculo = new SimuladoQuestao();
        vinculo.setSimulado(simulado(SIMULADO_ID, TURMA_ID, DISC_A));
        vinculo.setQuestao(questao);
        vinculo.setStatus(StatusSimuladoQuestao.ATIVA);
        return vinculo;
    }

    private static Questao questao(Long id, StatusQuestao status) {
        Questao questao = new Questao();
        questao.setId(id);
        questao.setStatus(status);
        return questao;
    }

    private static Aluno aluno(Long id) {
        Aluno aluno = new Aluno();
        aluno.setId(id);
        return aluno;
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
