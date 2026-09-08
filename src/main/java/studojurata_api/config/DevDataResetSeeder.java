package studojurata_api.config;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.Ordered;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import studojurata_api.ia.model.HistoricoGeracaoIA;
import studojurata_api.ia.model.RevisaoConteudo;
import studojurata_api.ia.model.enums.NivelDominio;
import studojurata_api.ia.model.enums.OrigemResultadoGeracao;
import studojurata_api.ia.repository.HistoricoGeracaoIARepository;
import studojurata_api.ia.repository.RevisaoConteudoRepository;

import studojurata_api.model.*;
import studojurata_api.model.enums.*;
import studojurata_api.model.gamificacao.PontuacaoAluno;
import studojurata_api.model.gamificacao.Skin;
import studojurata_api.model.gamificacao.SkinAluno;

import studojurata_api.repository.*;
import studojurata_api.repository.gamificacao.PontuacaoAlunoRepository;
import studojurata_api.repository.gamificacao.SkinAlunoRepository;
import studojurata_api.repository.gamificacao.SkinRepository;

/**
 * Script de reset + carga de dados reais da escola administrada (cursos de
 * tecnologia infantojuvenil: Geek Júnior, Robótica, Programação Gamificada,
 * Geek Teens).
 *
 * O QUE FAZ:
 * 1) Apaga TODOS os registros de TODAS as tabelas do banco (respeitando a
 *    ordem de dependência das FKs, sem precisar de DROP/TRUNCATE manual em SQL).
 * 2) Recria os dados reais da escola: 1 escola, 4 cursos, 2 disciplinas
 *    (Robótica e Programação Gamificada), 4 turmas (1 por curso, 1 aula
 *    semanal de 1h30, capacidade 8), 2 professores (1 titular por
 *    disciplina), 16 alunos de 7 a 14 anos (14 com matrícula ativa + 2 só no
 *    histórico — 1 concluída, 1 cancelada — cobrindo todas as situações de
 *    matrícula), responsáveis, planos de ensino/aula, aulas, frequência,
 *    simulados (alguns já respondidos, outros pendentes — "a fazer" — e
 *    questões de IA aguardando revisão do professor), notas, eventos de
 *    aula demonstrativa, gamificação e histórico do módulo de IA.
 *
 * COMO RODAR (NUNCA roda sozinho em produção — só com o profile "seed"):
 *   mvn spring-boot:run -Dspring-boot.run.profiles=seed
 *   (ou, na IDE, adicionar "seed" em "Active profiles" da run configuration)
 *
 * ATENÇÃO: isso apaga TODOS os dados existentes no banco configurado em
 * application.properties. Use apenas em ambiente de desenvolvimento/teste.
 */
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
@Profile("seed")
public class DevDataResetSeeder implements CommandLineRunner {

    private final PasswordEncoder passwordEncoder;

    // --- núcleo acadêmico ---
    private final EscolaRepository escolaRepository;
    private final CursoRepository cursoRepository;
    private final DisciplinaRepository disciplinaRepository;
    private final TurmaRepository turmaRepository;
    private final HorarioTurmaRepository horarioTurmaRepository;
    private final TurmaDisciplinaRepository turmaDisciplinaRepository;
    private final TurmaDisciplinaSubstitutoRepository turmaDisciplinaSubstitutoRepository;

    // --- pessoas / perfis ---
    private final PessoaRepository pessoaRepository;
    private final ProfessorRepository professorRepository;
    private final AlunoRepository alunoRepository;
    private final ResponsavelRepository responsavelRepository;
    private final ResponsavelAlunoRepository responsavelAlunoRepository;
    private final NotificacaoEnviadaRepository notificacaoEnviadaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AlunoTurmaRepository alunoTurmaRepository;

    // --- currículo / aulas ---
    private final PlanoEnsinoRepository planoEnsinoRepository;
    private final ConteudoPlanoRepository conteudoPlanoRepository;
    private final PlanoAulaRepository planoAulaRepository;
    private final AulaRepository aulaRepository;
    private final AulaConteudoRepository aulaConteudoRepository;
    private final FrequenciaRepository frequenciaRepository;

    // --- questões / simulados ---
    private final QuestaoRepository questaoRepository;
    private final AlternativaRepository alternativaRepository;
    private final QuestaoConteudoRepository questaoConteudoRepository;
    private final SimuladoRepository simuladoRepository;
    private final SimuladoQuestaoRepository simuladoQuestaoRepository;
    private final SimuladoAlunoRepository simuladoAlunoRepository;
    private final QuestaoAlunoRepository questaoAlunoRepository;
    private final NotaRepository notaRepository;

    // --- diversos ---
    private final EventoRepository eventoRepository;
    private final AuditLogRepository auditLogRepository;

    // --- gamificação ---
    private final SkinRepository skinRepository;
    private final PontuacaoAlunoRepository pontuacaoAlunoRepository;
    private final SkinAlunoRepository skinAlunoRepository;

    // --- IA ---
    private final RevisaoConteudoRepository revisaoConteudoRepository;
    private final HistoricoGeracaoIARepository historicoGeracaoIARepository;

    @Override
    @Transactional
    public void run(String... args) {
        limparBanco();
        semear();
    }

    // =========================================================================
    // 1) LIMPEZA — ordem do "filho" para o "pai", respeitando as FKs
    // =========================================================================
    private void limparBanco() {
        questaoAlunoRepository.deleteAllInBatch();
        skinAlunoRepository.deleteAllInBatch();
        pontuacaoAlunoRepository.deleteAllInBatch();
        simuladoAlunoRepository.deleteAllInBatch();
        simuladoQuestaoRepository.deleteAllInBatch();
        historicoGeracaoIARepository.deleteAllInBatch();
        revisaoConteudoRepository.deleteAllInBatch();
        simuladoRepository.deleteAllInBatch();
        questaoConteudoRepository.deleteAllInBatch();
        alternativaRepository.deleteAllInBatch();
        questaoRepository.deleteAllInBatch();
        frequenciaRepository.deleteAllInBatch();
        aulaConteudoRepository.deleteAllInBatch();
        aulaRepository.deleteAllInBatch();
        conteudoPlanoRepository.deleteAllInBatch();
        planoAulaRepository.deleteAllInBatch();
        planoEnsinoRepository.deleteAllInBatch();
        notaRepository.deleteAllInBatch();
        auditLogRepository.deleteAllInBatch();
        eventoRepository.deleteAllInBatch();
        notificacaoEnviadaRepository.deleteAllInBatch();
        responsavelAlunoRepository.deleteAllInBatch();
        alunoTurmaRepository.deleteAllInBatch();
        horarioTurmaRepository.deleteAllInBatch();
        turmaDisciplinaSubstitutoRepository.deleteAllInBatch();
        turmaDisciplinaRepository.deleteAllInBatch();
        usuarioRepository.deleteAllInBatch();
        alunoRepository.deleteAllInBatch();
        professorRepository.deleteAllInBatch();
        responsavelRepository.deleteAllInBatch();
        turmaRepository.deleteAllInBatch();
        disciplinaRepository.deleteAllInBatch();
        cursoRepository.deleteAllInBatch();
        pessoaRepository.deleteAllInBatch();
        skinRepository.deleteAllInBatch();
        escolaRepository.deleteAllInBatch();
    }

    /** Um aluno a matricular numa turma — usado só para reduzir repetição no semear(). */
    private record AlunoSeed(String nome, String cpf, String nascimentoIso, String username,
                              Sexo sexo, String nomeResponsavel, Parentesco parentesco) {
    }

    // =========================================================================
    // 2) CARGA DE DADOS — do "pai" para o "filho"
    // =========================================================================
    private void semear() {

        LocalDate inicioAnoLetivo = LocalDate.of(2026, 2, 2);
        LocalDate fimAnoLetivo = LocalDate.of(2026, 12, 18);

        // ---- Escola ------------------------------------------------------------
        Escola escola = new Escola();
        escola.setNome("Escola Studo Jurata");
        escola.setCnpj("12.345.678/0001-90");
        escola.setStatus(StatusAtivoInativo.ATIVO);
        escola = escolaRepository.save(escola);

        // ---- Cursos --------------------------------------------------------------
        Curso cursoGeekJunior = criarCurso(escola, "Geek Júnior",
                "Primeiros passos em tecnologia para crianças de 7 a 9 anos: robótica e programação gamificada.", 60);
        Curso cursoRobotica = criarCurso(escola, "Robótica",
                "Montagem de circuitos, sensores e lógica de automação com kits de robótica.", 60);
        Curso cursoProgGamificada = criarCurso(escola, "Programação Gamificada",
                "Lógica de programação por blocos, com jogos e desafios gamificados.", 60);
        Curso cursoGeekTeens = criarCurso(escola, "Geek Teens",
                "Robótica e programação gamificada em nível avançado para adolescentes de 12 a 14 anos.", 60);

        // ---- Disciplinas -----------------------------------------------------------
        Disciplina discRobotica = new Disciplina();
        discRobotica.setEscola(escola);
        discRobotica.setTitulo("Robótica");
        discRobotica.setStatus(StatusAtivoInativo.ATIVO);
        discRobotica = disciplinaRepository.save(discRobotica);

        Disciplina discProgGamificada = new Disciplina();
        discProgGamificada.setEscola(escola);
        discProgGamificada.setTitulo("Programação Gamificada");
        discProgGamificada.setStatus(StatusAtivoInativo.ATIVO);
        discProgGamificada = disciplinaRepository.save(discProgGamificada);

        // ---- Turmas (1 por curso, 1 aula semanal de 1h30, capacidade 8) ------------
        // Confirmado pelo usuário: nome da turma = curso + dia da semana + horário de início.
        Turma turmaGeekJunior = criarTurma(escola, cursoGeekJunior, DiaSemana.TERCA, 8, 0,
                inicioAnoLetivo, fimAnoLetivo);
        Turma turmaRobotica = criarTurma(escola, cursoRobotica, DiaSemana.QUARTA, 14, 0,
                inicioAnoLetivo, fimAnoLetivo);
        Turma turmaProgGamificada = criarTurma(escola, cursoProgGamificada, DiaSemana.QUINTA, 14, 0,
                inicioAnoLetivo, fimAnoLetivo);
        Turma turmaGeekTeens = criarTurma(escola, cursoGeekTeens, DiaSemana.SABADO, 9, 0,
                inicioAnoLetivo, fimAnoLetivo);

        // ---- Professores (1 titular por disciplina) --------------------------------
        Pessoa pessoaProfRobotica = criarPessoa("Rafael Torres Mendes", "800.000.000-01",
                LocalDate.of(1990, 4, 18), "(11) 98100-0001", "rafael.mendes@studojurata.com", Sexo.MASCULINO);
        Pessoa pessoaProfProgGamificada = criarPessoa("Juliana Prado Costa", "800.000.000-02",
                LocalDate.of(1992, 9, 7), "(11) 98100-0002", "juliana.costa@studojurata.com", Sexo.FEMININO);

        Professor profRobotica = new Professor();
        profRobotica.setPessoa(pessoaProfRobotica);
        profRobotica.setStatus(StatusAtivoInativo.ATIVO);
        profRobotica = professorRepository.save(profRobotica);

        Professor profProgGamificada = new Professor();
        profProgGamificada.setPessoa(pessoaProfProgGamificada);
        profProgGamificada.setStatus(StatusAtivoInativo.ATIVO);
        profProgGamificada = professorRepository.save(profProgGamificada);

        criarUsuario(escola, pessoaProfRobotica, "rafael.mendes", "senha123", TipoUsuario.PROFESSOR, null, profRobotica);
        criarUsuario(escola, pessoaProfProgGamificada, "juliana.costa", "senha123", TipoUsuario.PROFESSOR, null, profProgGamificada);

        // ---- Administrador (necessário para Evento.criadoPor) -----------------
        Pessoa pessoaAdmin = criarPessoa("Ana Paula Admin", "000.000.000-00",
                LocalDate.of(1980, 1, 1), "(11) 90000-0000", "admin@studojurata.com", Sexo.FEMININO);
        Usuario usuarioAdmin = criarUsuario(escola, pessoaAdmin, "admin", "admin123", TipoUsuario.ADMINISTRADOR, null, null);

        // ---- TurmaDisciplina --------------------------------------------------------
        // Confirmado pelo usuário: Robótica só tem a matéria Robótica; Programação
        // Gamificada só tem a matéria Programação Gamificada; Geek Júnior e Geek Teens
        // têm as duas.
        TurmaDisciplina tdGeekJuniorRobotica = criarTurmaDisciplina(turmaGeekJunior, discRobotica, profRobotica);
        TurmaDisciplina tdGeekJuniorProgGamificada = criarTurmaDisciplina(turmaGeekJunior, discProgGamificada, profProgGamificada);
        TurmaDisciplina tdRobotica = criarTurmaDisciplina(turmaRobotica, discRobotica, profRobotica);
        TurmaDisciplina tdProgGamificada = criarTurmaDisciplina(turmaProgGamificada, discProgGamificada, profProgGamificada);
        TurmaDisciplina tdGeekTeensRobotica = criarTurmaDisciplina(turmaGeekTeens, discRobotica, profRobotica);
        TurmaDisciplina tdGeekTeensProgGamificada = criarTurmaDisciplina(turmaGeekTeens, discProgGamificada, profProgGamificada);

        // ---- Alunos (7 a 14 anos) + Responsáveis + Matrículas ativas ---------------
        List<AlunoSeed> seedsGeekJunior = List.of(
                new AlunoSeed("Enzo Ferreira Lima", "700.000.000-01", "2019-03-14", "aluno.enzo",
                        Sexo.MASCULINO, "Marcelo Ferreira Lima (pai)", Parentesco.PAI),
                new AlunoSeed("Alice Martins Souza", "700.000.000-02", "2019-06-02", "aluno.alice",
                        Sexo.FEMININO, "Patrícia Martins Souza (mãe)", Parentesco.MAE),
                new AlunoSeed("Davi Rodrigues Alves", "700.000.000-03", "2018-04-20", "aluno.davi",
                        Sexo.MASCULINO, "Renata Rodrigues Alves (mãe)", Parentesco.MAE),
                new AlunoSeed("Sophia Cardoso Pinto", "700.000.000-04", "2017-02-11", "aluno.sophia",
                        Sexo.FEMININO, "Eduardo Cardoso Pinto (pai)", Parentesco.PAI));

        List<AlunoSeed> seedsRobotica = List.of(
                new AlunoSeed("Miguel Santos Barbosa", "700.000.000-05", "2017-05-30", "aluno.miguel",
                        Sexo.MASCULINO, "Vanessa Santos Barbosa (mãe)", Parentesco.MAE),
                new AlunoSeed("Laura Nascimento Dias", "700.000.000-06", "2016-01-18", "aluno.laura",
                        Sexo.FEMININO, "Ricardo Nascimento Dias (pai)", Parentesco.PAI),
                new AlunoSeed("Gabriel Almeida Rocha", "700.000.000-07", "2015-07-09", "aluno.gabriel",
                        Sexo.MASCULINO, "Cláudia Almeida Rocha (avó)", Parentesco.AVO),
                new AlunoSeed("Isabela Correia Teixeira", "700.000.000-08", "2014-03-25", "aluno.isabela",
                        Sexo.FEMININO, "Fábio Correia Teixeira (pai)", Parentesco.PAI));

        List<AlunoSeed> seedsProgGamificada = List.of(
                new AlunoSeed("Bernardo Vieira Castro", "700.000.000-09", "2016-06-12", "aluno.bernardo",
                        Sexo.MASCULINO, "Adriana Vieira Castro (mãe)", Parentesco.MAE),
                new AlunoSeed("Manuela Ribeiro Duarte", "700.000.000-10", "2015-02-28", "aluno.manuela",
                        Sexo.FEMININO, "Marcos Ribeiro Duarte (pai)", Parentesco.PAI),
                new AlunoSeed("Heitor Monteiro Farias", "700.000.000-11", "2014-07-04", "aluno.heitor",
                        Sexo.MASCULINO, "Simone Monteiro Farias (mãe)", Parentesco.MAE));

        List<AlunoSeed> seedsGeekTeens = List.of(
                new AlunoSeed("Yasmin Cunha Moreira", "700.000.000-12", "2014-01-09", "aluno.yasmin",
                        Sexo.FEMININO, "Tiago Cunha Moreira (pai)", Parentesco.PAI),
                new AlunoSeed("Arthur Pereira Nogueira", "700.000.000-13", "2013-05-17", "aluno.arthur",
                        Sexo.MASCULINO, "Letícia Pereira Nogueira (mãe)", Parentesco.MAE),
                new AlunoSeed("Luiza Batista Gonçalves", "700.000.000-14", "2012-03-03", "aluno.luiza",
                        Sexo.FEMININO, "Otávio Batista Gonçalves (pai)", Parentesco.PAI));

        List<Aluno> alunosGeekJunior = criarAlunos(escola, seedsGeekJunior);
        List<Aluno> alunosRobotica = criarAlunos(escola, seedsRobotica);
        List<Aluno> alunosProgGamificada = criarAlunos(escola, seedsProgGamificada);
        List<Aluno> alunosGeekTeens = criarAlunos(escola, seedsGeekTeens);

        for (Aluno aluno : alunosGeekJunior) criarMatricula(aluno, turmaGeekJunior, StatusMatricula.ATIVA, inicioAnoLetivo, null);
        for (Aluno aluno : alunosRobotica) criarMatricula(aluno, turmaRobotica, StatusMatricula.ATIVA, inicioAnoLetivo, null);
        for (Aluno aluno : alunosProgGamificada) criarMatricula(aluno, turmaProgGamificada, StatusMatricula.ATIVA, inicioAnoLetivo, null);
        for (Aluno aluno : alunosGeekTeens) criarMatricula(aluno, turmaGeekTeens, StatusMatricula.ATIVA, inicioAnoLetivo, null);

        // ---- Alunos só no histórico (cobrem CONCLUIDA e CANCELADA) -----------------
        Aluno alunoConcluido = criarAlunos(escola, List.of(
                new AlunoSeed("Théo Azevedo Ramos", "700.000.000-15", "2016-04-08", "aluno.theo",
                        Sexo.MASCULINO, "Camila Azevedo Ramos (mãe)", Parentesco.MAE))).get(0);
        Aluno alunoCancelado = criarAlunos(escola, List.of(
                new AlunoSeed("Valentina Moraes Lopes", "700.000.000-16", "2013-08-01", "aluno.valentina",
                        Sexo.FEMININO, "Bruno Moraes Lopes (pai)", Parentesco.PAI))).get(0);

        criarMatricula(alunoConcluido, turmaRobotica, StatusMatricula.CONCLUIDA,
                LocalDate.of(2025, 2, 3), LocalDate.of(2025, 12, 12));
        criarMatricula(alunoCancelado, turmaGeekTeens, StatusMatricula.CANCELADA,
                inicioAnoLetivo, LocalDate.of(2026, 5, 16));

        // ---- Plano de Ensino + Conteúdo + Plano de Aula + Aulas --------------------
        PlanoEnsino peGeekJuniorRobotica = criarPlanoEnsino(tdGeekJuniorRobotica, cursoGeekJunior,
                "Robótica - Geek Júnior");
        PlanoEnsino peGeekJuniorProgGamificada = criarPlanoEnsino(tdGeekJuniorProgGamificada, cursoGeekJunior,
                "Programação Gamificada - Geek Júnior");
        PlanoEnsino peRobotica = criarPlanoEnsino(tdRobotica, cursoRobotica, "Robótica");
        PlanoEnsino peProgGamificada = criarPlanoEnsino(tdProgGamificada, cursoProgGamificada, "Programação Gamificada");
        PlanoEnsino peGeekTeensRobotica = criarPlanoEnsino(tdGeekTeensRobotica, cursoGeekTeens,
                "Robótica - Geek Teens");
        PlanoEnsino peGeekTeensProgGamificada = criarPlanoEnsino(tdGeekTeensProgGamificada, cursoGeekTeens,
                "Programação Gamificada - Geek Teens");

        List<ConteudoPlano> conteudosGeekJuniorRobotica = criarConteudos(peGeekJuniorRobotica, "Introdução à Robótica", "Montagem de Circuitos");
        List<ConteudoPlano> conteudosGeekJuniorProgGamificada = criarConteudos(peGeekJuniorProgGamificada, "Lógica de Programação", "Estruturas de Repetição");
        List<ConteudoPlano> conteudosRobotica = criarConteudos(peRobotica, "Sensores e Atuadores", "Montagem de Circuitos");
        List<ConteudoPlano> conteudosProgGamificada = criarConteudos(peProgGamificada, "Lógica de Programação", "Estruturas de Repetição");
        List<ConteudoPlano> conteudosGeekTeensRobotica = criarConteudos(peGeekTeensRobotica, "Sensores e Atuadores", "Automação Avançada");
        List<ConteudoPlano> conteudosGeekTeensProgGamificada = criarConteudos(peGeekTeensProgGamificada, "Estruturas de Repetição", "Projeto de Jogo Final");

        List<Aula> aulasGeekJuniorRobotica = criarPlanoAulaComAulas(tdGeekJuniorRobotica, peGeekJuniorRobotica, "Aula de Robótica");
        List<Aula> aulasGeekJuniorProgGamificada = criarPlanoAulaComAulas(tdGeekJuniorProgGamificada, peGeekJuniorProgGamificada, "Aula de Programação Gamificada");
        List<Aula> aulasRobotica = criarPlanoAulaComAulas(tdRobotica, peRobotica, "Aula de Robótica");
        List<Aula> aulasProgGamificada = criarPlanoAulaComAulas(tdProgGamificada, peProgGamificada, "Aula de Programação Gamificada");
        List<Aula> aulasGeekTeensRobotica = criarPlanoAulaComAulas(tdGeekTeensRobotica, peGeekTeensRobotica, "Aula de Robótica");
        List<Aula> aulasGeekTeensProgGamificada = criarPlanoAulaComAulas(tdGeekTeensProgGamificada, peGeekTeensProgGamificada, "Aula de Programação Gamificada");

        vincularAulaConteudo(aulasGeekJuniorRobotica, conteudosGeekJuniorRobotica);
        vincularAulaConteudo(aulasGeekJuniorProgGamificada, conteudosGeekJuniorProgGamificada);
        vincularAulaConteudo(aulasRobotica, conteudosRobotica);
        vincularAulaConteudo(aulasProgGamificada, conteudosProgGamificada);
        vincularAulaConteudo(aulasGeekTeensRobotica, conteudosGeekTeensRobotica);
        vincularAulaConteudo(aulasGeekTeensProgGamificada, conteudosGeekTeensProgGamificada);

        // ---- Frequência: alunos ativos de cada turma, nas aulas daquela turma -----
        marcarFrequenciaTurma(alunosGeekJunior, concat(aulasGeekJuniorRobotica, aulasGeekJuniorProgGamificada));
        marcarFrequenciaTurma(alunosRobotica, aulasRobotica);
        marcarFrequenciaTurma(alunosProgGamificada, aulasProgGamificada);
        marcarFrequenciaTurma(alunosGeekTeens, concat(aulasGeekTeensRobotica, aulasGeekTeensProgGamificada));

        // ---- Questões (5 por turma-disciplina) + Simulados -------------------------
        List<Questao> questoesGeekJuniorRobotica = criarQuestoes(discRobotica, conteudosGeekJuniorRobotica, "Geek Júnior");
        List<Questao> questoesGeekJuniorProgGamificada = criarQuestoes(discProgGamificada, conteudosGeekJuniorProgGamificada, "Geek Júnior");
        List<Questao> questoesRobotica = criarQuestoes(discRobotica, conteudosRobotica, "Robótica");
        List<Questao> questoesProgGamificada = criarQuestoes(discProgGamificada, conteudosProgGamificada, "Programação Gamificada");
        List<Questao> questoesGeekTeensRobotica = criarQuestoes(discRobotica, conteudosGeekTeensRobotica, "Geek Teens");
        List<Questao> questoesGeekTeensProgGamificada = criarQuestoes(discProgGamificada, conteudosGeekTeensProgGamificada, "Geek Teens");

        Simulado simuladoGeekJuniorRobotica = criarSimulado("Simulado de Robótica - Geek Júnior", discRobotica, peGeekJuniorRobotica, turmaGeekJunior, questoesGeekJuniorRobotica);
        Simulado simuladoGeekJuniorProgGamificada = criarSimulado("Simulado de Programação Gamificada - Geek Júnior", discProgGamificada, peGeekJuniorProgGamificada, turmaGeekJunior, questoesGeekJuniorProgGamificada);
        Simulado simuladoRobotica = criarSimulado("Simulado de Robótica", discRobotica, peRobotica, turmaRobotica, questoesRobotica);
        Simulado simuladoProgGamificada = criarSimulado("Simulado de Programação Gamificada", discProgGamificada, peProgGamificada, turmaProgGamificada, questoesProgGamificada);
        Simulado simuladoGeekTeensRobotica = criarSimulado("Simulado de Robótica - Geek Teens", discRobotica, peGeekTeensRobotica, turmaGeekTeens, questoesGeekTeensRobotica);
        Simulado simuladoGeekTeensProgGamificada = criarSimulado("Simulado de Programação Gamificada - Geek Teens", discProgGamificada, peGeekTeensProgGamificada, turmaGeekTeens, questoesGeekTeensProgGamificada);

        // ---- SimuladoAluno: metade responde (CONCLUIDO), metade fica "a fazer" ----
        // (StatusSimuladoAluno.PENDENTE — ver Javadoc do enum: é o estado normal
        // logo após o lançamento do simulado, antes do aluno abrir a prova.)
        responderSimulado(simuladoGeekJuniorRobotica, alunosGeekJunior.get(0), questoesGeekJuniorRobotica, 4);
        responderSimulado(simuladoGeekJuniorRobotica, alunosGeekJunior.get(1), questoesGeekJuniorRobotica, 3);
        criarSimuladoAlunoPendente(simuladoGeekJuniorRobotica, alunosGeekJunior.get(2));
        criarSimuladoAlunoPendente(simuladoGeekJuniorRobotica, alunosGeekJunior.get(3));

        responderSimulado(simuladoGeekJuniorProgGamificada, alunosGeekJunior.get(2), questoesGeekJuniorProgGamificada, 5);
        criarSimuladoAlunoPendente(simuladoGeekJuniorProgGamificada, alunosGeekJunior.get(0));
        criarSimuladoAlunoPendente(simuladoGeekJuniorProgGamificada, alunosGeekJunior.get(1));
        criarSimuladoAlunoPendente(simuladoGeekJuniorProgGamificada, alunosGeekJunior.get(3));

        responderSimulado(simuladoRobotica, alunosRobotica.get(0), questoesRobotica, 5);
        responderSimulado(simuladoRobotica, alunosRobotica.get(1), questoesRobotica, 2);
        criarSimuladoAlunoPendente(simuladoRobotica, alunosRobotica.get(2));
        criarSimuladoAlunoPendente(simuladoRobotica, alunosRobotica.get(3));

        responderSimulado(simuladoProgGamificada, alunosProgGamificada.get(0), questoesProgGamificada, 3);
        criarSimuladoAlunoPendente(simuladoProgGamificada, alunosProgGamificada.get(1));
        criarSimuladoAlunoPendente(simuladoProgGamificada, alunosProgGamificada.get(2));

        responderSimulado(simuladoGeekTeensRobotica, alunosGeekTeens.get(0), questoesGeekTeensRobotica, 4);
        criarSimuladoAlunoPendente(simuladoGeekTeensRobotica, alunosGeekTeens.get(1));
        criarSimuladoAlunoPendente(simuladoGeekTeensRobotica, alunosGeekTeens.get(2));

        responderSimulado(simuladoGeekTeensProgGamificada, alunosGeekTeens.get(1), questoesGeekTeensProgGamificada, 5);
        responderSimulado(simuladoGeekTeensProgGamificada, alunosGeekTeens.get(2), questoesGeekTeensProgGamificada, 1);
        criarSimuladoAlunoPendente(simuladoGeekTeensProgGamificada, alunosGeekTeens.get(0));

        // ---- Notas (por aluno/disciplina/turma) -------------------------------------
        criarNota(alunosGeekJunior.get(0), discRobotica, turmaGeekJunior, 8.0, 1);
        criarNota(alunosGeekJunior.get(1), discRobotica, turmaGeekJunior, 6.0, 1);
        criarNota(alunosGeekJunior.get(2), discProgGamificada, turmaGeekJunior, 10.0, 1);
        criarNota(alunosRobotica.get(0), discRobotica, turmaRobotica, 10.0, 1);
        criarNota(alunosRobotica.get(1), discRobotica, turmaRobotica, 4.0, 1);
        criarNota(alunosProgGamificada.get(0), discProgGamificada, turmaProgGamificada, 6.0, 1);
        criarNota(alunosGeekTeens.get(0), discRobotica, turmaGeekTeens, 8.0, 1);
        criarNota(alunosGeekTeens.get(1), discProgGamificada, turmaGeekTeens, 10.0, 1);
        criarNota(alunosGeekTeens.get(2), discProgGamificada, turmaGeekTeens, 2.0, 1);

        // ---- Questões de IA pendentes de revisão ("simulados a revisar") -----------
        // Vinculadas a um simulado ainda em RASCUNHO por disciplina, cobrindo a tela
        // de revisão de questões geradas por IA antes de irem para um simulado real.
        Simulado revisaoRobotica = criarSimuladoRascunho("Revisão IA — Robótica", discRobotica, turmaRobotica);
        Simulado revisaoProgGamificada = criarSimuladoRascunho("Revisão IA — Programação Gamificada", discProgGamificada, turmaProgGamificada);

        Questao questaoIaRobotica1 = criarQuestaoIaPendente(discRobotica, conteudosRobotica.get(0),
                "Qual componente converte energia elétrica em movimento no robô?", NivelDificuldade.FACIL,
                "Motor", "Resistor", "LED");
        Questao questaoIaRobotica2 = criarQuestaoIaPendente(discRobotica, conteudosRobotica.get(1),
                "O que acontece se os fios do motor forem invertidos?", NivelDificuldade.MEDIA,
                "O motor gira no sentido contrário", "O motor não liga mais", "O motor gira mais rápido");
        Questao questaoIaProgGamificada1 = criarQuestaoIaPendente(discProgGamificada, conteudosProgGamificada.get(0),
                "Qual bloco repete um conjunto de comandos várias vezes?", NivelDificuldade.FACIL,
                "Bloco de repetição", "Bloco de espera", "Bloco de som");
        Questao questaoIaProgGamificada2 = criarQuestaoIaPendente(discProgGamificada, conteudosProgGamificada.get(1),
                "Um laço de repetição sem condição de parada causa qual problema?", NivelDificuldade.MEDIA,
                "Trava o programa em um loop infinito", "Deixa o programa mais rápido", "Apaga as variáveis do programa");

        vincularSimuladoQuestao(revisaoRobotica, questaoIaRobotica1, 1);
        vincularSimuladoQuestao(revisaoRobotica, questaoIaRobotica2, 2);
        vincularSimuladoQuestao(revisaoProgGamificada, questaoIaProgGamificada1, 1);
        vincularSimuladoQuestao(revisaoProgGamificada, questaoIaProgGamificada2, 2);

        criarHistoricoGeracaoIA(conteudosRobotica.get(0), discRobotica, revisaoRobotica, NivelDificuldade.FACIL,
                TipoQuestao.ALTERNATIVAS, 2, 0, 2, 0, OrigemResultadoGeracao.MISTA, "gemini-1.5-flash", 1400L);
        criarHistoricoGeracaoIA(conteudosProgGamificada.get(0), discProgGamificada, revisaoProgGamificada, NivelDificuldade.FACIL,
                TipoQuestao.ALTERNATIVAS, 2, 1, 1, 0, OrigemResultadoGeracao.FALLBACK_BANCO, "gemini-1.5-flash", 1800L);

        // ---- Eventos: aulas demonstrativas constantes + reunião -------------------
        criarEvento("Aula demonstrativa — Robótica", "Aula aberta para famílias interessadas no curso de Robótica.",
                LocalDateTime.of(2026, 7, 18, 9, 0), usuarioAdmin, true);
        criarEvento("Aula demonstrativa — Geek Júnior", "Aula aberta para famílias interessadas no curso Geek Júnior.",
                LocalDateTime.of(2026, 9, 12, 8, 0), usuarioAdmin, false);
        criarEvento("Aula demonstrativa — Programação Gamificada", "Aula aberta para famílias interessadas no curso de Programação Gamificada.",
                LocalDateTime.of(2026, 9, 19, 14, 0), usuarioAdmin, false);
        criarEvento("Aula demonstrativa — Geek Teens", "Aula aberta para famílias interessadas no curso Geek Teens.",
                LocalDateTime.of(2026, 9, 26, 9, 0), usuarioAdmin, false);
        criarEvento("Reunião de pais e mestres", "Reunião geral do semestre.",
                LocalDateTime.of(2026, 9, 5, 19, 0), usuarioAdmin, false);

        // ---- Auditoria -------------------------------------------------------------
        criarAuditLog("Nota", 1L, AcaoAuditoria.ATUALIZACAO, "admin", "total: 7.0 -> 8.0");
        criarAuditLog("SimuladoAluno", 1L, AcaoAuditoria.CRIACAO, "rafael.mendes", "tentativa finalizada pelo aluno");

        // ---- Gamificação -------------------------------------------------------------
        Skin skinClassico = criarSkin("Mascote Clássico", "Visual padrão do mascote Studo Jurata.", 0, "skins/classico.png");
        Skin skinExplorador = criarSkin("Mascote Explorador", "Traje de explorador, desbloqueado com moedas de reforço.", 50, "skins/explorador.png");
        criarSkin("Mascote Cientista", "Jaleco de cientista, para quem completa muitos simulados.", 80, "skins/cientista.png");

        criarPontuacao(alunosGeekJunior.get(0), 120);
        criarPontuacao(alunosGeekJunior.get(2), 40);
        criarPontuacao(alunosRobotica.get(0), 200);
        criarPontuacao(alunosRobotica.get(1), 10);
        criarPontuacao(alunosGeekTeens.get(1), 260);

        criarSkinAluno(alunosGeekJunior.get(0), skinClassico, true);
        criarSkinAluno(alunosRobotica.get(0), skinExplorador, true);

        // ---- Módulo de IA (revisão espaçada) ---------------------------------------
        criarRevisao(alunosGeekJunior.get(3), conteudosGeekJuniorRobotica.get(0), 1, NivelDominio.BAIXO);
        criarRevisao(alunosProgGamificada.get(1), conteudosProgGamificada.get(0), 3, NivelDominio.MEDIO);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private Curso criarCurso(Escola escola, String nome, String descricao, int cargaHorariaTotal) {
        Curso curso = new Curso();
        curso.setEscola(escola);
        curso.setNome(nome);
        curso.setDescricao(descricao);
        curso.setCargaHorariaTotal(cargaHorariaTotal);
        curso.setStatus(StatusAtivoInativo.ATIVO);
        return cursoRepository.save(curso);
    }

    /** Nome da turma = curso + dia da semana + horário de início (confirmado pelo usuário). */
    private Turma criarTurma(Escola escola, Curso curso, DiaSemana dia, int horaInicio, int minutoInicio,
                              LocalDate dataInicio, LocalDate dataFim) {
        LocalTime inicio = LocalTime.of(horaInicio, minutoInicio);
        LocalTime fim = inicio.plusMinutes(90);

        Turma turma = new Turma();
        turma.setEscola(escola);
        turma.setCurso(curso);
        turma.setTitulo(curso.getNome() + " - " + rotuloDiaSemana(dia) + " " + inicio);
        turma.setCapacidadeMaxima(8);
        turma.setStatus(StatusTurma.ATIVA);
        turma.setDataInicio(dataInicio);
        turma.setDataFim(dataFim);
        turma = turmaRepository.save(turma);

        criarHorario(turma, dia, inicio, fim);
        return turma;
    }

    private String rotuloDiaSemana(DiaSemana dia) {
        return switch (dia) {
            case SEGUNDA -> "Segunda";
            case TERCA -> "Terça";
            case QUARTA -> "Quarta";
            case QUINTA -> "Quinta";
            case SEXTA -> "Sexta";
            case SABADO -> "Sábado";
            case DOMINGO -> "Domingo";
        };
    }

    private void criarHorario(Turma turma, DiaSemana dia, LocalTime inicio, LocalTime fim) {
        HorarioTurma h = new HorarioTurma();
        h.setTurma(turma);
        h.setDiaSemana(dia);
        h.setHoraInicio(inicio);
        h.setHoraFim(fim);
        horarioTurmaRepository.save(h);
    }

    private Pessoa criarPessoa(String nome, String cpf, LocalDate nascimento, String telefone, String email, Sexo sexo) {
        Pessoa p = new Pessoa();
        p.setNome(nome);
        p.setCpf(cpf);
        p.setDataNascimento(nascimento);
        p.setTelefone(telefone);
        p.setEmail(email);
        p.setSexo(sexo);
        p.setStatus(StatusAtivoInativo.ATIVO);
        return pessoaRepository.save(p);
    }

    private Usuario criarUsuario(Escola escola, Pessoa pessoa, String username, String senhaPlana,
                                  TipoUsuario tipo, Aluno aluno, Professor professor) {
        Usuario u = new Usuario();
        u.setEscola(escola);
        u.setPessoa(pessoa);
        u.setUsername(username);
        u.setSenha(passwordEncoder.encode(senhaPlana));
        u.setTipoUsuario(tipo);
        u.setStatus(StatusAtivoInativo.ATIVO);
        u.setAluno(aluno);
        u.setProfessor(professor);
        return usuarioRepository.save(u);
    }

    private TurmaDisciplina criarTurmaDisciplina(Turma turma, Disciplina disciplina, Professor professor) {
        TurmaDisciplina td = new TurmaDisciplina();
        td.setTurma(turma);
        td.setDisciplina(disciplina);
        td.setProfessor(professor);
        td.setStatus(StatusAtivoInativo.ATIVO);
        return turmaDisciplinaRepository.save(td);
    }

    private List<Aluno> criarAlunos(Escola escola, List<AlunoSeed> seeds) {
        List<Aluno> alunos = new ArrayList<>();
        for (AlunoSeed seed : seeds) {
            alunos.add(criarAlunoComResponsavel(escola, seed));
        }
        return alunos;
    }

    private Aluno criarAlunoComResponsavel(Escola escola, AlunoSeed seed) {
        Pessoa pessoaAluno = criarPessoa(seed.nome(), seed.cpf(), LocalDate.parse(seed.nascimentoIso()),
                "(11) 93" + seed.cpf().substring(10, 12) + "-0000",
                seed.username().replace(".", "_") + "@studojurata.com", seed.sexo());

        Aluno aluno = new Aluno();
        aluno.setPessoa(pessoaAluno);
        aluno.setMatricula("MAT-" + pessoaAluno.getId());
        aluno = alunoRepository.save(aluno);

        criarUsuario(escola, pessoaAluno, seed.username(), "senha123", TipoUsuario.ALUNO, aluno, null);

        Pessoa pessoaResp = criarPessoa(seed.nomeResponsavel(), "RESP-" + pessoaAluno.getId(),
                LocalDate.of(1980, 1, 1), "(11) 94" + seed.cpf().substring(10, 12) + "-0000",
                "resp." + seed.username() + "@studojurata.com",
                seed.parentesco() == Parentesco.PAI || seed.parentesco() == Parentesco.TIO ? Sexo.MASCULINO : Sexo.FEMININO);
        Responsavel responsavel = new Responsavel();
        responsavel.setPessoa(pessoaResp);
        responsavel = responsavelRepository.save(responsavel);

        ResponsavelAluno ra = new ResponsavelAluno();
        ra.setResponsavel(responsavel);
        ra.setAluno(aluno);
        ra.setParentesco(seed.parentesco());
        ra.setAceitouTermos(true);
        ra.setDataAceite(LocalDateTime.now());
        ra.setTextoVersao("Aceito o uso dos dados do meu dependente na plataforma Studo Jurata.");
        responsavelAlunoRepository.save(ra);

        return aluno;
    }

    private void criarMatricula(Aluno aluno, Turma turma, StatusMatricula status, LocalDate dataInicio, LocalDate dataFim) {
        AlunoTurma at = new AlunoTurma();
        at.setAluno(aluno);
        at.setTurma(turma);
        at.setDataInicio(dataInicio);
        at.setDataFim(dataFim);
        at.setStatus(status);
        alunoTurmaRepository.save(at);
    }

    private PlanoEnsino criarPlanoEnsino(TurmaDisciplina td, Curso curso, String titulo) {
        PlanoEnsino pe = new PlanoEnsino();
        pe.setTurmaDisciplina(td);
        pe.setProfessor(td.getProfessor());
        pe.setCurso(curso);
        pe.setTitulo(titulo);
        pe.setCargaHoraria(60);
        pe.setEmenta("Ementa de " + titulo);
        pe.setObjetivoGeral("Desenvolver o raciocínio lógico e as habilidades práticas do curso " + curso.getNome() + ".");
        pe.setMetodologia("Aulas práticas com kits e desafios em grupo.");
        pe.setDataInicio(LocalDate.of(2026, 2, 2));
        pe.setDataFim(LocalDate.of(2026, 12, 18));
        pe.setStatus(StatusPlano.ATIVO);
        return planoEnsinoRepository.save(pe);
    }

    private List<ConteudoPlano> criarConteudos(PlanoEnsino pe, String... titulos) {
        List<ConteudoPlano> lista = new ArrayList<>();
        int ordem = 1;
        for (String titulo : titulos) {
            ConteudoPlano cp = new ConteudoPlano();
            cp.setPlanoEnsino(pe);
            cp.setTitulo(titulo);
            cp.setDescricao("Conteúdo: " + titulo);
            cp.setOrdem(ordem++);
            cp.setStatus(StatusAtivoInativo.ATIVO);
            lista.add(conteudoPlanoRepository.save(cp));
        }
        return lista;
    }

    private List<Aula> criarPlanoAulaComAulas(TurmaDisciplina td, PlanoEnsino pe, String tituloBase) {
        PlanoAula pa = new PlanoAula();
        pa.setTurmaDisciplina(td);
        pa.setPlanoEnsino(pe);
        pa.setStatus(StatusPlano.ATIVO);
        pa = planoAulaRepository.save(pa);

        List<Aula> aulas = new ArrayList<>();
        for (int i = 1; i <= 2; i++) {
            Aula aula = new Aula();
            aula.setPlanoAula(pa);
            aula.setCargaHoraria(2);
            aula.setDataPrevista(LocalDate.of(2026, 3, i));
            aula.setOrdem(i);
            aula.setTitulo(tituloBase + " " + i);
            aula.setDataPublicacao(LocalDate.of(2026, 3, i));
            aula.setObservacoes("Aula ministrada normalmente.");
            aula.setStatus(StatusAtivoInativo.ATIVO);
            aulas.add(aulaRepository.save(aula));
        }
        return aulas;
    }

    private void vincularAulaConteudo(List<Aula> aulas, List<ConteudoPlano> conteudos) {
        for (int i = 0; i < aulas.size(); i++) {
            AulaConteudo ac = new AulaConteudo();
            ac.setAula(aulas.get(i));
            ac.setConteudoPlano(conteudos.get(i % conteudos.size()));
            aulaConteudoRepository.save(ac);
        }
    }

    private void marcarFrequenciaTurma(List<Aluno> alunosDaTurma, List<Aula> aulasDaTurma) {
        for (Aula aula : aulasDaTurma) {
            for (int i = 0; i < alunosDaTurma.size(); i++) {
                // Um aluno por turma falta com justificativa, os demais presentes —
                // cobre os dois estados de frequência na tela do professor.
                boolean presente = i != alunosDaTurma.size() - 1;
                criarFrequencia(alunosDaTurma.get(i), aula, presente, presente ? null : "Atestado médico");
            }
        }
    }

    private void criarFrequencia(Aluno aluno, Aula aula, boolean presente, String justificativa) {
        Frequencia f = new Frequencia();
        f.setAluno(aluno);
        f.setAula(aula);
        f.setPresente(presente);
        f.setJustificativa(justificativa);
        frequenciaRepository.save(f);
    }

    /** Uma questão de múltipla escolha pronta pra semear: enunciado + até 3 alternativas (a 1ª é a correta). */
    private record QuestaoAlternativasSeed(String enunciado, NivelDificuldade nivel, String... alternativas) {
    }

    /** Uma afirmação de questão Verdadeiro/Falso: cada uma julgada à parte (ver AlternativaVerdadeiroFalso no front). */
    private record AfirmacaoSeed(String texto, boolean verdadeira) {
    }

    /** Uma questão Verdadeiro/Falso pronta pra semear: enunciado + até 3 afirmações independentes. */
    private record QuestaoVFSeed(String enunciado, AfirmacaoSeed... afirmacoes) {
    }

    /**
     * Banco de questões reais por disciplina (não texto placeholder) — no máximo
     * 3 alternativas por questão (MAXIMO_ALTERNATIVAS do front) e V/F com
     * afirmações cujo gabarito bate de fato com a resposta certa.
     */
    private List<Questao> criarQuestoes(Disciplina disciplina, List<ConteudoPlano> conteudos, String rotulo) {
        boolean robotica = disciplina.getTitulo().equals("Robótica");

        List<QuestaoAlternativasSeed> bancoAlternativas = robotica
                ? List.of(
                    new QuestaoAlternativasSeed(
                            "Qual componente transforma energia elétrica em movimento no robô?",
                            NivelDificuldade.FACIL, "Motor", "Resistor", "LED"),
                    new QuestaoAlternativasSeed(
                            "Qual sensor permite ao robô detectar um obstáculo à frente?",
                            NivelDificuldade.MEDIA, "Sensor ultrassônico", "Sensor de luz", "Sensor de temperatura"),
                    new QuestaoAlternativasSeed(
                            "O que acontece se os dois fios do motor forem invertidos?",
                            NivelDificuldade.DIFICIL, "O motor gira no sentido contrário", "O motor não liga mais", "O motor gira mais rápido"))
                : List.of(
                    new QuestaoAlternativasSeed(
                            "Qual bloco repete um conjunto de comandos várias vezes?",
                            NivelDificuldade.FACIL, "Bloco de repetição", "Bloco de espera", "Bloco de som"),
                    new QuestaoAlternativasSeed(
                            "O que é um algoritmo?",
                            NivelDificuldade.MEDIA, "Uma sequência de passos para resolver um problema", "Um tipo de sensor", "Um personagem do jogo"),
                    new QuestaoAlternativasSeed(
                            "Para o personagem andar só quando uma tecla for pressionada, qual bloco deve ser usado?",
                            NivelDificuldade.DIFICIL, "Bloco de condição (se)", "Bloco de repetição", "Bloco de variável"));

        List<QuestaoVFSeed> bancoVF = robotica
                ? List.of(
                    new QuestaoVFSeed("Julgue as afirmações sobre componentes eletrônicos:",
                            new AfirmacaoSeed("O resistor limita a passagem de corrente elétrica no circuito.", true),
                            new AfirmacaoSeed("A bateria é o componente que gera o movimento mecânico do robô.", false),
                            new AfirmacaoSeed("Um circuito precisa estar fechado para a corrente elétrica circular.", true)),
                    new QuestaoVFSeed("Julgue as afirmações sobre sensores e automação:",
                            new AfirmacaoSeed("O sensor ultrassônico usa som para medir distância.", true),
                            new AfirmacaoSeed("Sensores permitem que o robô reaja automaticamente ao ambiente.", true),
                            new AfirmacaoSeed("Um sensor não muda o comportamento do robô, só o motor faz isso.", false)))
                : List.of(
                    new QuestaoVFSeed("Julgue as afirmações sobre lógica de programação:",
                            new AfirmacaoSeed("Um laço de repetição sem condição de parada pode travar o programa.", true),
                            new AfirmacaoSeed("Uma variável guarda um valor que pode mudar durante o programa.", true),
                            new AfirmacaoSeed("Comandos dentro de um bloco \"se\" são executados mesmo quando a condição é falsa.", false)),
                    new QuestaoVFSeed("Julgue as afirmações sobre blocos e eventos:",
                            new AfirmacaoSeed("O bloco \"quando a bandeira verde for clicada\" inicia o programa.", true),
                            new AfirmacaoSeed("Eventos servem para o programa reagir a ações do jogador.", true),
                            new AfirmacaoSeed("Todo programa precisa ter um bloco de som para funcionar.", false)));

        List<Questao> questoes = new ArrayList<>();
        for (QuestaoAlternativasSeed seed : bancoAlternativas) {
            questoes.add(criarQuestaoAlternativas(disciplina, conteudos, rotulo, seed));
        }
        for (QuestaoVFSeed seed : bancoVF) {
            questoes.add(criarQuestaoVF(disciplina, conteudos, rotulo, seed));
        }
        return questoes;
    }

    private Questao criarQuestaoAlternativas(Disciplina disciplina, List<ConteudoPlano> conteudos, String rotulo,
                                              QuestaoAlternativasSeed seed) {
        Questao q = new Questao();
        q.setEnunciado("[" + rotulo + "] " + seed.enunciado());
        q.setTipo(TipoQuestao.ALTERNATIVAS);
        q.setDisciplina(disciplina);
        q.setNivelDificuldade(seed.nivel());
        q.setOrigem(OrigemQuestao.PROFESSOR);
        q.setStatus(StatusQuestao.APROVADA);
        q = questaoRepository.save(q);

        for (int i = 0; i < seed.alternativas().length; i++) {
            Alternativa a = new Alternativa();
            a.setQuestao(q);
            a.setTexto(seed.alternativas()[i]);
            a.setCorreta(i == 0); // a primeira do array é sempre a correta, por convenção do banco acima
            a.setOrdem(i + 1);
            alternativaRepository.save(a);
        }

        vincularQuestaoConteudo(q, conteudos);
        return q;
    }

    private Questao criarQuestaoVF(Disciplina disciplina, List<ConteudoPlano> conteudos, String rotulo, QuestaoVFSeed seed) {
        Questao q = new Questao();
        q.setEnunciado("[" + rotulo + "] " + seed.enunciado());
        q.setTipo(TipoQuestao.VERDADEIRO_FALSO);
        q.setDisciplina(disciplina);
        q.setNivelDificuldade(NivelDificuldade.MEDIA);
        q.setOrigem(OrigemQuestao.PROFESSOR);
        q.setStatus(StatusQuestao.APROVADA);
        q = questaoRepository.save(q);

        for (int i = 0; i < seed.afirmacoes().length; i++) {
            AfirmacaoSeed afirmacao = seed.afirmacoes()[i];
            Alternativa a = new Alternativa();
            a.setQuestao(q);
            a.setTexto(afirmacao.texto());
            a.setCorreta(afirmacao.verdadeira());
            a.setOrdem(i + 1);
            alternativaRepository.save(a);
        }

        vincularQuestaoConteudo(q, conteudos);
        return q;
    }

    private void vincularQuestaoConteudo(Questao questao, List<ConteudoPlano> conteudos) {
        QuestaoConteudo qc = new QuestaoConteudo();
        qc.setQuestao(questao);
        qc.setConteudoPlano(conteudos.get(0));
        questaoConteudoRepository.save(qc);
    }

    /**
     * Questão de origem IA, ainda PENDENTE — aparece na tela de revisão do
     * professor. Até 3 alternativas (MAXIMO_ALTERNATIVAS do front); a
     * primeira do array é sempre a correta.
     */
    private Questao criarQuestaoIaPendente(Disciplina disciplina, ConteudoPlano conteudo, String enunciado,
                                            NivelDificuldade nivel, String... alternativas) {
        Questao q = new Questao();
        q.setEnunciado(enunciado);
        q.setTipo(TipoQuestao.ALTERNATIVAS);
        q.setDisciplina(disciplina);
        q.setNivelDificuldade(nivel);
        q.setOrigem(OrigemQuestao.IA);
        q.setStatus(StatusQuestao.PENDENTE);
        q = questaoRepository.save(q);

        for (int i = 0; i < alternativas.length; i++) {
            Alternativa a = new Alternativa();
            a.setQuestao(q);
            a.setTexto(alternativas[i]);
            a.setCorreta(i == 0);
            a.setOrdem(i + 1);
            alternativaRepository.save(a);
        }

        QuestaoConteudo qc = new QuestaoConteudo();
        qc.setQuestao(q);
        qc.setConteudoPlano(conteudo);
        questaoConteudoRepository.save(qc);

        return q;
    }

    private Simulado criarSimulado(String titulo, Disciplina disciplina, PlanoEnsino planoEnsino, Turma turma, List<Questao> questoes) {
        Simulado s = new Simulado();
        s.setTitulo(titulo);
        s.setDisciplina(disciplina);
        s.setPlanoEnsino(planoEnsino);
        s.setTurma(turma);
        s.setTipoDestinacao(TipoDestinacaoSimulado.TODOS);
        s.setDataInicio(LocalDateTime.of(2026, 8, 20, 8, 0));
        s.setDataFim(LocalDateTime.of(2026, 9, 20, 23, 59));
        s.setTempoLimite(1800);
        s.setNotaMaxima(10.0);
        s.setQuantidadeQuestoes(questoes.size());
        s.setStatus(StatusSimulado.PUBLICADO);
        s = simuladoRepository.save(s);

        int ordem = 1;
        for (Questao q : questoes) {
            vincularSimuladoQuestao(s, q, ordem++);
        }
        return s;
    }

    /** Simulado ainda em RASCUNHO — reúne as questões de IA aguardando aprovação. */
    private Simulado criarSimuladoRascunho(String titulo, Disciplina disciplina, Turma turma) {
        Simulado s = new Simulado();
        s.setTitulo(titulo);
        s.setDisciplina(disciplina);
        s.setTurma(turma);
        s.setTipoDestinacao(TipoDestinacaoSimulado.TODOS);
        s.setNotaMaxima(10.0);
        s.setQuantidadeQuestoes(0);
        s.setStatus(StatusSimulado.RASCUNHO);
        return simuladoRepository.save(s);
    }

    private void vincularSimuladoQuestao(Simulado simulado, Questao questao, int ordem) {
        SimuladoQuestao sq = new SimuladoQuestao();
        sq.setSimulado(simulado);
        sq.setQuestao(questao);
        sq.setOrdem(ordem);
        sq.setPontuacao(2.0);
        sq.setStatus(StatusSimuladoQuestao.ATIVA);
        simuladoQuestaoRepository.save(sq);
    }

    private void responderSimulado(Simulado simulado, Aluno aluno, List<Questao> questoes, int quantidadeAcertos) {
        SimuladoAluno sa = new SimuladoAluno();
        sa.setSimulado(simulado);
        sa.setAluno(aluno);
        sa.setQuantidadeAcertos(quantidadeAcertos);
        sa.setNota(quantidadeAcertos * 2.0);
        sa.setTempoGasto(900);
        sa.setFinalizadoPorTempo(false);
        sa.setStatus(StatusSimuladoAluno.CONCLUIDO);
        sa = simuladoAlunoRepository.save(sa);

        int acertosRestantes = quantidadeAcertos;
        for (Questao questao : questoes) {
            boolean acertou = acertosRestantes > 0;
            if (acertou) acertosRestantes--;

            List<Alternativa> alternativas = alternativaRepository.findByQuestaoIdOrderByOrdem(questao.getId());

            QuestaoAluno qa = new QuestaoAluno();
            qa.setSimuladoAluno(sa);
            qa.setQuestao(questao);
            qa.setAcertou(acertou);
            qa.setRespondida(true);
            qa.setTempoResposta(90);

            if (questao.getTipo() == TipoQuestao.VERDADEIRO_FALSO) {
                // Acertou = julgou todas as afirmações certas; errou = inverte o
                // julgamento de todas, garantindo pelo menos uma errada.
                qa.setAlternativa(null);
                qa.setAlternativasVerdadeiras(alternativas.stream()
                        .filter(a -> Boolean.TRUE.equals(a.getCorreta()) == acertou)
                        .toList());
            } else {
                Alternativa escolhida = alternativas.stream()
                        .filter(a -> a.getCorreta().equals(acertou))
                        .findFirst()
                        .orElse(alternativas.isEmpty() ? null : alternativas.get(0));
                qa.setAlternativa(escolhida);
                qa.setAlternativasVerdadeiras(List.of());
            }

            questaoAlunoRepository.save(qa);
        }
    }

    /** Tentativa "a fazer" — criada no lançamento do simulado, aluno ainda não abriu a prova. */
    private void criarSimuladoAlunoPendente(Simulado simulado, Aluno aluno) {
        SimuladoAluno sa = new SimuladoAluno();
        sa.setSimulado(simulado);
        sa.setAluno(aluno);
        sa.setStatus(StatusSimuladoAluno.PENDENTE);
        simuladoAlunoRepository.save(sa);
    }

    private void criarNota(Aluno aluno, Disciplina disciplina, Turma turma, double total, int qtdSimulados) {
        Nota nota = new Nota();
        nota.setAluno(aluno);
        nota.setDisciplina(disciplina);
        nota.setTurma(turma);
        nota.setTotal(total);
        nota.setQuantidadeSimuladosConsiderados(qtdSimulados);
        notaRepository.save(nota);
    }

    private void criarEvento(String titulo, String descricao, LocalDateTime dataHorario, Usuario criadoPor, boolean concluido) {
        Evento evento = new Evento();
        evento.setTitulo(titulo);
        evento.setDescricao(descricao);
        evento.setDataHorario(dataHorario);
        evento.setConcluido(concluido);
        evento.setCriadoPor(criadoPor);
        eventoRepository.save(evento);
    }

    private void criarAuditLog(String entidade, Long entidadeId, AcaoAuditoria acao, String usuario, String detalhes) {
        AuditLog log = new AuditLog();
        log.setEntidade(entidade);
        log.setEntidadeId(entidadeId);
        log.setAcao(acao);
        log.setUsuario(usuario);
        log.setDetalhes(detalhes);
        auditLogRepository.save(log);
    }

    private Skin criarSkin(String nome, String descricao, int custoMoedas, String urlAsset) {
        Skin skin = new Skin();
        skin.setNome(nome);
        skin.setDescricao(descricao);
        skin.setCustoMoedas(custoMoedas);
        skin.setUrlAsset(urlAsset);
        skin.setDisponivel(true);
        return skinRepository.save(skin);
    }

    private void criarPontuacao(Aluno aluno, int moedas) {
        PontuacaoAluno p = new PontuacaoAluno();
        p.setAluno(aluno);
        p.setMoedas(moedas);
        pontuacaoAlunoRepository.save(p);
    }

    private void criarSkinAluno(Aluno aluno, Skin skin, boolean ativa) {
        SkinAluno sa = new SkinAluno();
        sa.setAluno(aluno);
        sa.setSkin(skin);
        sa.setDataAquisicao(LocalDateTime.now());
        sa.setAtiva(ativa);
        skinAlunoRepository.save(sa);
    }

    private void criarRevisao(Aluno aluno, ConteudoPlano conteudo, int quantidadeReforcos, NivelDominio nivel) {
        RevisaoConteudo r = new RevisaoConteudo();
        r.setAluno(aluno);
        r.setConteudoPlano(conteudo);
        r.setQuantidadeReforcos(quantidadeReforcos);
        r.setDataUltimoReforco(LocalDate.now());
        r.setDataProximoReforco(LocalDate.now().plusDays((long) Math.pow(2, quantidadeReforcos)));
        r.setNivelDominio(nivel);
        revisaoConteudoRepository.save(r);
    }

    private void criarHistoricoGeracaoIA(ConteudoPlano conteudo, Disciplina disciplina, Simulado simulado,
                                          NivelDificuldade nivel, TipoQuestao tipo, int solicitada, int reaproveitada,
                                          int gerada, int fallback, OrigemResultadoGeracao origem, String modelo, long tempoMs) {
        HistoricoGeracaoIA h = new HistoricoGeracaoIA();
        h.setConteudoPlano(conteudo);
        h.setDisciplina(disciplina);
        h.setSimulado(simulado);
        h.setNivelDificuldade(nivel);
        h.setTipoQuestao(tipo);
        h.setQuantidadeSolicitada(solicitada);
        h.setQuantidadeReaproveitadaCache(reaproveitada);
        h.setQuantidadeGeradaGemini(gerada);
        h.setQuantidadeFallbackBanco(fallback);
        h.setOrigemResultado(origem);
        h.setModeloUtilizado(modelo);
        h.setTempoRespostaMs(tempoMs);
        h.setDataGeracao(LocalDateTime.now());
        historicoGeracaoIARepository.save(h);
    }

    @SafeVarargs
    private final List<Aula> concat(List<Aula>... listas) {
        List<Aula> resultado = new ArrayList<>();
        for (List<Aula> lista : listas) {
            resultado.addAll(lista);
        }
        return resultado;
    }
}
