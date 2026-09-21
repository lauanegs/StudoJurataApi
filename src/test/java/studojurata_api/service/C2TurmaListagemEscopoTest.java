package studojurata_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.BDDMockito.given;
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

import studojurata_api.repository.AlunoTurmaRepository;
import studojurata_api.repository.CursoDisciplinaRepository;
import studojurata_api.repository.CursoRepository;
import studojurata_api.repository.PlanoAulaRepository;
import studojurata_api.repository.PlanoEnsinoRepository;
import studojurata_api.repository.TurmaDisciplinaRepository;
import studojurata_api.repository.TurmaRepository;
import studojurata_api.model.AlunoTurma;
import studojurata_api.model.Turma;
import studojurata_api.model.TurmaDisciplina;
import studojurata_api.security.EscolaContext;
import studojurata_api.security.EscopoProfessor;
import studojurata_api.security.UsuarioAutenticado;
import studojurata_api.support.AuthorizationTestSupport;

/**
 * C2.2 — listagens de turma e de vínculo turma-disciplina escopadas por perfil.
 *
 * <p>Cobre os dois services no mesmo teste porque o bloco é um só: a mesma
 * regra (admin → escola/tudo, professor → suas turmas, aluno → suas turmas) e o
 * mesmo par de consultas. Identidade e escopo são reais; só repositórios dublam.
 */
@ExtendWith(MockitoExtension.class)
class C2TurmaListagemEscopoTest {

    private static final long ESCOLA_ID = 1L;
    private static final long ALUNO_ID = 7L;
    private static final long PROFESSOR_ID = 42L;
    private static final long TURMA_A = 10L;
    private static final long TURMA_B = 20L;

    @Mock private TurmaRepository turmaRepository;
    @Mock private AlunoTurmaRepository alunoTurmaRepository;
    @Mock private AlunoTurmaService alunoTurmaService;
    @Mock private CursoRepository cursoRepository;
    @Mock private EscolaContext escolaContext;
    @Mock private EscopoProfessor escopoProfessor;
    @Mock private TurmaDisciplinaRepository turmaDisciplinaRepository;
    @Mock private CursoDisciplinaRepository cursoDisciplinaRepository;
    @Mock private PlanoEnsinoRepository planoEnsinoRepository;
    @Mock private PlanoAulaRepository planoAulaRepository;

    private TurmaService turmaService;
    private TurmaDisciplinaService turmaDisciplinaService;

    @BeforeEach
    void setUp() {
        turmaService = new TurmaService(turmaRepository, alunoTurmaRepository, alunoTurmaService, cursoRepository,
                escolaContext, escopoProfessor, new UsuarioAutenticado());
        turmaDisciplinaService = new TurmaDisciplinaService(turmaDisciplinaRepository, turmaRepository,
                cursoDisciplinaRepository, planoEnsinoRepository, planoAulaRepository, alunoTurmaRepository,
                escopoProfessor, new UsuarioAutenticado());
    }

    @AfterEach
    void limparContexto() {
        AuthorizationTestSupport.limparContexto();
    }

    // --- TurmaService.listar() ---------------------------------------------

    @Test
    @DisplayName("administrador lista as turmas da escola, como antes")
    void administradorListaTurmasDaEscola() {
        AuthorizationTestSupport.autenticarComoAdministrador();
        given(escolaContext.escolaAtualId()).willReturn(ESCOLA_ID);
        given(turmaRepository.findByEscola_Id(ESCOLA_ID)).willReturn(List.of(turma(TURMA_A), turma(TURMA_B)));

        assertThat(turmaService.listar()).hasSize(2);
    }

    @Test
    @DisplayName("professor lista apenas as turmas em que leciona")
    void professorListaApenasSuasTurmas() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(escopoProfessor.turmaIdsDoProfessor(PROFESSOR_ID)).willReturn(Set.of(TURMA_A));
        given(turmaRepository.findAllById(Set.of(TURMA_A))).willReturn(List.of(turma(TURMA_A)));

        assertThat(turmaService.listar()).extracting(Turma::getId).containsExactly(TURMA_A);

        verify(turmaRepository, never()).findByEscola_Id(ESCOLA_ID);
        verify(turmaRepository, never()).findAll();
    }

    @Test
    @DisplayName("aluno lista apenas as turmas em que está matriculado")
    void alunoListaApenasSuasTurmas() {
        AuthorizationTestSupport.autenticarComoAluno(ALUNO_ID);
        given(alunoTurmaRepository.findByAluno_Id(ALUNO_ID)).willReturn(List.of(matricula(TURMA_B)));
        given(turmaRepository.findAllById(Set.of(TURMA_B))).willReturn(List.of(turma(TURMA_B)));

        assertThat(turmaService.listar()).extracting(Turma::getId).containsExactly(TURMA_B);
    }

    @Test
    @DisplayName("aluno sem matrícula não consulta o repositório de turmas")
    void alunoSemMatriculaNaoConsultaTurmas() {
        AuthorizationTestSupport.autenticarComoAluno(ALUNO_ID);
        given(alunoTurmaRepository.findByAluno_Id(ALUNO_ID)).willReturn(List.of());

        assertThat(turmaService.listar()).isEmpty();

        verifyNoInteractions(turmaRepository);
    }

    @Test
    @DisplayName("professor sem vínculo não lista turma nenhuma")
    void professorSemVinculoListaVazio() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(escopoProfessor.turmaIdsDoProfessor(PROFESSOR_ID)).willReturn(Set.of());

        assertThat(turmaService.listar()).isEmpty();

        verifyNoInteractions(turmaRepository);
    }

    @Test
    @DisplayName("sem autenticação a listagem de turmas falha fechado (403)")
    void semAutenticacaoNaoListaTurmas() {
        AuthorizationTestSupport.limparContexto();

        assertForbidden(() -> turmaService.listar());
        verifyNoInteractions(turmaRepository, alunoTurmaRepository);
    }

    // --- TurmaDisciplinaService.listar() -----------------------------------

    @Test
    @DisplayName("administrador continua vendo todos os vínculos")
    void administradorListaTodosOsVinculos() {
        AuthorizationTestSupport.autenticarComoAdministrador();
        given(turmaDisciplinaRepository.findAll()).willReturn(List.of(vinculo(1L), vinculo(2L)));

        assertThat(turmaDisciplinaService.listar()).hasSize(2);
    }

    @Test
    @DisplayName("professor vê apenas os próprios vínculos")
    void professorVeApenasSeusVinculos() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(turmaDisciplinaRepository.findByProfessorId(PROFESSOR_ID)).willReturn(List.of(vinculo(1L)));

        assertThat(turmaDisciplinaService.listar()).hasSize(1);

        verify(turmaDisciplinaRepository, never()).findAll();
    }

    @Test
    @DisplayName("aluno vê os vínculos das suas turmas")
    void alunoVeVinculosDasSuasTurmas() {
        AuthorizationTestSupport.autenticarComoAluno(ALUNO_ID);
        given(alunoTurmaRepository.findByAluno_Id(ALUNO_ID)).willReturn(List.of(matricula(TURMA_A)));
        given(turmaDisciplinaRepository.findByTurma_IdIn(Set.of(TURMA_A))).willReturn(List.of(vinculo(1L)));

        assertThat(turmaDisciplinaService.listar()).hasSize(1);

        verify(turmaDisciplinaRepository, never()).findAll();
    }

    @Test
    @DisplayName("aluno sem matrícula não consulta vínculos")
    void alunoSemMatriculaNaoConsultaVinculos() {
        AuthorizationTestSupport.autenticarComoAluno(ALUNO_ID);
        given(alunoTurmaRepository.findByAluno_Id(ALUNO_ID)).willReturn(List.of());

        assertThat(turmaDisciplinaService.listar()).isEmpty();

        verifyNoInteractions(turmaDisciplinaRepository);
    }

    @Test
    @DisplayName("sem autenticação a listagem de vínculos falha fechado (403)")
    void semAutenticacaoNaoListaVinculos() {
        AuthorizationTestSupport.limparContexto();

        assertForbidden(() -> turmaDisciplinaService.listar());
        verifyNoInteractions(turmaDisciplinaRepository);
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

    private static Turma turma(long id) {
        Turma turma = new Turma();
        turma.setId(id);
        return turma;
    }

    private static AlunoTurma matricula(long turmaId) {
        AlunoTurma matricula = new AlunoTurma();
        matricula.setTurma(turma(turmaId));
        return matricula;
    }

    private static TurmaDisciplina vinculo(long id) {
        TurmaDisciplina vinculo = new TurmaDisciplina();
        vinculo.setId(id);
        return vinculo;
    }
}
