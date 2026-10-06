package studojurata_api.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Value;
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
    @Value("${studojurata.cors.allowed-origins:}")
    private String origensExtras;

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
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> origens = new ArrayList<>(List.of("http://localhost:*", "http://127.0.0.1:*"));
        Arrays.stream(origensExtras.split(","))
                .map(String::trim)
                .filter(origem -> !origem.isEmpty())
                .forEach(origens::add);
        configuration.setAllowedOriginPatterns(origens);
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
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/login").permitAll()

                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**",
                        "/v3/api-docs.yaml").permitAll()

                .requestMatchers("/usuarios/**").hasRole("ADMINISTRADOR")
                .requestMatchers("/escolas/**").hasRole("ADMINISTRADOR")

                .requestMatchers(HttpMethod.GET, "/responsaveis/**").hasRole("ADMINISTRADOR")
                .requestMatchers(HttpMethod.GET, "/pessoas/**").hasAnyRole("PROFESSOR", "ADMINISTRADOR")
                .requestMatchers(HttpMethod.POST, "/pessoas/**", "/alunos/**", "/professores/**",
                        "/responsaveis/**", "/responsavel-aluno/**").hasRole("ADMINISTRADOR")
                .requestMatchers(HttpMethod.PUT, "/pessoas/**", "/alunos/**", "/professores/**",
                        "/responsaveis/**", "/responsavel-aluno/**").hasRole("ADMINISTRADOR")
                .requestMatchers(HttpMethod.DELETE, "/pessoas/**", "/alunos/**", "/professores/**",
                        "/responsaveis/**", "/responsavel-aluno/**").hasRole("ADMINISTRADOR")
                .requestMatchers(HttpMethod.GET, "/responsavel-aluno/por-responsavel/**")
                        .hasRole("ADMINISTRADOR")

                .requestMatchers(HttpMethod.GET, "/eventos/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/eventos/**").hasRole("ADMINISTRADOR")
                .requestMatchers(HttpMethod.PUT, "/eventos/**").hasRole("ADMINISTRADOR")
                .requestMatchers(HttpMethod.DELETE, "/eventos/**").hasRole("ADMINISTRADOR")

                .requestMatchers(HttpMethod.GET, "/notas").hasAnyRole("PROFESSOR", "ADMINISTRADOR")
                .requestMatchers(HttpMethod.POST, "/notas/recalcular").hasAnyRole("PROFESSOR", "ADMINISTRADOR")
                .requestMatchers(HttpMethod.DELETE, "/notas/**").hasRole("ADMINISTRADOR")
                .requestMatchers("/notas/**").authenticated()

                .requestMatchers("/gamificacao/**").authenticated()

                .requestMatchers(HttpMethod.POST, "/cursos/**", "/disciplinas/**", "/curso-disciplina/**")
                        .hasRole("ADMINISTRADOR")
                .requestMatchers(HttpMethod.PUT, "/cursos/**", "/disciplinas/**").hasRole("ADMINISTRADOR")
                .requestMatchers(HttpMethod.PATCH, "/cursos/**", "/disciplinas/**").hasRole("ADMINISTRADOR")
                .requestMatchers(HttpMethod.DELETE, "/cursos/**", "/disciplinas/**", "/curso-disciplina/**")
                        .hasRole("ADMINISTRADOR")
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

                .requestMatchers(HttpMethod.GET, "/aluno-turma/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/aluno-turma/**").hasAnyRole("PROFESSOR", "ADMINISTRADOR")
                .requestMatchers(HttpMethod.PUT, "/aluno-turma/**").hasAnyRole("PROFESSOR", "ADMINISTRADOR")

                .requestMatchers(HttpMethod.POST, "/simulado-aluno/*/finalizar").authenticated()
                .requestMatchers(HttpMethod.POST, "/simulado-aluno/*/conferir-questao").authenticated()
                .requestMatchers(HttpMethod.GET, "/simulado-aluno", "/professores/*/desempenho")
                        .hasAnyRole("PROFESSOR", "ADMINISTRADOR")
                .requestMatchers(HttpMethod.GET, "/simulados/**", "/questoes/**", "/alternativas/**",
                        "/simulado-questao/**", "/simulado-aluno/**", "/questao-aluno/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/simulados/**", "/questoes/**", "/alternativas/**",
                        "/simulado-questao/**", "/simulado-aluno/**", "/questao-aluno/**")
                        .hasAnyRole("PROFESSOR", "ADMINISTRADOR")
                .requestMatchers(HttpMethod.PUT, "/simulados/**", "/questoes/**", "/alternativas/**",
                        "/simulado-questao/**", "/questao-aluno/**").hasAnyRole("PROFESSOR", "ADMINISTRADOR")
                .requestMatchers(HttpMethod.PATCH, "/simulados/**").hasAnyRole("PROFESSOR", "ADMINISTRADOR")
                .requestMatchers(HttpMethod.DELETE, "/questoes/*/conteudos/**").hasAnyRole("PROFESSOR", "ADMINISTRADOR")
                .requestMatchers(HttpMethod.DELETE, "/simulado-questao/**").hasAnyRole("PROFESSOR", "ADMINISTRADOR")
                .requestMatchers(HttpMethod.DELETE, "/simulados/**", "/questoes/**", "/alternativas/**",
                        "/simulado-questao/**", "/simulado-aluno/**", "/questao-aluno/**").hasRole("ADMINISTRADOR")

                .requestMatchers("/ia/**").hasAnyRole("PROFESSOR", "ADMINISTRADOR")

                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .maximumSessions(1)
            )
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .csrf(csrf -> csrf.disable());

        return http.build();
    }
}
