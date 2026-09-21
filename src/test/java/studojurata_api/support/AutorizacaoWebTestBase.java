package studojurata_api.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import studojurata_api.config.SecurityConfig;
import studojurata_api.security.AlunoAccessGuard;
import studojurata_api.security.CustomUserDetailsService;

/**
 * Base dos testes de autorização que passam pelo {@code springSecurityFilterChain}.
 *
 * <p>Importa o {@link SecurityConfig} real: a intenção é verificar exatamente
 * as regras de rota que valem em produção, e não uma configuração paralela de
 * teste que poderia divergir dela.
 *
 * <p>A raiz do contexto continua sendo a classe principal da aplicação (a
 * {@code @SpringBootConfiguration} encontrada a partir do pacote do teste),
 * para que a varredura de componentes enxergue os controllers exatamente como
 * em produção. Como ela carrega {@code @EnableJpaAuditing}, o
 * {@code jpaAuditingHandler} é registrado com uma referência por nome a
 * {@code jpaMappingContext} — bean que só existe quando o JPA está ativo, o
 * que uma fatia web deliberadamente não ativa. O dublê abaixo satisfaz essa
 * referência sem trazer o JPA (nem banco de dados) para o teste.
 *
 * <p>{@link CustomUserDetailsService} é dublado porque o {@code SecurityConfig}
 * o exige para montar o {@code DaoAuthenticationProvider}; {@link AlunoAccessGuard}
 * entra como dublê para os controllers que já o usam (Notas e Gamificação) e
 * para os testes de posse que virão nos blocos seguintes.
 *
 * <p>As subclasses devem declarar {@code @WebMvcTest(controllers = ...)}. Rotas
 * sem controller na fatia ainda são avaliadas pelo filtro de segurança, o que
 * permite testar negação por papel sem carregar o controller correspondente.
 */
@Import(SecurityConfig.class)
public abstract class AutorizacaoWebTestBase {

    protected static final MediaType JSON = MediaType.APPLICATION_JSON;

    @MockBean
    protected JpaMetamodelMappingContext jpaMappingContext;

    @MockBean
    protected CustomUserDetailsService customUserDetailsService;

    @MockBean
    protected AlunoAccessGuard alunoAccessGuard;

    @Autowired
    protected MockMvc mockMvc;

    // --- Atalhos de autenticação para MockMvc -------------------------------

    protected RequestPostProcessor comoAluno(long alunoId) {
        return SecurityMockMvcRequestPostProcessors.authentication(
                AuthorizationTestSupport.autenticacaoDeAluno(alunoId));
    }

    protected RequestPostProcessor comoProfessor(long professorId) {
        return SecurityMockMvcRequestPostProcessors.authentication(
                AuthorizationTestSupport.autenticacaoDeProfessor(professorId));
    }

    protected RequestPostProcessor comoAdministrador() {
        return SecurityMockMvcRequestPostProcessors.authentication(
                AuthorizationTestSupport.autenticacaoDeAdministrador());
    }

    /** Corpo JSON vazio, útil quando a asserção é sobre autorização e não sobre o payload. */
    protected MockHttpServletRequestBuilder comCorpoVazio(MockHttpServletRequestBuilder builder) {
        return builder.contentType(JSON).content("{}");
    }
}
