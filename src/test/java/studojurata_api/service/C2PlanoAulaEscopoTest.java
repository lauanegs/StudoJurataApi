package studojurata_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
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

import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.model.AlunoTurma;
import studojurata_api.model.ConteudoPlano;
import studojurata_api.model.PlanoAula;
import studojurata_api.model.PlanoEnsino;
import studojurata_api.model.Turma;
import studojurata_api.model.TurmaDisciplina;
import studojurata_api.repository.AlunoTurmaRepository;
import studojurata_api.repository.AulaConteudoRepository;
import studojurata_api.repository.AulaRepository;
import studojurata_api.repository.ConteudoPlanoRepository;
import studojurata_api.repository.CursoRepository;
import studojurata_api.repository.HorarioTurmaRepository;
import studojurata_api.repository.PlanoAulaRepository;
import studojurata_api.repository.PlanoEnsinoRepository;
import studojurata_api.repository.TurmaDisciplinaRepository;
import studojurata_api.security.EscolaContext;
import studojurata_api.security.EscopoAluno;
import studojurata_api.security.EscopoProfessor;
import studojurata_api.security.EscopoUsuario;
import studojurata_api.security.UsuarioAutenticado;
import studojurata_api.support.AuthorizationTestSupport;

/**
 * C2.3 — escopo das listagens de plano de ensino, plano de aula, conteudo de
 * plano e das aulas de um plano de aula.
 *
 * <p>Identidade e regra de administrador sao reais; o recorte de vinculos
 * ({@link EscopoUsuario}) e dublado aqui, porque o alvo do teste e o
 * comportamento de cada service. As duas regras de escopo novas
 * ({@link EscopoAluno} e o proprio {@code EscopoUsuario}) tem casos proprios no
 * fim da classe.
 */
@ExtendWith(MockitoExtension.class)
class C2PlanoAulaEscopoTest {

    private static final long ESCOLA_ID = 1L;
    private static final long ALUNO_ID = 7L;
    private static final long PROFESSOR_ID = 42L;
    private static final long VINCULO_ID = 100L;
    private static final long OUTRO_VINCULO_ID = 200L;
    private static final long PLANO_AULA_ID = 300L;

    @Mock private PlanoEnsinoRepository planoEnsinoRepository;
    @Mock private CursoRepository cursoRepository;
    @Mock private PlanoAulaService planoAulaService;
    @Mock private EscolaContext escolaContext;
    @Mock private EscopoUsuario escopoUsuario;
    @Mock private PlanoAulaRepository planoAulaRepository;
    @Mock private AulaRepository aulaRepository;
    @Mock private HorarioTurmaRepository horarioTurmaRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private ConteudoPlanoRepository conteudoPlanoRepository;
    @Mock private AulaConteudoRepository aulaConteudoRepository;
    @Mock private AlunoTurmaRepository alunoTurmaRepository;
    @Mock private TurmaDisciplinaRepository turmaDisciplinaRepository;
    @Mock private EscopoProfessor escopoProfessor;

    private UsuarioAutenticado usuarioAutenticado;
    private PlanoEnsinoService planoEnsinoService;
    private PlanoAulaService servicoPlanoAula;
    private ConteudoPlanoService conteudoPlanoService;
    private AulaService aulaService;

    @BeforeEach
    void setUp() {
        usuarioAutenticado = new UsuarioAutenticado();
        planoEnsinoService = new PlanoEnsinoService(planoEnsinoRepository, cursoRepository, planoAulaService,
                escolaContext, usuarioAutenticado, escopoUsuario);
        servicoPlanoAula = new PlanoAulaService(planoAulaRepository, aulaRepository, usuarioAutenticado, escopoUsuario);
        conteudoPlanoService = new ConteudoPlanoService(conteudoPlanoRepository, aulaConteudoRepository,
                usuarioAutenticado, escopoUsuario);
        aulaService = new AulaService(aulaRepository, planoAulaRepository, planoEnsinoRepository,
                horarioTurmaRepository, auditLogService, usuarioAutenticado, escopoUsuario);
    }

    @AfterEach
    void limparContexto() {
        AuthorizationTestSupport.limparContexto();
    }

    // --- PlanoEnsinoService.listar() ---------------------------------------

    @Test
    @DisplayName("administrador lista os planos da escola, como antes")
    void administradorListaPlanosDaEscola() {
        AuthorizationTestSupport.autenticarComoAdministrador();
        given(escolaContext.escolaAtualId()).willReturn(ESCOLA_ID);
        given(planoEnsinoRepository.findByCurso_Escola_Id(ESCOLA_ID)).willReturn(List.of(plano(1L)));

        assertThat(planoEnsinoService.listar()).hasSize(1);

        verifyNoInteractions(escopoUsuario);
    }

    @Test
    @DisplayName("professor ve os planos dos seus vinculos e os planos genericos (D-C2)")
    void professorVeSeusPlanosMaisGenericos() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(escopoUsuario.turmaDisciplinaIds()).willReturn(Set.of(VINCULO_ID));
        given(planoEnsinoRepository.findByTurmaDisciplina_IdIn(Set.of(VINCULO_ID))).willReturn(List.of(plano(1L)));
        given(planoEnsinoRepository.findByTurmaDisciplinaIsNull()).willReturn(List.of(plano(2L)));

        assertThat(planoEnsinoService.listar()).extracting(PlanoEnsino::getId)
                .containsExactlyInAnyOrder(1L, 2L);

        verify(planoEnsinoRepository, never()).findAll();
        verify(planoEnsinoRepository, never()).findByCurso_Escola_Id(anyLong());
    }

    @Test
    @DisplayName("escopo vazio em planos nao consulta vinculos e preserva os genericos")
    void escopoVazioPreservaGenericos() {
        AuthorizationTestSupport.autenticarComoAluno(ALUNO_ID);
        given(escopoUsuario.turmaDisciplinaIds()).willReturn(Set.of());
        given(planoEnsinoRepository.findByTurmaDisciplinaIsNull()).willReturn(List.of(plano(2L)));

        assertThat(planoEnsinoService.listar()).extracting(PlanoEnsino::getId).containsExactly(2L);

        verify(planoEnsinoRepository, never()).findByTurmaDisciplina_IdIn(any());
    }

    @Test
    @DisplayName("sem autenticacao a listagem de planos falha fechado (403)")
    void semAutenticacaoNaoListaPlanos() {
        AuthorizationTestSupport.limparContexto();

        assertForbidden(() -> planoEnsinoService.listar());
        verifyNoInteractions(planoEnsinoRepository);
    }

    // --- PlanoAulaService.listar() -----------------------------------------

    @Test
    @DisplayName("administrador lista todos os planos de aula")
    void administradorListaTodosOsPlanosDeAula() {
        AuthorizationTestSupport.autenticarComoAdministrador();
        given(planoAulaRepository.findAll()).willReturn(List.of(new PlanoAula()));

        assertThat(servicoPlanoAula.listar()).hasSize(1);
    }

    @Test
    @DisplayName("professor lista apenas planos de aula dos seus vinculos")
    void professorListaSeusPlanosDeAula() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(escopoUsuario.turmaDisciplinaIds()).willReturn(Set.of(VINCULO_ID));
        given(planoAulaRepository.findByTurmaDisciplina_IdIn(Set.of(VINCULO_ID))).willReturn(List.of(new PlanoAula()));

        assertThat(servicoPlanoAula.listar()).hasSize(1);

        verify(planoAulaRepository, never()).findAll();
    }

    @Test
    @DisplayName("escopo vazio em plano de aula retorna vazio sem consultar repositorio")
    void escopoVazioNaoConsultaPlanosDeAula() {
        AuthorizationTestSupport.autenticarComoAluno(ALUNO_ID);
        given(escopoUsuario.turmaDisciplinaIds()).willReturn(Set.of());

        assertThat(servicoPlanoAula.listar()).isEmpty();

        verifyNoInteractions(planoAulaRepository);
    }

    @Test
    @DisplayName("sem autenticacao a listagem de planos de aula falha fechado (403)")
    void semAutenticacaoNaoListaPlanosDeAula() {
        AuthorizationTestSupport.limparContexto();

        assertForbidden(() -> servicoPlanoAula.listar());
        verifyNoInteractions(planoAulaRepository);
    }

    // --- ConteudoPlanoService.listar() -------------------------------------

    @Test
    @DisplayName("administrador lista todos os conteudos")
    void administradorListaTodosOsConteudos() {
        AuthorizationTestSupport.autenticarComoAdministrador();
        given(conteudoPlanoRepository.findAll()).willReturn(List.of(new ConteudoPlano()));

        assertThat(conteudoPlanoService.listar()).hasSize(1);
    }

    @Test
    @DisplayName("professor ve conteudos dos seus planos e os preservados (generico/sem plano)")
    void professorVeConteudosDoEscopoMaisPreservados() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(escopoUsuario.turmaDisciplinaIds()).willReturn(Set.of(VINCULO_ID));
        given(conteudoPlanoRepository.findByPlanoEnsino_TurmaDisciplina_IdIn(Set.of(VINCULO_ID)))
                .willReturn(List.of(new ConteudoPlano()));
        given(conteudoPlanoRepository.findByPlanoEnsino_TurmaDisciplinaIsNull()).willReturn(List.of(new ConteudoPlano()));
        given(conteudoPlanoRepository.findByPlanoEnsinoIsNull()).willReturn(List.of());

        assertThat(conteudoPlanoService.listar()).hasSize(2);

        verify(conteudoPlanoRepository, never()).findAll();
    }

    @Test
    @DisplayName("escopo vazio em conteudos preserva generico e sem plano")
    void escopoVazioPreservaConteudos() {
        AuthorizationTestSupport.autenticarComoAluno(ALUNO_ID);
        given(escopoUsuario.turmaDisciplinaIds()).willReturn(Set.of());
        given(conteudoPlanoRepository.findByPlanoEnsino_TurmaDisciplinaIsNull()).willReturn(List.of(new ConteudoPlano()));
        given(conteudoPlanoRepository.findByPlanoEnsinoIsNull()).willReturn(List.of(new ConteudoPlano()));

        assertThat(conteudoPlanoService.listar()).hasSize(2);

        verify(conteudoPlanoRepository, never()).findAll();
    }

    @Test
    @DisplayName("sem autenticacao a listagem de conteudos falha fechado (403)")
    void semAutenticacaoNaoListaConteudos() {
        AuthorizationTestSupport.limparContexto();

        assertForbidden(() -> conteudoPlanoService.listar());
        verifyNoInteractions(conteudoPlanoRepository);
    }

    // --- AulaService.listarPorPlanoAula() ----------------------------------

    @Test
    @DisplayName("administrador acessa as aulas de qualquer plano de aula")
    void administradorAcessaAulasDeQualquerPlano() {
        AuthorizationTestSupport.autenticarComoAdministrador();
        given(planoAulaRepository.findById(PLANO_AULA_ID)).willReturn(Optional.of(planoDeAula(OUTRO_VINCULO_ID)));
        given(aulaRepository.findByPlanoAula_IdOrderByOrdemAsc(PLANO_AULA_ID)).willReturn(List.of());

        assertThat(aulaService.listarPorPlanoAula(PLANO_AULA_ID)).isEmpty();

        verifyNoInteractions(escopoUsuario);
    }

    @Test
    @DisplayName("professor acessa aulas de plano do seu vinculo")
    void professorAcessaAulasDoSeuVinculo() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(planoAulaRepository.findById(PLANO_AULA_ID)).willReturn(Optional.of(planoDeAula(VINCULO_ID)));
        given(escopoUsuario.turmaDisciplinaIds()).willReturn(Set.of(VINCULO_ID));
        given(aulaRepository.findByPlanoAula_IdOrderByOrdemAsc(PLANO_AULA_ID)).willReturn(List.of());

        assertThat(aulaService.listarPorPlanoAula(PLANO_AULA_ID)).isEmpty();
    }

    @Test
    @DisplayName("professor nao acessa aulas de plano de outro vinculo")
    void professorNaoAcessaAulasForaDoEscopo() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(planoAulaRepository.findById(PLANO_AULA_ID)).willReturn(Optional.of(planoDeAula(OUTRO_VINCULO_ID)));
        given(escopoUsuario.turmaDisciplinaIds()).willReturn(Set.of(VINCULO_ID));

        assertForbidden(() -> aulaService.listarPorPlanoAula(PLANO_AULA_ID));
        verifyNoInteractions(aulaRepository);
    }

    @Test
    @DisplayName("aluno acessa aulas apenas de plano das suas turmas")
    void alunoAcessaAulasDasSuasTurmas() {
        AuthorizationTestSupport.autenticarComoAluno(ALUNO_ID);
        given(planoAulaRepository.findById(PLANO_AULA_ID)).willReturn(Optional.of(planoDeAula(VINCULO_ID)));
        given(escopoUsuario.turmaDisciplinaIds()).willReturn(Set.of(VINCULO_ID));
        given(aulaRepository.findByPlanoAula_IdOrderByOrdemAsc(PLANO_AULA_ID)).willReturn(List.of());

        assertThat(aulaService.listarPorPlanoAula(PLANO_AULA_ID)).isEmpty();
    }

    @Test
    @DisplayName("plano de aula inexistente responde 404 sem consultar aulas")
    void planoInexistenteNaoConsultaAulas() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(planoAulaRepository.findById(PLANO_AULA_ID)).willReturn(Optional.empty());

        try {
            aulaService.listarPorPlanoAula(PLANO_AULA_ID);
            fail("esperava RecursoNaoEncontradoException");
        } catch (RecursoNaoEncontradoException esperado) {
            verifyNoInteractions(aulaRepository);
        }
    }

    @Test
    @DisplayName("sem autenticacao as aulas de um plano falham fechado (403)")
    void semAutenticacaoNaoAcessaAulas() {
        AuthorizationTestSupport.limparContexto();
        given(planoAulaRepository.findById(PLANO_AULA_ID)).willReturn(Optional.of(planoDeAula(VINCULO_ID)));

        assertForbidden(() -> aulaService.listarPorPlanoAula(PLANO_AULA_ID));
        verifyNoInteractions(aulaRepository);
    }

    // --- Regras de escopo novas --------------------------------------------

    @Test
    @DisplayName("EscopoAluno: vinculos das turmas do aluno, sem repetir e sem N+1")
    void escopoAlunoResolveVinculos() {
        EscopoAluno escopoAluno = new EscopoAluno(alunoTurmaRepository, turmaDisciplinaRepository);
        given(alunoTurmaRepository.findByAluno_Id(ALUNO_ID))
                .willReturn(List.of(matricula(10L), matricula(20L)));
        given(turmaDisciplinaRepository.findByTurma_IdIn(Set.of(10L, 20L)))
                .willReturn(List.of(vinculo(VINCULO_ID), vinculo(OUTRO_VINCULO_ID), vinculo(VINCULO_ID)));

        assertThat(escopoAluno.turmaDisciplinaIdsDoAluno(ALUNO_ID))
                .containsExactlyInAnyOrder(VINCULO_ID, OUTRO_VINCULO_ID);
    }

    @Test
    @DisplayName("EscopoAluno: aluno sem turma nao consulta vinculos")
    void escopoAlunoSemTurmaNaoConsultaVinculos() {
        EscopoAluno escopoAluno = new EscopoAluno(alunoTurmaRepository, turmaDisciplinaRepository);
        given(alunoTurmaRepository.findByAluno_Id(ALUNO_ID)).willReturn(List.of());

        assertThat(escopoAluno.turmaDisciplinaIdsDoAluno(ALUNO_ID)).isEmpty();

        verifyNoInteractions(turmaDisciplinaRepository);
    }

    @Test
    @DisplayName("EscopoUsuario: professor delega ao escopo de professor")
    void escopoUsuarioDelegaParaProfessor() {
        EscopoUsuario real = new EscopoUsuario(escopoProfessor, new EscopoAluno(alunoTurmaRepository, turmaDisciplinaRepository), usuarioAutenticado);
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(escopoProfessor.turmaDisciplinaIdsDoProfessor(PROFESSOR_ID)).willReturn(Set.of(VINCULO_ID));

        assertThat(real.turmaDisciplinaIds()).containsExactly(VINCULO_ID);
    }

    @Test
    @DisplayName("EscopoUsuario: administrador nao recebe escopo (vazio nunca e 'todos')")
    void escopoUsuarioAdministradorEhVazio() {
        EscopoUsuario real = new EscopoUsuario(escopoProfessor, new EscopoAluno(alunoTurmaRepository, turmaDisciplinaRepository), usuarioAutenticado);
        AuthorizationTestSupport.autenticarComoAdministrador();

        assertThat(real.turmaDisciplinaIds()).isEmpty();
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

    private static PlanoEnsino plano(long id) {
        PlanoEnsino plano = new PlanoEnsino();
        plano.setId(id);
        return plano;
    }

    private static PlanoAula planoDeAula(long vinculoId) {
        TurmaDisciplina vinculo = vinculo(vinculoId);

        PlanoAula planoAula = new PlanoAula();
        planoAula.setId(PLANO_AULA_ID);
        planoAula.setTurmaDisciplina(vinculo);
        return planoAula;
    }

    private static TurmaDisciplina vinculo(long id) {
        TurmaDisciplina vinculo = new TurmaDisciplina();
        vinculo.setId(id);
        return vinculo;
    }

    private static AlunoTurma matricula(long turmaId) {
        Turma turma = new Turma();
        turma.setId(turmaId);

        AlunoTurma matricula = new AlunoTurma();
        matricula.setTurma(turma);
        return matricula;
    }
}
