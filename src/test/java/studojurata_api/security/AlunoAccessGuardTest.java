package studojurata_api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import studojurata_api.model.Pessoa;
import studojurata_api.model.Usuario;
import studojurata_api.model.enums.StatusAtivoInativo;
import studojurata_api.model.enums.TipoUsuario;
import studojurata_api.support.AuthorizationTestSupport;

/**
 * Teste unitário das quatro decisões do {@link AlunoAccessGuard}: leitura,
 * escrita, gestão e turma.
 *
 * <p>O {@link EscopoProfessor} é dublado porque o objeto sob teste aqui é a
 * decisão de autorização, não o cálculo de escopo (coberto por
 * {@code EscopoProfessorTest}). O guard é a instância real.
 */
class AlunoAccessGuardTest {

    private static final long ALUNO_LOGADO_ID = 7L;
    private static final long OUTRO_ALUNO_ID = 8L;
    private static final long PROFESSOR_LOGADO_ID = 42L;
    private static final long TURMA_ID = 10L;

    private EscopoProfessor escopoProfessor;
    private AlunoAccessGuard guard;

    @BeforeEach
    void setUp() {
        escopoProfessor = mock(EscopoProfessor.class);
        guard = new AlunoAccessGuard(escopoProfessor);
    }

    @AfterEach
    void limparContexto() {
        AuthorizationTestSupport.limparContexto();
    }

    // --- Leitura -----------------------------------------------------------

    @Test
    @DisplayName("aluno acessando o próprio id é permitido")
    void alunoNoProprioIdEhPermitido() {
        AuthorizationTestSupport.autenticarComoAluno(ALUNO_LOGADO_ID);

        guard.garantir(ALUNO_LOGADO_ID);
    }

    @Test
    @DisplayName("aluno acessando id de outro aluno recebe 403")
    void alunoEmIdDeOutroAlunoRecehe403() {
        AuthorizationTestSupport.autenticarComoAluno(ALUNO_LOGADO_ID);

        assertForbidden(() -> guard.garantir(OUTRO_ALUNO_ID));
    }

    @Test
    @DisplayName("aluno sem vínculo (Usuario.aluno nulo) recebe 403")
    void alunoSemVinculoRecehe403() {
        AuthorizationTestSupport.autenticarComoAlunoSemVinculo(AuthorizationTestSupport.PESSOA_ADMIN_ID);

        assertForbidden(() -> guard.garantir(ALUNO_LOGADO_ID));
    }

    @Test
    @DisplayName("professor acessa aluno das suas turmas")
    void professorAcessaAlunoDoEscopo() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_LOGADO_ID);
        given(escopoProfessor.lecionaPara(PROFESSOR_LOGADO_ID, ALUNO_LOGADO_ID)).willReturn(true);

        guard.garantir(ALUNO_LOGADO_ID);
    }

    @Test
    @DisplayName("professor acessando aluno fora do escopo recebe 403")
    void professorAcessaAlunoForaDoEscopoRecehe403() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_LOGADO_ID);
        given(escopoProfessor.lecionaPara(PROFESSOR_LOGADO_ID, OUTRO_ALUNO_ID)).willReturn(false);

        assertForbidden(() -> guard.garantir(OUTRO_ALUNO_ID));
    }

    @Test
    @DisplayName("professor sem vínculo com Professor recebe 403")
    void professorSemVinculoRecehe403() {
        autenticarProfessorSemVinculo();

        assertForbidden(() -> guard.garantir(ALUNO_LOGADO_ID));
    }

    @Test
    @DisplayName("administrador acessa qualquer aluno (dentro da fronteira de escola)")
    void administradorAcessaQualquerAluno() {
        AuthorizationTestSupport.autenticarComoAdministrador();

        guard.garantir(ALUNO_LOGADO_ID);
        guard.garantir(OUTRO_ALUNO_ID);
    }

    @Test
    @DisplayName("sem autenticação a leitura falha fechado (403)")
    void semAutenticacaoFalhaFechado() {
        AuthorizationTestSupport.limparContexto();

        assertForbidden(() -> guard.garantir(ALUNO_LOGADO_ID));
    }

    // --- Escrita -----------------------------------------------------------

    @Test
    @DisplayName("aluno escreve em nome próprio")
    void alunoEscreveNoProprioNome() {
        AuthorizationTestSupport.autenticarComoAluno(ALUNO_LOGADO_ID);

        guard.garantirEscritaDoAluno(ALUNO_LOGADO_ID);
    }

    @Test
    @DisplayName("aluno não escreve em nome de outro aluno")
    void alunoNaoEscreveEmNomeDeOutro() {
        AuthorizationTestSupport.autenticarComoAluno(ALUNO_LOGADO_ID);

        assertForbidden(() -> guard.garantirEscritaDoAluno(OUTRO_ALUNO_ID));
    }

    @Test
    @DisplayName("professor não escreve em nome de aluno, mesmo dentro do escopo")
    void professorNaoEscreveEmNomeDeAluno() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_LOGADO_ID);
        // Escopo favorável de propósito: a escrita não consulta escopo.
        given(escopoProfessor.lecionaPara(PROFESSOR_LOGADO_ID, ALUNO_LOGADO_ID)).willReturn(true);

        assertForbidden(() -> guard.garantirEscritaDoAluno(ALUNO_LOGADO_ID));
    }

    @Test
    @DisplayName("administrador escreve em nome de aluno")
    void administradorEscreveEmNomeDeAluno() {
        AuthorizationTestSupport.autenticarComoAdministrador();

        guard.garantirEscritaDoAluno(ALUNO_LOGADO_ID);
    }

    @Test
    @DisplayName("sem autenticação a escrita falha fechado (403)")
    void semAutenticacaoNaoEscreve() {
        AuthorizationTestSupport.limparContexto();

        assertForbidden(() -> guard.garantirEscritaDoAluno(ALUNO_LOGADO_ID));
    }

    // --- Gestão ------------------------------------------------------------

    @Test
    @DisplayName("gestão é liberada para professor e administrador")
    void gestaoLiberadaParaProfessorEAdministrador() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_LOGADO_ID);
        guard.garantirAcessoDeGestao();

        AuthorizationTestSupport.autenticarComoAdministrador();
        guard.garantirAcessoDeGestao();
    }

    @Test
    @DisplayName("gestão é negada para aluno e para requisição sem autenticação")
    void gestaoNegadaParaAlunoESemAutenticacao() {
        AuthorizationTestSupport.autenticarComoAluno(ALUNO_LOGADO_ID);
        assertForbidden(guard::garantirAcessoDeGestao);

        AuthorizationTestSupport.limparContexto();
        assertForbidden(guard::garantirAcessoDeGestao);
    }

    // --- Turma -------------------------------------------------------------

    @Test
    @DisplayName("professor acessa turma do seu escopo")
    void professorAcessaTurmaDoEscopo() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_LOGADO_ID);
        given(escopoProfessor.turmaIdsDoProfessor(PROFESSOR_LOGADO_ID)).willReturn(Set.of(TURMA_ID));

        guard.garantirAcessoATurma(TURMA_ID);
    }

    @Test
    @DisplayName("professor acessando turma alheia recebe 403")
    void professorAcessaTurmaAlheiaRecehe403() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_LOGADO_ID);
        given(escopoProfessor.turmaIdsDoProfessor(PROFESSOR_LOGADO_ID)).willReturn(Set.of(TURMA_ID));

        assertForbidden(() -> guard.garantirAcessoATurma(99L));
    }

    @Test
    @DisplayName("aluno não acessa dados recortados por turma")
    void alunoNaoAcessaTurma() {
        AuthorizationTestSupport.autenticarComoAluno(ALUNO_LOGADO_ID);

        assertForbidden(() -> guard.garantirAcessoATurma(TURMA_ID));
    }

    @Test
    @DisplayName("administrador acessa qualquer turma e turma nula é recusada")
    void administradorAcessaTurmaETurmaNulaEhRecusada() {
        AuthorizationTestSupport.autenticarComoAdministrador();
        guard.garantirAcessoATurma(TURMA_ID);

        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_LOGADO_ID);
        assertForbidden(() -> guard.garantirAcessoATurma(null));
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

    /**
     * Cenário montado localmente: a fábrica compartilhada não tem (e não deve
     * ganhar nesta etapa) um caso "professor sem vínculo".
     */
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
}
