package studojurata_api.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import studojurata_api.model.enums.StatusAtivoInativo;
import studojurata_api.model.enums.TipoUsuario;
import studojurata_api.model.institucional.Escola;
import studojurata_api.model.pessoa.Pessoa;
import studojurata_api.model.pessoa.Usuario;
import studojurata_api.repository.institucional.EscolaRepository;
import studojurata_api.repository.pessoa.PessoaRepository;
import studojurata_api.repository.pessoa.UsuarioRepository;

/**
 * Cria o primeiro administrador num banco vazio (produção), a partir das
 * variáveis ADMIN_USERNAME e ADMIN_PASSWORD. Não faz nada se as variáveis
 * faltam ou se já existe algum administrador; depois do primeiro acesso, o
 * administrador completa o cadastro (CPF, e-mail) e troca a senha pela tela de
 * usuários.
 */
@Component
@RequiredArgsConstructor
public class AdminInicialBootstrap implements CommandLineRunner {

    private static final String CPF_PROVISORIO = "000.000.000-00";

    private final UsuarioRepository usuarioRepository;
    private final PessoaRepository pessoaRepository;
    private final EscolaRepository escolaRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${studojurata.admin-inicial.username:}")
    private String username;

    @Value("${studojurata.admin-inicial.senha:}")
    private String senha;

    @Override
    @Transactional
    public void run(String... args) {
        if (username.isBlank() || senha.isBlank()) return;

        boolean jaExisteAdministrador = usuarioRepository.findAll().stream()
                .anyMatch(usuario -> usuario.getTipoUsuario() == TipoUsuario.ADMINISTRADOR);
        if (jaExisteAdministrador) return;

        Escola escola = escolaRepository.findAll().stream().findFirst().orElseGet(() -> {
            Escola nova = new Escola();
            nova.setNome("Studo Jurata");
            nova.setStatus(StatusAtivoInativo.ATIVO);
            return escolaRepository.save(nova);
        });

        Pessoa pessoa = new Pessoa();
        pessoa.setNome("Administrador");
        pessoa.setCpf(CPF_PROVISORIO);
        pessoa.setStatus(StatusAtivoInativo.ATIVO);
        pessoa = pessoaRepository.save(pessoa);

        Usuario admin = new Usuario();
        admin.setEscola(escola);
        admin.setPessoa(pessoa);
        admin.setUsername(username.trim());
        admin.setSenha(passwordEncoder.encode(senha));
        admin.setTipoUsuario(TipoUsuario.ADMINISTRADOR);
        admin.setStatus(StatusAtivoInativo.ATIVO);
        usuarioRepository.save(admin);
    }
}
