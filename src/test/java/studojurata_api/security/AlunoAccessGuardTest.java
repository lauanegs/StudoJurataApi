package studojurata_api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import studojurata_api.support.AuthorizationTestSupport;

/**
 * Teste unitário do guard que já existe em produção (Notas e Gamificação).
 *
 * <p>Não altera nenhuma regra: apenas congela o comportamento atual para que
 * os blocos seguintes possam generalizar o mesmo guard para tentativas de
 * simulado, respostas e frequência sem risco de regressão silenciosa.
 */
class AlunoAccessGuardTest {

    private static final long ALUNO_LOGADO_ID = 7L;
    private static final long OUTRO_ALUNO_ID = 8L;

    private final AlunoAccessGuard guard = new AlunoAccessGuard();

    @AfterEach
    void limparContexto() {
        AuthorizationTestSupport.limparContexto();
    }

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
    @DisplayName("professor acessa aluno de qualquer id (necessidade de gestão)")
    void professorAcessaQualquerAluno() {
        AuthorizationTestSupport.autenticarComoProfessor(42L);

        guard.garantir(ALUNO_LOGADO_ID);
        guard.garantir(OUTRO_ALUNO_ID);
    }

    @Test
    @DisplayName("administrador acessa aluno de qualquer id")
    void administradorAcessaQualquerAluno() {
        AuthorizationTestSupport.autenticarComoAdministrador();

        guard.garantir(ALUNO_LOGADO_ID);
        guard.garantir(OUTRO_ALUNO_ID);
    }

    @Test
    @DisplayName("sem autenticação o guard falha fechado (403)")
    void semAutenticacaoFalhaFechado() {
        AuthorizationTestSupport.limparContexto();

        assertForbidden(() -> guard.garantir(ALUNO_LOGADO_ID));
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
