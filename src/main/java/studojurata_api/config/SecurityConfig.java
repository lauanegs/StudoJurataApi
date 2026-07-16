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

                // Swagger/OpenAPI liberado sem autenticação — conveniência de
                // ambiente de desenvolvimento (o "Try it out" das rotas
                // protegidas continuará exigindo sessão logada). ATENÇÃO:
                // antes de subir para produção, avalie restringir isso (ex.:
                // hasRole("ADMINISTRADOR") ou springdoc.swagger-ui.enabled=false
                // em application-prod.properties), pois expõe publicamente a
                // documentação de todos os endpoints da API.
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**",
                        "/v3/api-docs.yaml").permitAll()

                // Gestão de usuários (login/senha/papel) e de escolas (tenant, item
                // 9.1) é restrita ao Administrador.
                .requestMatchers("/usuarios/**").hasRole("ADMINISTRADOR")
                .requestMatchers("/escolas/**").hasRole("ADMINISTRADOR")
                .requestMatchers("/audit-log/**").hasRole("ADMINISTRADOR")

                // Cadastro/edição/exclusão de perfis é restrita ao Administrador;
                // consulta (GET) fica liberada para qualquer usuário autenticado.
                .requestMatchers(HttpMethod.POST, "/pessoas/**", "/alunos/**", "/professores/**",
                        "/responsaveis/**", "/responsavel-aluno/**").hasRole("ADMINISTRADOR")
                .requestMatchers(HttpMethod.PUT, "/pessoas/**", "/alunos/**", "/professores/**",
                        "/responsaveis/**", "/responsavel-aluno/**").hasRole("ADMINISTRADOR")
                .requestMatchers(HttpMethod.DELETE, "/pessoas/**", "/alunos/**", "/professores/**",
                        "/responsaveis/**", "/responsavel-aluno/**").hasRole("ADMINISTRADOR")
                // Checkbox de aceite de consentimento (item 10.3) pode ser feito por
                // qualquer usuário autenticado (o próprio responsável).
                .requestMatchers(HttpMethod.POST, "/responsavel-aluno/*/aceitar-termos").authenticated()

                // Eventos (item 2.8): apenas o Administrador cria/edita/exclui,
                // conforme o Documento de Interfaces; consulta liberada a todos.
                .requestMatchers(HttpMethod.GET, "/eventos/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/eventos/**").hasRole("ADMINISTRADOR")
                .requestMatchers(HttpMethod.PUT, "/eventos/**").hasRole("ADMINISTRADOR")
                .requestMatchers(HttpMethod.DELETE, "/eventos/**").hasRole("ADMINISTRADOR")

                // Notas (item 1.2/2.13 + correção 2.1 da Terceira Análise —
                // IDOR): listagem geral e recálculo são operações de gestão,
                // não consulta do próprio aluno, então ficam restritas a
                // quem administra notas; exclusão fica só com o Admin. Os
                // demais endpoints (buscar por id, histórico por aluno)
                // continuam liberados a qualquer autenticado, mas o
                // NotaController garante via AlunoAccessGuard que um Aluno
                // só veja as suas próprias notas.
                .requestMatchers(HttpMethod.GET, "/notas").hasAnyRole("PROFESSOR", "ADMINISTRADOR")
                .requestMatchers(HttpMethod.POST, "/notas/recalcular").hasAnyRole("PROFESSOR", "ADMINISTRADOR")
                .requestMatchers(HttpMethod.DELETE, "/notas/**").hasRole("ADMINISTRADOR")
                .requestMatchers("/notas/**").authenticated()

                // Gamificação (item 8.1/8.2): sempre por aluno específico, nunca
                // uma listagem comparativa — liberado a qualquer autenticado,
                // mas o GamificacaoController garante via AlunoAccessGuard
                // (correção 2.1 da Terceira Análise) que um Aluno só acesse a
                // própria pontuação/skins, nunca as de outro aluno.
                .requestMatchers("/gamificacao/**").authenticated()

                // Correção 2.5 da Terceira Análise Crítica: gestão pedagógica
                // (cursos, turmas e seus horários semanais, disciplinas,
                // vínculo turma-disciplina, planos de ensino/aula, conteúdo,
                // aulas e frequência) não tinha nenhuma regra própria e caía
                // em anyRequest().authenticated() — ou seja, um Aluno logado
                // podia criar/editar/excluir esses registros via API.
                // Consulta (GET) continua liberada a qualquer autenticado;
                // escrita fica restrita a quem efetivamente gerencia esse
                // conteúdo (Professor/Admin). /horarios/** cobre o DELETE de
                // HorarioTurma (rota própria, fora de /turmas/**).
                .requestMatchers(HttpMethod.GET, "/cursos/**", "/turmas/**", "/horarios/**", "/disciplinas/**",
                        "/turma-disciplina/**", "/plano-ensino/**", "/conteudo-plano/**", "/plano-aula/**",
                        "/aulas/**", "/aula-conteudo/**", "/frequencia/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/cursos/**", "/turmas/**", "/horarios/**", "/disciplinas/**",
                        "/turma-disciplina/**", "/plano-ensino/**", "/conteudo-plano/**", "/plano-aula/**",
                        "/aulas/**", "/aula-conteudo/**", "/frequencia/**").hasAnyRole("PROFESSOR", "ADMINISTRADOR")
                .requestMatchers(HttpMethod.PUT, "/cursos/**", "/turmas/**", "/horarios/**", "/disciplinas/**",
                        "/turma-disciplina/**", "/plano-ensino/**", "/conteudo-plano/**", "/plano-aula/**",
                        "/aulas/**", "/aula-conteudo/**", "/frequencia/**").hasAnyRole("PROFESSOR", "ADMINISTRADOR")
                .requestMatchers(HttpMethod.DELETE, "/cursos/**", "/turmas/**", "/horarios/**", "/disciplinas/**",
                        "/turma-disciplina/**", "/plano-ensino/**", "/conteudo-plano/**", "/plano-aula/**",
                        "/aulas/**", "/aula-conteudo/**", "/frequencia/**").hasAnyRole("PROFESSOR", "ADMINISTRADOR")

                // Módulo de simulados: montagem/moderação/lançamento é tarefa do
                // professor (ou administrador); o aluno só consulta (GET) e
                // finaliza a própria tentativa (POST /simulado-aluno/{id}/finalizar).
                .requestMatchers(HttpMethod.POST, "/simulado-aluno/*/finalizar").authenticated()
                .requestMatchers(HttpMethod.GET, "/simulados/**", "/questoes/**", "/alternativas/**",
                        "/simulado-questao/**", "/simulado-aluno/**", "/questao-aluno/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/simulados/**", "/questoes/**", "/alternativas/**",
                        "/simulado-questao/**", "/simulado-aluno/**", "/questao-aluno/**")
                        .hasAnyRole("PROFESSOR", "ADMINISTRADOR")
                .requestMatchers(HttpMethod.PUT, "/simulados/**", "/questoes/**", "/alternativas/**",
                        "/simulado-questao/**", "/questao-aluno/**").hasAnyRole("PROFESSOR", "ADMINISTRADOR")
                .requestMatchers(HttpMethod.DELETE, "/simulados/**", "/questoes/**", "/alternativas/**",
                        "/simulado-questao/**", "/simulado-aluno/**", "/questao-aluno/**").hasRole("ADMINISTRADOR")

                // Módulo de IA: geração, revisão de conteúdo e histórico são
                // ferramentas de gestão pedagógica (professor/administrador);
                // recomendações também, já que hoje alimentam a decisão do
                // professor de gerar (ou não) um novo simulado (item 1.4).
                .requestMatchers("/ia/**").hasAnyRole("PROFESSOR", "ADMINISTRADOR")

                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                // maximumSessions(1) limita sessões concorrentes (1 login ativo por
                // usuário) — é complementar, não é timeout por inatividade. A
                // expiração de sessão real (item 5.3/10.5 da Segunda Análise
                // Crítica) está configurada em server.servlet.session.timeout
                // (application.properties).
                .maximumSessions(1)
            )
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .csrf(csrf -> csrf.disable());

        return http.build();
    }
}
