package studojurata_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import studojurata_api.model.Curso;
import studojurata_api.model.Escola;
import studojurata_api.model.Turma;
import studojurata_api.model.enums.StatusAtivoInativo;
import studojurata_api.model.enums.StatusTurma;
import studojurata_api.repository.AlunoTurmaRepository;
import studojurata_api.repository.CursoRepository;
import studojurata_api.repository.TurmaRepository;
import studojurata_api.security.AlunoAccessGuard;
import studojurata_api.security.EscolaContext;
import studojurata_api.security.EscopoProfessor;
import studojurata_api.security.EscopoUsuario;
import studojurata_api.security.PlanejamentoAccessGuard;
import studojurata_api.security.UsuarioAutenticado;
import studojurata_api.support.AuthorizationTestSupport;

/**
 * Escrita de turma (POST/PUT/DELETE e ativar): o escopo do professor vem do
 * vinculo turma-disciplina — a mesma fonte do {@link EscopoProfessor} ja usada
 * nas listagens e pelos demais guardas. Antes desta correcao o SecurityConfig
 * liberava as rotas para PROFESSOR e o service nao checava posse, entao um
 * professor autenticado alterava/inativava turma de outro professor (ou de
 * outra escola) chamando a API direto.
 *
 * <p>Identidade e guardas sao reais; so os repositorios dublam.
 */
@ExtendWith(MockitoExtension.class)
class C2TurmaEscritaEscopoTest {

    private static final long ESCOLA_ID = 1L;
    private static final long OUTRA_ESCOLA_ID = 99L;
    private static final long ALUNO_ID = 7L;
    private static final long PROFESSOR_ID = 42L;
    private static final long TURMA_A = 10L;
    private static final long TURMA_B = 20L;
    private static final long CURSO_ID = 3L;

    @Mock private TurmaRepository turmaRepository;
    @Mock private AlunoTurmaRepository alunoTurmaRepository;
    @Mock private AlunoTurmaService alunoTurmaService;
    @Mock private CursoRepository cursoRepository;
    @Mock private EscolaContext escolaContext;
    @Mock private EscopoProfessor escopoProfessor;
    @Mock private EscopoUsuario escopoUsuario;

    private TurmaService service;

    @BeforeEach
    void setUp() {
        service = new TurmaService(turmaRepository, alunoTurmaRepository, alunoTurmaService, cursoRepository,
                escolaContext, escopoProfessor, new UsuarioAutenticado(), new AlunoAccessGuard(escopoProfessor),
                new PlanejamentoAccessGuard(new UsuarioAutenticado(), escopoUsuario, escopoProfessor));
    }

    @AfterEach
    void limparContexto() {
        AuthorizationTestSupport.limparContexto();
    }

    // --- ADMINISTRADOR ------------------------------------------------------

    @Test
    @DisplayName("administrador atualiza qualquer turma, sem consultar escopo de professor")
    void administradorAtualizaQualquerTurma() {
        AuthorizationTestSupport.autenticarComoAdministrador();
        dadoTurmaExistente(TURMA_B);
        dadoCursoDaEscola(ESCOLA_ID);
        given(turmaRepository.save(any(Turma.class))).willAnswer(chamada -> chamada.getArgument(0));

        assertThat(service.atualizar(TURMA_B, corpoDaTurma()).getId()).isEqualTo(TURMA_B);

        verify(turmaRepository).save(any(Turma.class));
        verifyNoInteractions(escopoProfessor);
    }

    @Test
    @DisplayName("administrador inativa e ativa qualquer turma")
    void administradorInativaEAtivaTurma() {
        AuthorizationTestSupport.autenticarComoAdministrador();
        given(turmaRepository.findById(TURMA_B)).willReturn(Optional.of(turma(TURMA_B)));
        given(turmaRepository.save(any(Turma.class))).willAnswer(chamada -> chamada.getArgument(0));

        service.deletar(TURMA_B);
        assertThat(service.ativar(TURMA_B).getStatus()).isEqualTo(StatusTurma.ATIVA);

        verifyNoInteractions(escopoProfessor);
    }

    @Test
    @DisplayName("administrador cria turma (comportamento da tela administrativa preservado)")
    void administradorCriaTurma() {
        AuthorizationTestSupport.autenticarComoAdministrador();
        dadoCursoDaEscola(ESCOLA_ID);
        given(turmaRepository.save(any(Turma.class))).willAnswer(chamada -> chamada.getArgument(0));

        Turma salva = service.salvar(corpoDaTurma());

        assertThat(salva.getTitulo()).isEqualTo("Turma A");
        assertThat(salva.getStatus()).isEqualTo(StatusTurma.ATIVA);
        verify(turmaRepository).save(any(Turma.class));
    }

    // --- PROFESSOR dentro do escopo ----------------------------------------

    @Test
    @DisplayName("professor atualiza a turma em que leciona, preservando a escola do registro")
    void professorAtualizaTurmaDoProprioEscopo() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        professorEhTitular(TURMA_A);
        dadoTurmaExistente(TURMA_A);
        dadoCursoDaEscola(ESCOLA_ID);
        given(turmaRepository.save(any(Turma.class))).willAnswer(chamada -> chamada.getArgument(0));

        Turma corpo = corpoDaTurma();
        // Corpo tentando trocar o tenant: a escola do registro e que vale.
        corpo.setEscola(escola(OUTRA_ESCOLA_ID));

        Turma salva = service.atualizar(TURMA_A, corpo);

        assertThat(salva.getEscola().getId()).isEqualTo(ESCOLA_ID);
        verify(turmaRepository).save(any(Turma.class));
    }

    @Test
    @DisplayName("professor inativa e ativa a propria turma")
    void professorInativaEAtivaTurmaDoProprioEscopo() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        professorEhTitular(TURMA_A);
        given(turmaRepository.findById(TURMA_A)).willReturn(Optional.of(turma(TURMA_A)));
        given(turmaRepository.save(any(Turma.class))).willAnswer(chamada -> chamada.getArgument(0));

        service.deletar(TURMA_A);
        assertThat(service.ativar(TURMA_A).getStatus()).isEqualTo(StatusTurma.ATIVA);

        verify(alunoTurmaRepository).existsByTurma_Id(TURMA_A);
    }

    // --- PROFESSOR fora do escopo ------------------------------------------

    @Test
    @DisplayName("professor nao atualiza turma fora do escopo e nada e lido nem gravado")
    void professorNaoAtualizaTurmaForaDoEscopo() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        professorEhTitular(TURMA_A);

        assertForbidden(() -> service.atualizar(TURMA_B, corpoDaTurma()));

        verify(turmaRepository, never()).save(any(Turma.class));
        verify(turmaRepository, never()).findById(any());
        verifyNoInteractions(cursoRepository, alunoTurmaService, alunoTurmaRepository);
    }

    @Test
    @DisplayName("professor nao inativa turma fora do escopo")
    void professorNaoInativaTurmaForaDoEscopo() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        professorEhTitular(TURMA_A);

        assertForbidden(() -> service.deletar(TURMA_B));

        verify(turmaRepository, never()).save(any(Turma.class));
        verify(turmaRepository, never()).findById(any());
        verifyNoInteractions(alunoTurmaRepository);
    }

    @Test
    @DisplayName("professor nao ativa turma fora do escopo")
    void professorNaoAtivaTurmaForaDoEscopo() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        professorEhTitular(TURMA_A);

        assertForbidden(() -> service.ativar(TURMA_B));

        verify(turmaRepository, never()).save(any(Turma.class));
        verify(turmaRepository, never()).findById(any());
    }

    @Test
    @DisplayName("professor nao move a propria turma para outra escola (curso de outro tenant)")
    void professorNaoUsaCursoDeOutraEscola() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        professorEhTitular(TURMA_A);
        dadoTurmaExistente(TURMA_A);
        given(escolaContext.escolaAtualId()).willReturn(ESCOLA_ID);
        given(cursoRepository.findById(CURSO_ID)).willReturn(Optional.of(cursoDaEscola(OUTRA_ESCOLA_ID)));

        assertForbidden(() -> service.atualizar(TURMA_A, corpoDaTurma()));

        verify(turmaRepository, never()).save(any(Turma.class));
    }

    // --- PERFIL / AUTENTICACAO --------------------------------------------

    @Test
    @DisplayName("aluno nao cria, nao altera, nao inativa e nao ativa turma")
    void alunoNaoEscreveTurma() {
        AuthorizationTestSupport.autenticarComoAluno(ALUNO_ID);

        assertForbidden(() -> service.salvar(corpoDaTurma()));
        assertForbidden(() -> service.atualizar(TURMA_A, corpoDaTurma()));
        assertForbidden(() -> service.deletar(TURMA_A));
        assertForbidden(() -> service.ativar(TURMA_A));

        verifyNoInteractions(turmaRepository, alunoTurmaRepository, cursoRepository, alunoTurmaService);
    }

    @Test
    @DisplayName("sem autenticacao nenhuma escrita de turma passa e nada e gravado")
    void semAutenticacaoNaoEscreveTurma() {
        AuthorizationTestSupport.limparContexto();

        assertForbidden(() -> service.salvar(corpoDaTurma()));
        assertForbidden(() -> service.atualizar(TURMA_A, corpoDaTurma()));
        assertForbidden(() -> service.deletar(TURMA_A));
        assertForbidden(() -> service.ativar(TURMA_A));

        verifyNoInteractions(turmaRepository, alunoTurmaRepository, cursoRepository, alunoTurmaService);
    }

    // --- helpers -----------------------------------------------------------

    private void professorEhTitular(long... turmaIds) {
        Set<Long> turmas = java.util.Arrays.stream(turmaIds).boxed()
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
        given(escopoProfessor.turmaIdsDoProfessor(PROFESSOR_ID)).willReturn(turmas);
    }

    private void dadoTurmaExistente(long turmaId) {
        given(turmaRepository.findById(turmaId)).willReturn(Optional.of(turma(turmaId)));
    }

    private void dadoCursoDaEscola(long escolaId) {
        given(escolaContext.escolaAtualId()).willReturn(ESCOLA_ID);
        given(cursoRepository.findById(CURSO_ID)).willReturn(Optional.of(cursoDaEscola(escolaId)));
    }

    private static Turma corpoDaTurma() {
        Turma turma = new Turma();
        turma.setTitulo("Turma A");
        turma.setCurso(cursoRef());
        turma.setCapacidadeMaxima(30);
        turma.setDataInicio(LocalDate.of(2026, 3, 1));
        turma.setStatus(StatusTurma.ATIVA);
        return turma;
    }

    private static Turma turma(long id) {
        Turma turma = new Turma();
        turma.setId(id);
        turma.setEscola(escola(ESCOLA_ID));
        turma.setStatus(StatusTurma.ATIVA);
        return turma;
    }

    private static Curso cursoRef() {
        Curso curso = new Curso();
        curso.setId(CURSO_ID);
        return curso;
    }

    private static Curso cursoDaEscola(long escolaId) {
        Curso curso = cursoRef();
        curso.setNome("Curso A");
        curso.setStatus(StatusAtivoInativo.ATIVO);
        curso.setEscola(escola(escolaId));
        return curso;
    }

    private static Escola escola(long id) {
        Escola escola = new Escola();
        escola.setId(id);
        return escola;
    }

    private static void assertForbidden(Runnable acao) {
        try {
            acao.run();
        } catch (ResponseStatusException ex) {
            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            return;
        }
        fail("esperava ResponseStatusException 403, mas a chamada foi permitida");
    }
}
