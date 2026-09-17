package studojurata_api.config;

import java.util.List;

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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import studojurata_api.security.CustomUserDetailsService;

@Configuration
public class SecurityConfig {

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

    /**
     * Autenticação é por cookie de sessão, então o CORS precisa de
     * allowCredentials e o front precisa enviar credentials: "include".
     *
     * Aceita localhost em qualquer porta porque o Vite troca sozinho de porta
     * (5174, 5175...) quando a 5173 está ocupada — com origem fixa, o login
     * falharia com 403 sem nenhuma pista de que o problema é CORS. As origens
     * de produção precisam ser adicionadas aqui antes do deploy.
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("http://localhost:*", "http://127.0.0.1:*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Sem isso o Spring Security bloqueia o preflight (OPTIONS) antes
            // de a configuração de CORS acima ser considerada.
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/login").permitAll()

                // Conveniência de desenvolvimento: expõe a documentação de todos
                // os endpoints. Restringir ou desligar
                // (springdoc.swagger-ui.enabled=false) antes de produção.
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**",
                        "/v3/api-docs.yaml").permitAll()

                .requestMatchers("/usuarios/**").hasRole("ADMINISTRADOR")
                .requestMatchers("/escolas/**").hasRole("ADMINISTRADOR")

                // Cadastro de perfis: escrita só do Administrador, consulta para
                // qualquer autenticado.
                .requestMatchers(HttpMethod.POST, "/pessoas/**", "/alunos/**", "/professores/**",
                        "/responsaveis/**", "/responsavel-aluno/**").hasRole("ADMINISTRADOR")
                .requestMatchers(HttpMethod.PUT, "/pessoas/**", "/alunos/**", "/professores/**",
                        "/responsaveis/**", "/responsavel-aluno/**").hasRole("ADMINISTRADOR")
                .requestMatchers(HttpMethod.DELETE, "/pessoas/**", "/alunos/**", "/professores/**",
                        "/responsaveis/**", "/responsavel-aluno/**").hasRole("ADMINISTRADOR")
                // O aceite de termos é feito pelo próprio responsável.
                .requestMatchers(HttpMethod.POST, "/responsavel-aluno/*/aceitar-termos").authenticated()

                .requestMatchers(HttpMethod.GET, "/eventos/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/eventos/**").hasRole("ADMINISTRADOR")
                .requestMatchers(HttpMethod.PUT, "/eventos/**").hasRole("ADMINISTRADOR")
                .requestMatchers(HttpMethod.DELETE, "/eventos/**").hasRole("ADMINISTRADOR")

                // Listagem geral e recálculo são operações de gestão. As demais
                // consultas de nota ficam abertas a autenticados porque o
                // NotaController aplica AlunoAccessGuard (aluno só vê as próprias).
                .requestMatchers(HttpMethod.GET, "/notas").hasAnyRole("PROFESSOR", "ADMINISTRADOR")
                .requestMatchers(HttpMethod.POST, "/notas/recalcular").hasAnyRole("PROFESSOR", "ADMINISTRADOR")
                .requestMatchers(HttpMethod.DELETE, "/notas/**").hasRole("ADMINISTRADOR")
                .requestMatchers("/notas/**").authenticated()

                // Sempre por aluno específico; o GamificacaoController aplica
                // AlunoAccessGuard.
                .requestMatchers("/gamificacao/**").authenticated()

                // Gestão pedagógica: consulta para qualquer autenticado, escrita
                // só para quem gerencia o conteúdo. /horarios/** cobre o DELETE de
                // HorarioTurma, que tem rota própria fora de /turmas/**.
                .requestMatchers(HttpMethod.GET, "/cursos/**", "/turmas/**", "/horarios/**", "/disciplinas/**",
                        "/turma-disciplina/**", "/curso-disciplina/**", "/plano-ensino/**", "/conteudo-plano/**", "/plano-aula/**",
                        "/aulas/**", "/frequencia/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/cursos/**", "/turmas/**", "/horarios/**", "/disciplinas/**",
                        "/turma-disciplina/**", "/curso-disciplina/**", "/plano-ensino/**", "/conteudo-plano/**", "/plano-aula/**",
                        "/aulas/**", "/frequencia/**").hasAnyRole("PROFESSOR", "ADMINISTRADOR")
                .requestMatchers(HttpMethod.PUT, "/cursos/**", "/turmas/**", "/horarios/**", "/disciplinas/**",
                        "/turma-disciplina/**", "/curso-disciplina/**", "/plano-ensino/**", "/conteudo-plano/**", "/plano-aula/**",
                        "/aulas/**", "/frequencia/**").hasAnyRole("PROFESSOR", "ADMINISTRADOR")
                .requestMatchers(HttpMethod.DELETE, "/cursos/**", "/turmas/**", "/horarios/**", "/disciplinas/**",
                        "/turma-disciplina/**", "/curso-disciplina/**", "/plano-ensino/**", "/conteudo-plano/**", "/plano-aula/**",
                        "/aulas/**", "/frequencia/**").hasAnyRole("PROFESSOR", "ADMINISTRADOR")

                // Sem estas regras, um Aluno logado poderia se automatricular ou
                // cancelar a matrícula de outro aluno via API.
                .requestMatchers(HttpMethod.GET, "/aluno-turma/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/aluno-turma/**").hasAnyRole("PROFESSOR", "ADMINISTRADOR")
                .requestMatchers(HttpMethod.PUT, "/aluno-turma/**").hasAnyRole("PROFESSOR", "ADMINISTRADOR")

                // Montagem, moderação e lançamento de simulados são do professor;
                // o aluno só consulta e finaliza a própria tentativa.
                .requestMatchers(HttpMethod.POST, "/simulado-aluno/*/finalizar").authenticated()
                .requestMatchers(HttpMethod.GET, "/simulados/**", "/questoes/**", "/alternativas/**",
                        "/simulado-questao/**", "/simulado-aluno/**", "/questao-aluno/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/simulados/**", "/questoes/**", "/alternativas/**",
                        "/simulado-questao/**", "/simulado-aluno/**", "/questao-aluno/**")
                        .hasAnyRole("PROFESSOR", "ADMINISTRADOR")
                .requestMatchers(HttpMethod.PUT, "/simulados/**", "/questoes/**", "/alternativas/**",
                        "/simulado-questao/**", "/questao-aluno/**").hasAnyRole("PROFESSOR", "ADMINISTRADOR")
                .requestMatchers(HttpMethod.PATCH, "/simulados/**").hasAnyRole("PROFESSOR", "ADMINISTRADOR")
                // Desvincular conteúdo não é excluir a questão: precisa vir antes
                // do DELETE /questoes/**, que é só do Administrador.
                .requestMatchers(HttpMethod.DELETE, "/questoes/*/conteudos/**").hasAnyRole("PROFESSOR", "ADMINISTRADOR")
                .requestMatchers(HttpMethod.DELETE, "/simulados/**", "/questoes/**", "/alternativas/**",
                        "/simulado-questao/**", "/simulado-aluno/**", "/questao-aluno/**").hasRole("ADMINISTRADOR")

                .requestMatchers("/ia/**").hasAnyRole("PROFESSOR", "ADMINISTRADOR")

                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                // Limita sessões concorrentes; o timeout por inatividade fica em
                // server.servlet.session.timeout.
                .maximumSessions(1)
            )
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .csrf(csrf -> csrf.disable());

        return http.build();
    }
}
