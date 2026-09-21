package studojurata_api.support;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import studojurata_api.model.Aluno;
import studojurata_api.model.Escola;
import studojurata_api.model.Pessoa;
import studojurata_api.model.Professor;
import studojurata_api.model.Usuario;
import studojurata_api.model.enums.StatusAtivoInativo;
import studojurata_api.model.enums.TipoUsuario;
import studojurata_api.security.CustomUserDetails;

/**
 * Fábrica de usuários autenticados para os testes de autorização.
 *
 * <p>Existe porque o restante do sistema não consegue montar um principal
 * autenticado sem banco de dados: {@code CustomUserDetails} encapsula a
 * entidade {@code Usuario}, que por sua vez aponta para {@code Pessoa} e,
 * conforme o perfil, para {@code Aluno} ou {@code Professor}. Aqui essas
 * entidades são construídas em memória, com ids fixos e sem persistência.
 *
 * <p>Só os atributos que as regras de autorização leem são preenchidos:
 * {@code tipoUsuario}, {@code Aluno.id} e {@code Professor.id}. Nenhuma
 * credencial real é usada — o hash de senha é um marcador sem valor.
 *
 * <p>Dois modos de uso:
 * <ul>
 *   <li>{@code autenticacaoDeXxx(...)} devolve um {@link Authentication} para
 *       uso em {@code SecurityMockMvcRequestPostProcessors.authentication(...)}
 *       nos testes de controller;</li>
 *   <li>{@code autenticarComoXxx(...)} popula o {@code SecurityContextHolder}
 *       para testes unitários que leem o contexto diretamente (guards).</li>
 * </ul>
 */
public final class AuthorizationTestSupport {

    public static final long ESCOLA_ID = 1L;
    public static final long USUARIO_ID = 500L;

    /** Pessoa usada pelo Administrador, que não tem Aluno nem Professor vinculado. */
    public static final long PESSOA_ADMIN_ID = 900L;

    private static final String SENHA_MARCADOR = "hash-irrelevante-para-autorizacao";

    private AuthorizationTestSupport() {
    }

    // --- Autenticação por perfil -------------------------------------------

    /** Aluno com vínculo ({@code Usuario.aluno}) apontando para {@code alunoId}. */
    public static Authentication autenticacaoDeAluno(long alunoId) {
        Pessoa pessoa = pessoa(alunoId);

        Aluno aluno = new Aluno();
        aluno.setId(alunoId);
        aluno.setPessoa(pessoa);

        return autenticacao(montarUsuario(TipoUsuario.ALUNO, pessoa, aluno, null));
    }

    /**
     * Aluno sem vínculo: {@code Usuario.aluno} fica nulo, cenário que o
     * {@code AlunoAccessGuard} precisa recusar em vez de deixar passar.
     */
    public static Authentication autenticacaoDeAlunoSemVinculo(long pessoaId) {
        return autenticacao(montarUsuario(TipoUsuario.ALUNO, pessoa(pessoaId), null, null));
    }

    public static Authentication autenticacaoDeProfessor(long professorId) {
        Pessoa pessoa = pessoa(professorId);

        Professor professor = new Professor();
        professor.setId(professorId);
        professor.setPessoa(pessoa);
        professor.setStatus(StatusAtivoInativo.ATIVO);

        return autenticacao(montarUsuario(TipoUsuario.PROFESSOR, pessoa, null, professor));
    }

    public static Authentication autenticacaoDeAdministrador() {
        return autenticacao(montarUsuario(TipoUsuario.ADMINISTRADOR, pessoa(PESSOA_ADMIN_ID), null, null));
    }

    // --- Conveniências para testes unitários (sem MockMvc) ------------------

    public static CustomUserDetails autenticarComoAluno(long alunoId) {
        return autenticar(autenticacaoDeAluno(alunoId));
    }

    public static CustomUserDetails autenticarComoAlunoSemVinculo(Long pessoaId) {
        return autenticar(autenticacaoDeAlunoSemVinculo(pessoaId));
    }

    public static CustomUserDetails autenticarComoProfessor(long professorId) {
        return autenticar(autenticacaoDeProfessor(professorId));
    }

    public static CustomUserDetails autenticarComoAdministrador() {
        return autenticar(autenticacaoDeAdministrador());
    }

    /** Deixa o contexto limpo para que um teste não herde a autenticação do anterior. */
    public static void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    // --- Montagem das entidades --------------------------------------------

    private static Escola escola() {
        Escola escola = new Escola();
        escola.setId(ESCOLA_ID);
        escola.setNome("Escola de Teste");
        escola.setStatus(StatusAtivoInativo.ATIVO);
        return escola;
    }

    private static Pessoa pessoa(long pessoaId) {
        Pessoa pessoa = new Pessoa();
        pessoa.setId(pessoaId);
        pessoa.setNome("Pessoa " + pessoaId);
        pessoa.setStatus(StatusAtivoInativo.ATIVO);
        return pessoa;
    }

    private static Usuario montarUsuario(TipoUsuario tipo, Pessoa pessoa, Aluno aluno, Professor professor) {
        Usuario usuario = new Usuario();
        usuario.setId(USUARIO_ID);
        usuario.setEscola(escola());
        usuario.setPessoa(pessoa);
        usuario.setUsername("usuario.teste");
        usuario.setSenha(SENHA_MARCADOR);
        usuario.setTipoUsuario(tipo);
        usuario.setStatus(StatusAtivoInativo.ATIVO);
        usuario.setAluno(aluno);
        usuario.setProfessor(professor);
        return usuario;
    }

    private static Authentication autenticacao(Usuario usuario) {
        CustomUserDetails principal = new CustomUserDetails(usuario);
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    private static CustomUserDetails autenticar(Authentication autenticacao) {
        SecurityContextHolder.getContext().setAuthentication(autenticacao);
        return (CustomUserDetails) autenticacao.getPrincipal();
    }
}
