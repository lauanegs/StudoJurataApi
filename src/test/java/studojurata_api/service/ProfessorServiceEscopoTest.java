package studojurata_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import studojurata_api.model.AlunoTurma;
import studojurata_api.model.Pessoa;
import studojurata_api.model.Professor;
import studojurata_api.model.Turma;
import studojurata_api.model.TurmaDisciplina;
import studojurata_api.model.Usuario;
import studojurata_api.model.enums.StatusAtivoInativo;
import studojurata_api.model.enums.TipoUsuario;
import studojurata_api.repository.AlunoTurmaRepository;
import studojurata_api.repository.ProfessorRepository;
import studojurata_api.repository.TurmaDisciplinaRepository;
import studojurata_api.security.CustomUserDetails;
import studojurata_api.security.ProfessorAccessGuard;
import studojurata_api.security.UsuarioAutenticado;
import studojurata_api.support.AuthorizationTestSupport;

/**
 * C2.1 — escopo da listagem e das consultas de professor.
 *
 * <p>Identidade e guarda são as implementações reais (leem o
 * {@code SecurityContextHolder}); só os repositórios são dublados. Assim o
 * teste exercita a regra de verdade, sem banco.
 */
@ExtendWith(MockitoExtension.class)
class ProfessorServiceEscopoTest {

    private static final long ALUNO_ID = 7L;
    private static final long PROFESSOR_ID = 42L;
    private static final long OUTRO_PROFESSOR_ID = 43L;
    private static final long TURMA_A = 10L;
    private static final long TURMA_B = 20L;

    @Mock private ProfessorRepository professorRepository;
    @Mock private TurmaDisciplinaRepository turmaDisciplinaRepository;
    @Mock private AlunoTurmaRepository alunoTurmaRepository;

    private ProfessorService service;

    @BeforeEach
    void setUp() {
        service = new ProfessorService(professorRepository, turmaDisciplinaRepository, alunoTurmaRepository,
                new UsuarioAutenticado(), new ProfessorAccessGuard());
    }

    @AfterEach
    void limparContexto() {
        AuthorizationTestSupport.limparContexto();
    }

    // --- listar() ----------------------------------------------------------

    @Test
    @DisplayName("administrador continua listando todos os professores")
    void administradorListaTodos() {
        AuthorizationTestSupport.autenticarComoAdministrador();
        given(professorRepository.findAll()).willReturn(List.of(professor(PROFESSOR_ID), professor(OUTRO_PROFESSOR_ID)));

        assertThat(service.listar()).hasSize(2);

        verify(professorRepository).findAll();
    }

    @Test
    @DisplayName("professor lista apenas a si mesmo, sem chamar findAll")
    void professorListaApenasASiMesmo() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(professorRepository.findById(PROFESSOR_ID)).willReturn(Optional.of(professor(PROFESSOR_ID)));

        assertThat(service.listar()).extracting(Professor::getId).containsExactly(PROFESSOR_ID);

        verify(professorRepository, never()).findAll();
    }

    @Test
    @DisplayName("professor sem cadastro vinculado não enxerga ninguém")
    void professorSemVinculoListaVazio() {
        autenticarProfessorSemVinculo();

        assertThat(service.listar()).isEmpty();

        verifyNoInteractions(professorRepository, turmaDisciplinaRepository, alunoTurmaRepository);
    }

    @Test
    @DisplayName("aluno lista apenas professores das turmas em que está matriculado")
    void alunoListaProfessoresDasSuasTurmas() {
        AuthorizationTestSupport.autenticarComoAluno(ALUNO_ID);
        given(alunoTurmaRepository.findByAluno_Id(ALUNO_ID))
                .willReturn(List.of(matricula(TURMA_A), matricula(TURMA_B)));
        given(turmaDisciplinaRepository.findByTurma_IdIn(Set.of(TURMA_A, TURMA_B)))
                .willReturn(List.of(vinculo(professor(PROFESSOR_ID)), vinculo(professor(OUTRO_PROFESSOR_ID)),
                        vinculo(professor(PROFESSOR_ID))));
        given(professorRepository.findAllById(Set.of(PROFESSOR_ID, OUTRO_PROFESSOR_ID)))
                .willReturn(List.of(professor(PROFESSOR_ID), professor(OUTRO_PROFESSOR_ID)));

        assertThat(service.listar()).extracting(Professor::getId)
                .containsExactlyInAnyOrder(PROFESSOR_ID, OUTRO_PROFESSOR_ID);

        verify(professorRepository, never()).findAll();
        verify(professorRepository, never()).findById(any());
    }

    @Test
    @DisplayName("aluno sem matrícula não consulta vínculos nem professores")
    void alunoSemTurmasListaVazioSemConsultar() {
        AuthorizationTestSupport.autenticarComoAluno(ALUNO_ID);
        given(alunoTurmaRepository.findByAluno_Id(ALUNO_ID)).willReturn(List.of());

        assertThat(service.listar()).isEmpty();

        verifyNoInteractions(turmaDisciplinaRepository, professorRepository);
    }

    @Test
    @DisplayName("sem autenticação a listagem falha fechado (403)")
    void semAutenticacaoNaoLista() {
        AuthorizationTestSupport.limparContexto();

        assertForbidden(() -> service.listar());
        verifyNoInteractions(professorRepository, turmaDisciplinaRepository, alunoTurmaRepository);
    }

    // --- buscar() ----------------------------------------------------------

    @Test
    @DisplayName("professor busca o próprio cadastro")
    void professorBuscaProprioCadastro() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(professorRepository.findById(PROFESSOR_ID)).willReturn(Optional.of(professor(PROFESSOR_ID)));

        assertThat(service.buscar(PROFESSOR_ID).getId()).isEqualTo(PROFESSOR_ID);
    }

    @Test
    @DisplayName("professor não busca cadastro de outro professor")
    void professorNaoBuscaOutroProfessor() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);

        assertForbidden(() -> service.buscar(OUTRO_PROFESSOR_ID));
        verifyNoInteractions(professorRepository);
    }

    @Test
    @DisplayName("administrador busca qualquer professor")
    void administradorBuscaQualquerProfessor() {
        AuthorizationTestSupport.autenticarComoAdministrador();
        given(professorRepository.findById(OUTRO_PROFESSOR_ID)).willReturn(Optional.of(professor(OUTRO_PROFESSOR_ID)));

        assertThat(service.buscar(OUTRO_PROFESSOR_ID).getId()).isEqualTo(OUTRO_PROFESSOR_ID);
    }

    @Test
    @DisplayName("aluno não busca cadastro de professor")
    void alunoNaoBuscaProfessor() {
        AuthorizationTestSupport.autenticarComoAluno(ALUNO_ID);

        assertForbidden(() -> service.buscar(PROFESSOR_ID));
        verifyNoInteractions(professorRepository);
    }

    // --- turmasLecionadas() ------------------------------------------------

    @Test
    @DisplayName("professor lista as próprias turmas")
    void professorListaAsPropriasTurmas() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(turmaDisciplinaRepository.findByProfessorId(PROFESSOR_ID))
                .willReturn(List.of(vinculo(professor(PROFESSOR_ID))));

        assertThat(service.turmasLecionadas(PROFESSOR_ID)).hasSize(1);
    }

    @Test
    @DisplayName("professor não lista turmas de outro professor")
    void professorNaoListaTurmasDeOutro() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);

        assertForbidden(() -> service.turmasLecionadas(OUTRO_PROFESSOR_ID));
        verifyNoInteractions(turmaDisciplinaRepository);
    }

    @Test
    @DisplayName("administrador lista turmas de qualquer professor")
    void administradorListaTurmasDeQualquerProfessor() {
        AuthorizationTestSupport.autenticarComoAdministrador();
        given(turmaDisciplinaRepository.findByProfessorId(OUTRO_PROFESSOR_ID))
                .willReturn(List.of(vinculo(professor(OUTRO_PROFESSOR_ID))));

        assertThat(service.turmasLecionadas(OUTRO_PROFESSOR_ID)).hasSize(1);
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

    /** Principal de professor sem vínculo com a entidade Professor. */
    private void autenticarProfessorSemVinculo() {
        Pessoa pessoa = new Pessoa();
        pessoa.setId(900L);
        pessoa.setStatus(StatusAtivoInativo.ATIVO);

        Usuario usuario = new Usuario();
        usuario.setId(901L);
        usuario.setPessoa(pessoa);
        usuario.setUsername("professor.sem.vinculo");
        usuario.setSenha("hash-irrelevante-para-autorizacao");
        usuario.setTipoUsuario(TipoUsuario.PROFESSOR);
        usuario.setStatus(StatusAtivoInativo.ATIVO);
        usuario.setProfessor(null);

        CustomUserDetails principal = new CustomUserDetails(usuario);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private static Professor professor(long id) {
        Professor professor = new Professor();
        professor.setId(id);
        professor.setStatus(StatusAtivoInativo.ATIVO);
        return professor;
    }

    private static TurmaDisciplina vinculo(Professor professor) {
        TurmaDisciplina vinculo = new TurmaDisciplina();
        vinculo.setProfessor(professor);
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
