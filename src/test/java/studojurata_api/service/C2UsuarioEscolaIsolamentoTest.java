package studojurata_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Optional;

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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import studojurata_api.exception.RequisicaoInvalidaException;
import studojurata_api.model.Escola;
import studojurata_api.model.Usuario;
import studojurata_api.model.enums.StatusAtivoInativo;
import studojurata_api.model.enums.TipoUsuario;
import studojurata_api.repository.UsuarioRepository;
import studojurata_api.security.CustomUserDetails;
import studojurata_api.security.EscolaContext;
import studojurata_api.support.AuthorizationTestSupport;

/**
 * C2 — isolamento por escola nos usuários ({@code /usuarios/**}).
 *
 * <p>A listagem já filtrava pela escola do logado, mas as rotas por id
 * (consultar, atualizar, desativar e reativar) não: um administrador da escola
 * A lia e reescrevia — inclusive a senha — um usuário da escola B. A validação
 * vive em {@code buscar()}, por onde todas as rotas por id passam.
 *
 * <p>Identidade e {@code EscolaContext} são reais; só o repositório e o
 * codificador de senha dublam.
 */
@ExtendWith(MockitoExtension.class)
class C2UsuarioEscolaIsolamentoTest {

    private static final long ESCOLA_A = 1L;
    private static final long ESCOLA_B = 2L;
    private static final long USUARIO_ID = 500L;
    private static final long USUARIO_DE_OUTRA_ESCOLA_ID = 501L;

    @Mock private UsuarioRepository repository;
    @Mock private PasswordEncoder passwordEncoder;

    private UsuarioService service;

    @BeforeEach
    void setUp() {
        service = new UsuarioService(repository, passwordEncoder, new EscolaContext());
    }

    @AfterEach
    void limparContexto() {
        AuthorizationTestSupport.limparContexto();
    }

    // --- leitura ------------------------------------------------------------

    @Test
    @DisplayName("administrador lê usuário da própria escola")
    void administradorLeUsuarioDaPropriaEscola() {
        AuthorizationTestSupport.autenticarComoAdministrador();
        given(repository.findById(USUARIO_ID)).willReturn(Optional.of(usuario(USUARIO_ID, ESCOLA_A)));

        assertThat(service.buscar(USUARIO_ID).getId()).isEqualTo(USUARIO_ID);
    }

    @Test
    @DisplayName("administrador não lê usuário de outra escola")
    void administradorNaoLeUsuarioDeOutraEscola() {
        AuthorizationTestSupport.autenticarComoAdministrador();
        given(repository.findById(USUARIO_DE_OUTRA_ESCOLA_ID))
                .willReturn(Optional.of(usuario(USUARIO_DE_OUTRA_ESCOLA_ID, ESCOLA_B)));

        assertForbidden(() -> service.buscar(USUARIO_DE_OUTRA_ESCOLA_ID));
    }

    @Test
    @DisplayName("sem escola no contexto (cadastro inicial) o comportamento antigo é preservado")
    void semEscolaNoContextoNaoFiltra() {
        autenticarAdministradorSemEscola();
        given(repository.findById(USUARIO_DE_OUTRA_ESCOLA_ID))
                .willReturn(Optional.of(usuario(USUARIO_DE_OUTRA_ESCOLA_ID, ESCOLA_B)));

        assertThat(service.buscar(USUARIO_DE_OUTRA_ESCOLA_ID).getId()).isEqualTo(USUARIO_DE_OUTRA_ESCOLA_ID);
    }

    // --- escrita por id -----------------------------------------------------

    @Test
    @DisplayName("administrador não atualiza usuário de outra escola e nada é gravado")
    void administradorNaoAtualizaUsuarioDeOutraEscola() {
        AuthorizationTestSupport.autenticarComoAdministrador();
        given(repository.findById(USUARIO_DE_OUTRA_ESCOLA_ID))
                .willReturn(Optional.of(usuario(USUARIO_DE_OUTRA_ESCOLA_ID, ESCOLA_B)));

        assertForbidden(() -> service.atualizar(USUARIO_DE_OUTRA_ESCOLA_ID, usuarioComSenha("nova-senha")));

        verify(repository, never()).save(any(Usuario.class));
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("administrador não desativa usuário de outra escola")
    void administradorNaoDesativaUsuarioDeOutraEscola() {
        AuthorizationTestSupport.autenticarComoAdministrador();
        given(repository.findById(USUARIO_DE_OUTRA_ESCOLA_ID))
                .willReturn(Optional.of(usuario(USUARIO_DE_OUTRA_ESCOLA_ID, ESCOLA_B)));

        assertForbidden(() -> service.deletar(USUARIO_DE_OUTRA_ESCOLA_ID));

        verify(repository, never()).save(any(Usuario.class));
    }

    @Test
    @DisplayName("administrador não reativa usuário de outra escola")
    void administradorNaoReativaUsuarioDeOutraEscola() {
        AuthorizationTestSupport.autenticarComoAdministrador();
        given(repository.findById(USUARIO_DE_OUTRA_ESCOLA_ID))
                .willReturn(Optional.of(usuario(USUARIO_DE_OUTRA_ESCOLA_ID, ESCOLA_B)));

        assertForbidden(() -> service.ativar(USUARIO_DE_OUTRA_ESCOLA_ID));

        verify(repository, never()).save(any(Usuario.class));
    }

    @Test
    @DisplayName("atualizar usuário da própria escola mantém a escola do cadastro")
    void administradorAtualizaUsuarioDaPropriaEscola() {
        AuthorizationTestSupport.autenticarComoAdministrador();
        given(repository.findById(USUARIO_ID)).willReturn(Optional.of(usuario(USUARIO_ID, ESCOLA_A)));
        given(passwordEncoder.encode("hash-antigo")).willReturn("hash-novo");
        given(repository.save(any(Usuario.class))).willAnswer(chamada -> chamada.getArgument(0));

        Usuario alteracao = usuario(USUARIO_ID, ESCOLA_B);
        assertThat(service.atualizar(USUARIO_ID, alteracao).getEscola().getId()).isEqualTo(ESCOLA_A);

        verify(repository).save(any(Usuario.class));
    }

    // --- criação ------------------------------------------------------------

    @Test
    @DisplayName("administrador não cadastra usuário em outra escola")
    void administradorNaoCadastraUsuarioEmOutraEscola() {
        AuthorizationTestSupport.autenticarComoAdministrador();

        assertForbidden(() -> service.salvar(usuarioComSenha("senha")));

        verify(repository, never()).save(any(Usuario.class));
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("cadastro sem escola é recusado como requisição inválida")
    void cadastroSemEscolaEhInvalido() {
        AuthorizationTestSupport.autenticarComoAdministrador();
        Usuario semEscola = usuarioComSenha("senha");
        semEscola.setEscola(null);

        try {
            service.salvar(semEscola);
            fail("esperava 400");
        } catch (RequisicaoInvalidaException esperada) {
            assertThat(esperada.getMessage()).contains("Escola");
        }

        verify(repository, never()).save(any(Usuario.class));
    }

    @Test
    @DisplayName("administrador cadastra usuário na própria escola")
    void administradorCadastraUsuarioNaPropriaEscola() {
        AuthorizationTestSupport.autenticarComoAdministrador();
        given(passwordEncoder.encode("senha")).willReturn("hash");
        given(repository.save(any(Usuario.class))).willAnswer(chamada -> chamada.getArgument(0));

        Usuario novo = usuarioComSenha("senha");
        novo.setEscola(escolaDa(ESCOLA_A));

        Usuario salvo = service.salvar(novo);

        assertThat(salvo.getSenha()).isEqualTo("hash");
        assertThat(salvo.getEscola().getId()).isEqualTo(ESCOLA_A);
    }

    // --- apoio --------------------------------------------------------------

    private static Usuario usuario(long id, long escolaId) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setEscola(escolaDa(escolaId));
        usuario.setUsername("usuario." + id);
        usuario.setSenha("hash-antigo");
        usuario.setTipoUsuario(TipoUsuario.ADMINISTRADOR);
        usuario.setStatus(StatusAtivoInativo.ATIVO);
        return usuario;
    }

    private static Escola escolaDa(long escolaId) {
        Escola escola = new Escola();
        escola.setId(escolaId);
        return escola;
    }

    /** Usuário do corpo da requisição: aponta para a escola B (a de outra escola). */
    private static Usuario usuarioComSenha(String senha) {
        Usuario usuario = usuario(0L, ESCOLA_B);
        usuario.setSenha(senha);
        return usuario;
    }

    /** Administrador sem escola: cenário anterior ao cadastro inicial da escola. */
    private static void autenticarAdministradorSemEscola() {
        Usuario usuario = new Usuario();
        usuario.setId(USUARIO_ID);
        usuario.setUsername("admin.sem.escola");
        usuario.setSenha("hash-irrelevante");
        usuario.setTipoUsuario(TipoUsuario.ADMINISTRADOR);
        usuario.setStatus(StatusAtivoInativo.ATIVO);

        CustomUserDetails principal = new CustomUserDetails(usuario);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private static void assertForbidden(Runnable acao) {
        try {
            acao.run();
            fail("esperava 403");
        } catch (ResponseStatusException erro) {
            assertThat(erro.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }
}
