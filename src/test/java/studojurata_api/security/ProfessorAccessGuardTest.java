package studojurata_api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
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
 * Teste unitário do {@link ProfessorAccessGuard}. Sem contexto Spring e sem
 * banco: o guard lê apenas o {@code SecurityContextHolder}.
 */
class ProfessorAccessGuardTest {

    private static final long PROFESSOR_LOGADO_ID = 42L;
    private static final long OUTRO_PROFESSOR_ID = 43L;
    private static final long ALUNO_ID = 7L;

    private final ProfessorAccessGuard guard = new ProfessorAccessGuard();

    @AfterEach
    void limparContexto() {
        AuthorizationTestSupport.limparContexto();
    }

    @Test
    @DisplayName("professor acessando o próprio id é permitido")
    void professorNoProprioIdEhPermitido() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_LOGADO_ID);

        guard.garantir(PROFESSOR_LOGADO_ID);
    }

    @Test
    @DisplayName("professor acessando id de outro professor recebe 403")
    void professorEmIdDeOutroProfessorRecehe403() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_LOGADO_ID);

        assertForbidden(() -> guard.garantir(OUTRO_PROFESSOR_ID));
    }

    @Test
    @DisplayName("professor sem vínculo (Usuario.professor nulo) recebe 403")
    void professorSemVinculoRecehe403() {
        autenticarProfessorSemVinculo();

        assertForbidden(() -> guard.garantir(PROFESSOR_LOGADO_ID));
    }

    @Test
    @DisplayName("administrador passa em qualquer id (gestão da própria escola)")
    void administradorPassaEmQualquerId() {
        AuthorizationTestSupport.autenticarComoAdministrador();

        guard.garantir(PROFESSOR_LOGADO_ID);
        guard.garantir(OUTRO_PROFESSOR_ID);
    }

    @Test
    @DisplayName("aluno recebe 403 mesmo que a rota não seja liberada para o perfil")
    void alunoRecehe403() {
        AuthorizationTestSupport.autenticarComoAluno(ALUNO_ID);

        assertForbidden(() -> guard.garantir(PROFESSOR_LOGADO_ID));
    }

    @Test
    @DisplayName("sem autenticação o guard falha fechado (403)")
    void semAutenticacaoFalhaFechado() {
        AuthorizationTestSupport.limparContexto();

        assertForbidden(() -> guard.garantir(PROFESSOR_LOGADO_ID));
    }

    @Test
    @DisplayName("professorLogadoId devolve o id apenas para professor vinculado")
    void professorLogadoIdDevolveOIdDoProfessor() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_LOGADO_ID);
        assertThat(guard.professorLogadoId()).contains(PROFESSOR_LOGADO_ID);

        AuthorizationTestSupport.autenticarComoAdministrador();
        assertThat(guard.professorLogadoId()).isEmpty();

        autenticarProfessorSemVinculo();
        assertThat(guard.professorLogadoId()).isEmpty();

        AuthorizationTestSupport.limparContexto();
        assertThat(guard.professorLogadoId()).isEqualTo(Optional.empty());
    }

    @Test
    @DisplayName("ehAdministrador é verdadeiro apenas para o administrador")
    void ehAdministradorApenasParaAdministrador() {
        AuthorizationTestSupport.autenticarComoAdministrador();
        assertThat(guard.ehAdministrador()).isTrue();

        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_LOGADO_ID);
        assertThat(guard.ehAdministrador()).isFalse();

        AuthorizationTestSupport.limparContexto();
        assertThat(guard.ehAdministrador()).isFalse();
    }

    /**
     * Cenário montado localmente de propósito: a fábrica compartilhada
     * ({@code AuthorizationTestSupport}) não tem — e não deve ganhar nesta
     * etapa — um caso "professor sem vínculo", porque alterá-la sairia dos
     * quatro arquivos aprovados no bloco.
     */
    private void autenticarProfessorSemVinculo() {
        Pessoa pessoa = new Pessoa();
        pessoa.setId(900L);
        pessoa.setNome("Professor sem vínculo");
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

    private void assertForbidden(Runnable acao) {
        try {
            acao.run();
        } catch (ResponseStatusException ex) {
            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            return;
        }
        fail("esperava ResponseStatusException 403, mas a chamada foi permitida");
    }
}
