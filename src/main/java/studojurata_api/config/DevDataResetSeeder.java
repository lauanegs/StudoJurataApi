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
 * Script de reset + carga de dados de demonstração.
 *
 * O QUE FAZ:
 * 1) Apaga TODOS os registros de TODAS as tabelas do banco (respeitando a
 *    ordem de dependência das FKs, sem precisar de DROP/TRUNCATE manual em SQL).
 * 2) Recria um conjunto mínimo e consistente de dados para cada entidade:
 *    1 escola, 1 curso, 2 disciplinas, 2 turmas, 2 professores, 4 alunos
 *    (2 por turma), 1 responsável por aluno, 1 simulado por turma com
 *    5 questões cada, além de aulas, frequência, notas, gamificação,
 *    eventos, auditoria e registros do módulo de IA.
 *
 * COMO RODAR (NUNCA roda sozinho em produção — só com o profile "seed"):
 *   mvn spring-boot:run -Dspring-boot.run.profiles=seed
 *   (ou, na IDE, adicionar "seed" em "Active profiles" da run configuration)
 *
 * ATENÇÃO: isso apaga TODOS os dados existentes no banco configurado em
 * application.properties. Use apenas em ambiente de desenvolvimento/teste.
 *
 * Correção de auditoria: até então esta classe não tinha nenhum guard de
 * profile — sendo um CommandLineRunner comum, o Spring Boot executava
 * run() (limpar + semear o banco inteiro) em TODA subida da aplicação,
 * independentemente de profile ativo, contradizendo o parágrafo acima.
 * @Profile("seed") agora faz o guard ser real: sem "-Dspring-boot.run.profiles=seed"
 * (ou "seed" em "Active profiles"), este seeder simplesmente não é
 * instanciado pelo Spring, e o banco não é tocado no boot normal.
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

    // =========================================================================
    // 2) CARGA DE DADOS — do "pai" para o "filho"
    // =========================================================================
    private void semear() {

        // ---- Escola / Curso / Disciplinas ----------------------------------
        Escola escola = new Escola();
        escola.setNome("Escola Studo Jurata");
        escola.setCnpj("12.345.678/0001-90");
        escola.setStatus(StatusAtivoInativo.ATIVO);
        escola = escolaRepository.save(escola);

        Curso curso = new Curso();
        curso.setEscola(escola);
        curso.setNome("Preparatório ENEM");
        curso.setDescricao("Curso preparatório para o Exame Nacional do Ensino Médio.");
        curso.setCargaHorariaTotal(400);
        curso.setStatus(StatusAtivoInativo.ATIVO);
        curso = cursoRepository.save(curso);

        Disciplina matematica = new Disciplina();
        matematica.setEscola(escola);
        matematica.setTitulo("Matemática");
        matematica.setStatus(StatusAtivoInativo.ATIVO);
        matematica = disciplinaRepository.save(matematica);

        Disciplina portugues = new Disciplina();
        portugues.setEscola(escola);
        portugues.setTitulo("Português");
        portugues.setStatus(StatusAtivoInativo.ATIVO);
        portugues = disciplinaRepository.save(portugues);

        // ---- Turmas + Horários ----------------------------------------------
        Turma turmaA = new Turma();
        turmaA.setEscola(escola);
        turmaA.setCurso(curso);
        turmaA.setTitulo("Turma A - Matutino");
        turmaA.setCapacidadeMaxima(30);
        turmaA.setStatus(StatusTurma.ATIVA);
        turmaA.setDataInicio(LocalDate.of(2026, 2, 1));
        turmaA.setDataFim(LocalDate.of(2026, 12, 15));
        turmaA = turmaRepository.save(turmaA);

        Turma turmaB = new Turma();
        turmaB.setEscola(escola);
        turmaB.setCurso(curso);
        turmaB.setTitulo("Turma B - Vespertino");
        turmaB.setCapacidadeMaxima(30);
        turmaB.setStatus(StatusTurma.ATIVA);
        turmaB.setDataInicio(LocalDate.of(2026, 2, 1));
        turmaB.setDataFim(LocalDate.of(2026, 12, 15));
        turmaB = turmaRepository.save(turmaB);

        criarHorario(turmaA, DiaSemana.SEGUNDA, 8, 0, 10, 0);
        criarHorario(turmaA, DiaSemana.QUARTA, 8, 0, 10, 0);
        criarHorario(turmaB, DiaSemana.TERCA, 14, 0, 16, 0);
        criarHorario(turmaB, DiaSemana.QUINTA, 14, 0, 16, 0);

        // ---- Professores ------------------------------------------------------
        Pessoa pessoaProf1 = criarPessoa("João Carlos Silva", "111.111.111-11",
                LocalDate.of(1985, 3, 12), "(11) 91111-1111", "joao.silva@studojurata.com", Sexo.MASCULINO);
        Pessoa pessoaProf2 = criarPessoa("Maria Fernanda Souza", "222.222.222-22",
                LocalDate.of(1988, 7, 25), "(11) 92222-2222", "maria.souza@studojurata.com", Sexo.FEMININO);

        Professor professor1 = new Professor();
        professor1.setPessoa(pessoaProf1);
        professor1.setStatus(StatusAtivoInativo.ATIVO);
        professor1 = professorRepository.save(professor1);

        Professor professor2 = new Professor();
        professor2.setPessoa(pessoaProf2);
        professor2.setStatus(StatusAtivoInativo.ATIVO);
        professor2 = professorRepository.save(professor2);

        Usuario usuarioProf1 = criarUsuario(escola, pessoaProf1, "joao.silva", "senha123", TipoUsuario.PROFESSOR, null, professor1);
        Usuario usuarioProf2 = criarUsuario(escola, pessoaProf2, "maria.souza", "senha123", TipoUsuario.PROFESSOR, null, professor2);

        // ---- Administrador (necessário para Evento.criadoPor) -----------------
        Pessoa pessoaAdmin = criarPessoa("Ana Paula Admin", "000.000.000-00",
                LocalDate.of(1980, 1, 1), "(11) 90000-0000", "admin@studojurata.com", Sexo.FEMININO);
        Usuario usuarioAdmin = criarUsuario(escola, pessoaAdmin, "admin", "admin123", TipoUsuario.ADMINISTRADOR, null, null);

        // ---- TurmaDisciplina (2 disciplinas x 2 turmas) ------------------------
        TurmaDisciplina tdA_mat = criarTurmaDisciplina(turmaA, matematica, professor1);
        TurmaDisciplina tdA_port = criarTurmaDisciplina(turmaA, portugues, professor2);
        TurmaDisciplina tdB_mat = criarTurmaDisciplina(turmaB, matematica, professor1);
        TurmaDisciplina tdB_port = criarTurmaDisciplina(turmaB, portugues, professor2);

        // ---- Alunos (2 por turma = 4) + Responsáveis (1 por aluno) -----------
        Aluno alunoA1 = criarAlunoComResponsavel(escola, "Pedro Henrique Lima", "333.333.333-33",
                "2009-05-10", "aluno.pedro", "Carlos Lima (pai)", Parentesco.PAI);
        Aluno alunoA2 = criarAlunoComResponsavel(escola, "Beatriz Costa Almeida", "444.444.444-44",
                "2009-08-22", "aluno.beatriz", "Fernanda Almeida (mãe)", Parentesco.MAE);
        Aluno alunoB1 = criarAlunoComResponsavel(escola, "Lucas Gabriel Oliveira", "555.555.555-55",
                "2008-11-02", "aluno.lucas", "Roberto Oliveira (pai)", Parentesco.PAI);
        Aluno alunoB2 = criarAlunoComResponsavel(escola, "Camila Ribeiro Santos", "666.666.666-66",
                "2008-12-30", "aluno.camila", "Juliana Santos (mãe)", Parentesco.MAE);

        criarMatricula(alunoA1, turmaA);
        criarMatricula(alunoA2, turmaA);
        criarMatricula(alunoB1, turmaB);
        criarMatricula(alunoB2, turmaB);

        // ---- Plano de Ensino + Conteúdo + Plano de Aula + Aulas ----------------
        PlanoEnsino peA_mat = criarPlanoEnsino(tdA_mat, curso, "Matemática Básica - Turma A");
        PlanoEnsino peA_port = criarPlanoEnsino(tdA_port, curso, "Redação e Interpretação - Turma A");
        PlanoEnsino peB_mat = criarPlanoEnsino(tdB_mat, curso, "Matemática Básica - Turma B");
        PlanoEnsino peB_port = criarPlanoEnsino(tdB_port, curso, "Redação e Interpretação - Turma B");

        List<ConteudoPlano> conteudosA_mat = criarConteudos(peA_mat, "Funções", "Geometria Plana");
        List<ConteudoPlano> conteudosA_port = criarConteudos(peA_port, "Interpretação de Texto", "Gramática");
        List<ConteudoPlano> conteudosB_mat = criarConteudos(peB_mat, "Funções", "Geometria Plana");
        List<ConteudoPlano> conteudosB_port = criarConteudos(peB_port, "Interpretação de Texto", "Gramática");

        List<Aula> aulasA_mat = criarPlanoAulaComAulas(tdA_mat, peA_mat, "Aula de Matemática");
        List<Aula> aulasA_port = criarPlanoAulaComAulas(tdA_port, peA_port, "Aula de Português");
        List<Aula> aulasB_mat = criarPlanoAulaComAulas(tdB_mat, peB_mat, "Aula de Matemática");
        List<Aula> aulasB_port = criarPlanoAulaComAulas(tdB_port, peB_port, "Aula de Português");

        // Vincula 1 conteúdo a cada aula (AulaConteudo)
        vincularAulaConteudo(aulasA_mat, conteudosA_mat);
        vincularAulaConteudo(aulasA_port, conteudosA_port);
        vincularAulaConteudo(aulasB_mat, conteudosB_mat);
        vincularAulaConteudo(aulasB_port, conteudosB_port);

        // ---- Frequência: os 2 alunos de cada turma nas aulas daquela turma -----
        for (Aula aula : concat(aulasA_mat, aulasA_port)) {
            criarFrequencia(alunoA1, aula, true, null);
            criarFrequencia(alunoA2, aula, true, null);
        }
        for (Aula aula : concat(aulasB_mat, aulasB_port)) {
            criarFrequencia(alunoB1, aula, true, null);
            criarFrequencia(alunoB2, aula, false, "Atestado médico");
        }

        // ---- Questões (5 por turma) + Alternativas + QuestaoConteudo ----------
        List<Questao> questoesA = criarQuestoes(matematica, conteudosA_mat, "Turma A");
        List<Questao> questoesB = criarQuestoes(portugues, conteudosB_port, "Turma B");

        // ---- Simulado (1 por turma, 5 questões cada) ---------------------------
        Simulado simuladoA = criarSimulado("Simulado 1 - Matemática", matematica, peA_mat, turmaA, questoesA);
        Simulado simuladoB = criarSimulado("Simulado 1 - Português", portugues, peB_port, turmaB, questoesB);

        // ---- SimuladoAluno + QuestaoAluno (respostas) --------------------------
        responderSimulado(simuladoA, alunoA1, questoesA, 4);
        responderSimulado(simuladoA, alunoA2, questoesA, 3);
        responderSimulado(simuladoB, alunoB1, questoesB, 5);
        responderSimulado(simuladoB, alunoB2, questoesB, 2);

        // ---- Notas (recalculadas "manualmente" a partir do simulado) ----------
        criarNota(alunoA1, matematica, turmaA, 8.0, 1);
        criarNota(alunoA2, matematica, turmaA, 6.0, 1);
        criarNota(alunoB1, portugues, turmaB, 10.0, 1);
        criarNota(alunoB2, portugues, turmaB, 4.0, 1);

        // ---- Eventos -------------------------------------------------------------
        criarEvento("Reunião de pais e mestres", "Reunião geral do 1º bimestre.",
                LocalDateTime.of(2026, 8, 15, 19, 0), usuarioAdmin);
        criarEvento("Semana de provas", "Semana de aplicação dos simulados bimestrais.",
                LocalDateTime.of(2026, 8, 25, 8, 0), usuarioAdmin);

        // ---- Auditoria -------------------------------------------------------------
        criarAuditLog("Nota", 1L, AcaoAuditoria.ATUALIZACAO, "admin", "total: 7.0 -> 8.0");
        criarAuditLog("SimuladoAluno", 1L, AcaoAuditoria.CRIACAO, "joao.silva", "tentativa finalizada pelo aluno");

        // ---- Gamificação -------------------------------------------------------------
        Skin skinClassico = criarSkin("Mascote Clássico", "Visual padrão do mascote Studo Jurata.", 0, "skins/classico.png");
        Skin skinExplorador = criarSkin("Mascote Explorador", "Traje de explorador, desbloqueado com moedas de reforço.", 50, "skins/explorador.png");
        criarSkin("Mascote Cientista", "Jaleco de cientista, para quem completa muitos simulados.", 80, "skins/cientista.png");

        criarPontuacao(alunoA1, 120);
        criarPontuacao(alunoA2, 40);
        criarPontuacao(alunoB1, 200);
        criarPontuacao(alunoB2, 10);

        criarSkinAluno(alunoA1, skinClassico, true);
        criarSkinAluno(alunoB1, skinExplorador, true);

        // ---- Módulo de IA (histórico de geração + revisão espaçada) -------------
        criarRevisao(alunoA2, conteudosA_mat.get(0), 1, NivelDominio.BAIXO);
        criarRevisao(alunoB2, conteudosB_port.get(0), 3, NivelDominio.MEDIO);

        criarHistoricoGeracaoIA(conteudosA_mat.get(0), matematica, simuladoA, NivelDificuldade.MEDIA,
                TipoQuestao.ALTERNATIVAS, 5, 2, 3, 0, OrigemResultadoGeracao.MISTA, "gemini-1.5-flash", 1200L);
        criarHistoricoGeracaoIA(conteudosB_port.get(0), portugues, simuladoB, NivelDificuldade.FACIL,
                TipoQuestao.VERDADEIRO_FALSO, 5, 0, 4, 1, OrigemResultadoGeracao.FALLBACK_BANCO, "gemini-1.5-flash", 2100L);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private void criarHorario(Turma turma, DiaSemana dia, int hIni, int mIni, int hFim, int mFim) {
        HorarioTurma h = new HorarioTurma();
        h.setTurma(turma);
        h.setDiaSemana(dia);
        h.setHoraInicio(LocalTime.of(hIni, mIni));
        h.setHoraFim(LocalTime.of(hFim, mFim));
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

    private Aluno criarAlunoComResponsavel(Escola escola, String nomeAluno, String cpfAluno, String nascimentoIso,
                                            String username, String nomeResponsavel, Parentesco parentesco) {
        Pessoa pessoaAluno = criarPessoa(nomeAluno, cpfAluno, LocalDate.parse(nascimentoIso),
                "(11) 93333-0000", username.replace(".", "_") + "@studojurata.com", Sexo.MASCULINO);

        Aluno aluno = new Aluno();
        aluno.setPessoa(pessoaAluno);
        aluno.setMatricula("MAT-" + pessoaAluno.getId());
        aluno = alunoRepository.save(aluno);

        criarUsuario(escola, pessoaAluno, username, "senha123", TipoUsuario.ALUNO, aluno, null);

        // Responsável (1 por aluno)
        Pessoa pessoaResp = criarPessoa(nomeResponsavel, "RESP-" + pessoaAluno.getId(),
                LocalDate.of(1980, 1, 1), "(11) 94444-0000", "resp." + username + "@studojurata.com", Sexo.FEMININO);
        Responsavel responsavel = new Responsavel();
        responsavel.setPessoa(pessoaResp);
        responsavel = responsavelRepository.save(responsavel);

        ResponsavelAluno ra = new ResponsavelAluno();
        ra.setResponsavel(responsavel);
        ra.setAluno(aluno);
        ra.setParentesco(parentesco);
        ra.setAceitouTermos(true);
        ra.setDataAceite(LocalDateTime.now());
        ra.setTextoVersao("Aceito o uso dos dados do meu dependente na plataforma Studo Jurata.");
        responsavelAlunoRepository.save(ra);

        return aluno;
    }

    private void criarMatricula(Aluno aluno, Turma turma) {
        AlunoTurma at = new AlunoTurma();
        at.setAluno(aluno);
        at.setTurma(turma);
        at.setDataInicio(turma.getDataInicio());
        at.setStatus(StatusMatricula.ATIVA);
        alunoTurmaRepository.save(at);
    }

    private PlanoEnsino criarPlanoEnsino(TurmaDisciplina td, Curso curso, String titulo) {
        PlanoEnsino pe = new PlanoEnsino();
        pe.setTurmaDisciplina(td);
        pe.setCurso(curso);
        pe.setTitulo(titulo);
        pe.setCargaHoraria(80);
        pe.setEmenta("Ementa de " + titulo);
        pe.setObjetivoGeral("Preparar o aluno para o ENEM.");
        pe.setMetodologia("Aulas expositivas + simulados.");
        pe.setDataInicio(LocalDate.of(2026, 2, 1));
        pe.setDataFim(LocalDate.of(2026, 12, 15));
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

    private void criarFrequencia(Aluno aluno, Aula aula, boolean presente, String justificativa) {
        Frequencia f = new Frequencia();
        f.setAluno(aluno);
        f.setAula(aula);
        f.setPresente(presente);
        f.setJustificativa(justificativa);
        frequenciaRepository.save(f);
    }

    private List<Questao> criarQuestoes(Disciplina disciplina, List<ConteudoPlano> conteudos, String rotulo) {
        List<Questao> questoes = new ArrayList<>();

        // 3 questões de múltipla escolha (4 alternativas cada, 1 correta)
        for (int i = 1; i <= 3; i++) {
            Questao q = new Questao();
            q.setEnunciado("[" + rotulo + "] Questão de múltipla escolha nº " + i);
            q.setTipo(TipoQuestao.ALTERNATIVAS);
            q.setDisciplina(disciplina);
            q.setNivelDificuldade(i == 1 ? NivelDificuldade.FACIL : i == 2 ? NivelDificuldade.MEDIA : NivelDificuldade.DIFICIL);
            q.setOrigem(OrigemQuestao.PROFESSOR);
            q.setStatus(StatusQuestao.APROVADA);
            q = questaoRepository.save(q);

            for (int alt = 1; alt <= 4; alt++) {
                Alternativa a = new Alternativa();
                a.setQuestao(q);
                a.setTexto("Alternativa " + (char) ('A' + alt - 1));
                a.setCorreta(alt == 1);
                a.setOrdem(alt);
                alternativaRepository.save(a);
            }

            vincularQuestaoConteudo(q, conteudos);
            questoes.add(q);
        }

        // 2 questões de verdadeiro/falso (2 alternativas cada)
        for (int i = 1; i <= 2; i++) {
            Questao q = new Questao();
            q.setEnunciado("[" + rotulo + "] Questão verdadeiro/falso nº " + i);
            q.setTipo(TipoQuestao.VERDADEIRO_FALSO);
            q.setDisciplina(disciplina);
            q.setNivelDificuldade(NivelDificuldade.MEDIA);
            q.setOrigem(OrigemQuestao.PROFESSOR);
            q.setStatus(StatusQuestao.APROVADA);
            q = questaoRepository.save(q);

            Alternativa v = new Alternativa();
            v.setQuestao(q);
            v.setTexto("Verdadeiro");
            v.setCorreta(true);
            v.setOrdem(1);
            alternativaRepository.save(v);

            Alternativa f = new Alternativa();
            f.setQuestao(q);
            f.setTexto("Falso");
            f.setCorreta(false);
            f.setOrdem(2);
            alternativaRepository.save(f);

            vincularQuestaoConteudo(q, conteudos);
            questoes.add(q);
        }

        return questoes;
    }

    private void vincularQuestaoConteudo(Questao questao, List<ConteudoPlano> conteudos) {
        QuestaoConteudo qc = new QuestaoConteudo();
        qc.setQuestao(questao);
        qc.setConteudoPlano(conteudos.get(0));
        questaoConteudoRepository.save(qc);
    }

    private Simulado criarSimulado(String titulo, Disciplina disciplina, PlanoEnsino planoEnsino, Turma turma, List<Questao> questoes) {
        Simulado s = new Simulado();
        s.setTitulo(titulo);
        s.setDisciplina(disciplina);
        s.setPlanoEnsino(planoEnsino);
        s.setTurma(turma);
        s.setTipoDestinacao(TipoDestinacaoSimulado.TODOS);
        s.setDataInicio(LocalDateTime.of(2026, 8, 25, 8, 0));
        s.setDataFim(LocalDateTime.of(2026, 8, 25, 10, 0));
        s.setTempoLimite(3600);
        s.setNotaMaxima(10.0);
        s.setQuantidadeQuestoes(questoes.size());
        s.setStatus(StatusSimulado.PUBLICADO);
        s = simuladoRepository.save(s);

        int ordem = 1;
        for (Questao q : questoes) {
            SimuladoQuestao sq = new SimuladoQuestao();
            sq.setSimulado(s);
            sq.setQuestao(q);
            sq.setOrdem(ordem++);
            sq.setPontuacao(2.0);
            sq.setStatus(StatusSimuladoQuestao.ATIVA);
            simuladoQuestaoRepository.save(sq);
        }
        return s;
    }

    private void responderSimulado(Simulado simulado, Aluno aluno, List<Questao> questoes, int quantidadeAcertos) {
        SimuladoAluno sa = new SimuladoAluno();
        sa.setSimulado(simulado);
        sa.setAluno(aluno);
        sa.setQuantidadeAcertos(quantidadeAcertos);
        sa.setNota(quantidadeAcertos * 2.0);
        sa.setTempoGasto(1800);
        sa.setFinalizadoPorTempo(false);
        sa.setStatus(StatusSimuladoAluno.CONCLUIDO);
        sa = simuladoAlunoRepository.save(sa);

        int acertosRestantes = quantidadeAcertos;
        for (Questao questao : questoes) {
            boolean acertou = acertosRestantes > 0;
            if (acertou) acertosRestantes--;

            List<Alternativa> alternativas = alternativaRepository.findAll().stream()
                    .filter(a -> a.getQuestao().getId().equals(questao.getId()))
                    .toList();
            Alternativa escolhida = alternativas.stream()
                    .filter(a -> a.getCorreta().equals(acertou))
                    .findFirst()
                    .orElse(alternativas.isEmpty() ? null : alternativas.get(0));

            QuestaoAluno qa = new QuestaoAluno();
            qa.setSimuladoAluno(sa);
            qa.setQuestao(questao);
            qa.setAlternativa(escolhida);
            qa.setAcertou(acertou);
            qa.setTempoResposta(120);
            questaoAlunoRepository.save(qa);
        }
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

    private void criarEvento(String titulo, String descricao, LocalDateTime dataHorario, Usuario criadoPor) {
        Evento evento = new Evento();
        evento.setTitulo(titulo);
        evento.setDescricao(descricao);
        evento.setDataHorario(dataHorario);
        evento.setConcluido(false);
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
