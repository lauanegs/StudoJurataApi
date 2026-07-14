package studojurata_api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import studojurata_api.security.CustomUserDetailsService;

@Configuration
public class SecurityConfig {

    /**
     * Senhas são sempre persistidas com hash (BCrypt) na camada de serviço,
     * nunca em texto puro (item 10.1 da análise crítica).
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    DaoAuthenticationProvider authenticationProvider(CustomUserDetailsService userDetailsService,
                                                       PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Login é público; o restante exige autenticação.
                .requestMatchers("/auth/login").permitAll()

                // Gestão de usuários (login/senha/papel) é restrita ao Administrador.
                .requestMatchers("/usuarios/**").hasRole("ADMINISTRADOR")

                // Cadastro/edição/exclusão de perfis é restrita ao Administrador;
                // consulta (GET) fica liberada para qualquer usuário autenticado.
                .requestMatchers(HttpMethod.POST, "/pessoas/**", "/alunos/**", "/professores/**",
                        "/responsaveis/**", "/responsavel-aluno/**").hasRole("ADMINISTRADOR")
                .requestMatchers(HttpMethod.PUT, "/pessoas/**", "/alunos/**", "/professores/**",
                        "/responsaveis/**", "/responsavel-aluno/**").hasRole("ADMINISTRADOR")
                .requestMatchers(HttpMethod.DELETE, "/pessoas/**", "/alunos/**", "/professores/**",
                        "/responsaveis/**", "/responsavel-aluno/**").hasRole("ADMINISTRADOR")

                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                // Expiração de sessão (item 10.5): limita a 1 sessão ativa por usuário.
                .maximumSessions(1)
            )
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .csrf(csrf -> csrf.disable());

        return http.build();
    }
}
