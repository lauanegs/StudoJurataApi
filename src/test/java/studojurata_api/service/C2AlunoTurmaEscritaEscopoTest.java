package studojurata_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.time.LocalDate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import studojurata_api.exception.RegraNegocioException;
import studojurata_api.model.Aluno;
import studojurata_api.model.AlunoTurma;
import studojurata_api.model.Pessoa;
import studojurata_api.model.ResponsavelAluno;
import studojurata_api.model.Turma;
import studojurata_api.model.enums.StatusMatricula;
import studojurata_api.model.enums.StatusTurma;
import studojurata_api.repository.AlunoTurmaRepository;
import studojurata_api.repository.AlunoRepository;
import studojurata_api.repository.ResponsavelAlunoRepository;
import studojurata_api.repository.TurmaRepository;
import studojurata_api.security.EscopoProfessor;
import studojurata_api.security.EscopoUsuario;
import studojurata_api.security.PlanejamentoAccessGuard;
import studojurata_api.security.UsuarioAutenticado;
import studojurata_api.support.AuthorizationTestSupport;

/**
 * C2 — escopo do professor nas matrículas ({@code /aluno-turma}).
 *
 * <p>Três buracos do mesmo domínio: a listagem trazia as matrículas da escola
 * inteira, e matricular/atualizar aceitavam qualquer turma — um professor podia
 * matricular aluno na turma de outro. A leitura passa a ser recortada pelas
 * turmas do professor e a escrita passa pelo {@code PlanejamentoAccessGuard},
 * o mesmo guard já usado pelo planejamento. Identidade e guardas são reais; só
 * os repositórios dublam.
 */
@ExtendWith(MockitoExtension.class)
class C2AlunoTurmaEscritaEscopoTest {

    private static final long ALUNO_ID = 7L;
    private static final long PROFESSOR_ID = 42L;
    private static final long OUTRO_PROFESSOR_ID = 43L;
    private static final long TURMA_A = 10L;
    private static final long TURMA_B = 20L;
    private static final long MATRICULA_ID = 900L;

    @Mock private AlunoTurmaRepository repository;
    @Mock private AlunoRepository alunoRepository;
    @Mock private ResponsavelAlunoRepository responsavelAlunoRepository;
    @Mock private TurmaRepository turmaRepository;
    @Mock private EscopoProfessor escopoProfessor;
    @Mock private EscopoUsuario escopoUsuario;

    private AlunoTurmaService service;

    @BeforeEach
    void setUp() {
        var planejamentoAccessGuard =
                new PlanejamentoAccessGuard(new UsuarioAutenticado(), escopoUsuario, escopoProfessor);
        service = new AlunoTurmaService(repository, alunoRepository, responsavelAlunoRepository, turmaRepository,
                new UsuarioAutenticado(), escopoProfessor, planejamentoAccessGuard);
    }

    @AfterEach
    void limparContexto() {
        AuthorizationTestSupport.limparContexto();
    }

    // --- listar() -----------------------------------------------------------

    @Test
    @DisplayName("administrador lista todas as matrículas")
    void administradorListaTodas() {
        AuthorizationTestSupport.autenticarComoAdministrador();
        given(repository.findAll()).willReturn(List.of(matricula(MATRICULA_ID, ALUNO_ID, TURMA_A)));

        assertThat(service.listar()).hasSize(1);

        verify(repository).findAll();
    }

    @Test
    @DisplayName("professor lista apenas as matrículas das suas turmas")
    void professorListaApenasSuasTurmas() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(escopoProfessor.turmaIdsDoProfessor(PROFESSOR_ID)).willReturn(Set.of(TURMA_A));
        given(repository.findByTurma_IdIn(Set.of(TURMA_A)))
                .willReturn(List.of(matricula(MATRICULA_ID, ALUNO_ID, TURMA_A)));

        assertThat(service.listar()).hasSize(1);

        verify(repository, never()).findAll();
        verify(repository).findByTurma_IdIn(Set.of(TURMA_A));
    }

    @Test
    @DisplayName("professor sem turmas não consulta matrícula nenhuma")
    void professorSemTurmasNaoLista() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(escopoProfessor.turmaIdsDoProfessor(PROFESSOR_ID)).willReturn(Set.of());

        assertThat(service.listar()).isEmpty();

        verify(repository, never()).findAll();
        verify(repository, never()).findByTurma_IdIn(any());
    }

    // --- matricular() -------------------------------------------------------

    @Test
    @DisplayName("professor matricula aluno em turma que leciona")
    void professorMatriculaNaPropriaTurma() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        dadoTurmaAtivaSemLimite(TURMA_A);
        dadoEscopoDoProfessor(TURMA_A);
        dadoAlunoMaiorDeIdade(ALUNO_ID);
        given(repository.save(any(AlunoTurma.class))).willAnswer(chamada -> chamada.getArgument(0));

        assertThat(service.matricular(novaMatricula(ALUNO_ID, TURMA_A)).getTurma().getId()).isEqualTo(TURMA_A);

        verify(repository).save(any(AlunoTurma.class));
    }

    @Test
    @DisplayName("administrador matricula em qualquer turma")
    void administradorMatriculaEmQualquerTurma() {
        AuthorizationTestSupport.autenticarComoAdministrador();
        dadoTurmaAtivaSemLimite(TURMA_B);
        dadoAlunoMaiorDeIdade(ALUNO_ID);
        given(repository.save(any(AlunoTurma.class))).willAnswer(chamada -> chamada.getArgument(0));

        assertThat(service.matricular(novaMatricula(ALUNO_ID, TURMA_B)).getTurma().getId()).isEqualTo(TURMA_B);

        verify(repository).save(any(AlunoTurma.class));
    }

    @Test
    @DisplayName("professor não matricula em turma de outro professor e nada é gravado")
    void professorNaoMatriculaEmTurmaAlheia() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        dadoEscopoDoProfessor(TURMA_A);

        assertForbidden(() -> service.matricular(novaMatricula(ALUNO_ID, TURMA_B)));

        verify(repository, never()).save(any(AlunoTurma.class));
        verifyNoInteractions(turmaRepository);
    }

    @Test
    @DisplayName("aluno não matricula e nada é gravado")
    void alunoNaoMatricula() {
        AuthorizationTestSupport.autenticarComoAluno(ALUNO_ID);

        assertForbidden(() -> service.matricular(novaMatricula(ALUNO_ID, TURMA_A)));

        verify(repository, never()).save(any(AlunoTurma.class));
        verifyNoInteractions(turmaRepository);
    }

    @Test
    @DisplayName("sem autenticação não matricula")
    void semAutenticacaoNaoMatricula() {
        assertForbidden(() -> service.matricular(novaMatricula(ALUNO_ID, TURMA_A)));

        verify(repository, never()).save(any(AlunoTurma.class));
        verifyNoInteractions(turmaRepository);
    }

    @Test
    @DisplayName("aluno menor de idade sem responsável não é matriculado")
    void menorSemResponsavelNaoMatricula() {
        AuthorizationTestSupport.autenticarComoAdministrador();
        dadoAlunoMenorDeIdade(ALUNO_ID);
        given(responsavelAlunoRepository.findByAlunoId(ALUNO_ID)).willReturn(List.of());

        try {
            service.matricular(novaMatricula(ALUNO_ID, TURMA_A));
            fail("esperava 409");
        } catch (RegraNegocioException esperada) {
            assertThat(esperada.getMessage()).contains("responsável");
        }

        verify(repository, never()).save(any(AlunoTurma.class));
    }

    @Test
    @DisplayName("aluno menor de idade com responsável é matriculado")
    void menorComResponsavelMatricula() {
        AuthorizationTestSupport.autenticarComoAdministrador();
        dadoTurmaAtivaSemLimite(TURMA_A);
        dadoAlunoMenorDeIdade(ALUNO_ID);
        given(responsavelAlunoRepository.findByAlunoId(ALUNO_ID)).willReturn(List.of(new ResponsavelAluno()));
        given(repository.save(any(AlunoTurma.class))).willAnswer(chamada -> chamada.getArgument(0));

        assertThat(service.matricular(novaMatricula(ALUNO_ID, TURMA_A)).getTurma().getId()).isEqualTo(TURMA_A);

        verify(repository).save(any(AlunoTurma.class));
    }

    // --- atualizar() --------------------------------------------------------

    @Test
    @DisplayName("professor atualiza matrícula da própria turma")
    void professorAtualizaMatriculaDaPropriaTurma() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        dadoMatriculaExistente(TURMA_A);
        dadoEscopoDoProfessor(TURMA_A);
        given(repository.save(any(AlunoTurma.class))).willAnswer(chamada -> chamada.getArgument(0));

        AlunoTurma alteracao = new AlunoTurma();
        alteracao.setStatus(StatusMatricula.CONCLUIDA);

        assertThat(service.atualizar(MATRICULA_ID, alteracao).getStatus()).isEqualTo(StatusMatricula.CONCLUIDA);

        verify(repository).save(any(AlunoTurma.class));
    }

    @Test
    @DisplayName("professor não mexe na matrícula de turma alheia e nada é gravado")
    void professorNaoAtualizaMatriculaDeTurmaAlheia() {
        AuthorizationTestSupport.autenticarComoProfessor(OUTRO_PROFESSOR_ID);
        dadoMatriculaExistente(TURMA_A);
        given(escopoProfessor.turmaIdsDoProfessor(OUTRO_PROFESSOR_ID)).willReturn(Set.of(TURMA_B));

        assertForbidden(() -> service.atualizar(MATRICULA_ID, new AlunoTurma()));

        verify(repository, never()).save(any(AlunoTurma.class));
    }

    @Test
    @DisplayName("professor não move matrícula para turma de outro professor")
    void professorNaoMoveMatriculaParaTurmaAlheia() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        dadoMatriculaExistente(TURMA_A);
        dadoEscopoDoProfessor(TURMA_A);

        AlunoTurma alteracao = new AlunoTurma();
        alteracao.setTurma(turmaComId(TURMA_B));

        assertForbidden(() -> service.atualizar(MATRICULA_ID, alteracao));

        verify(repository, never()).save(any(AlunoTurma.class));
    }

    // --- apoio --------------------------------------------------------------

    private void dadoEscopoDoProfessor(long... turmaIds) {
        Set<Long> turmas = new java.util.LinkedHashSet<>();
        for (long turmaId : turmaIds) {
            turmas.add(turmaId);
        }
        given(escopoProfessor.turmaIdsDoProfessor(PROFESSOR_ID)).willReturn(turmas);
    }

    private void dadoTurmaAtivaSemLimite(long turmaId) {
        given(turmaRepository.findById(turmaId)).willReturn(Optional.of(turmaAtiva(turmaId)));
        given(repository.findFirstByAluno_IdAndTurma_IdAndStatus(ALUNO_ID, turmaId, StatusMatricula.ATIVA))
                .willReturn(Optional.empty());
    }

    /** Aluno maior de idade: a regra de responsável não se aplica. */
    private void dadoAlunoMaiorDeIdade(long alunoId) {
        dadoAlunoNascidoEm(alunoId, LocalDate.now().minusYears(20));
    }

    /** Aluno menor de idade: exige responsável vinculado. */
    private void dadoAlunoMenorDeIdade(long alunoId) {
        dadoAlunoNascidoEm(alunoId, LocalDate.now().minusYears(10));
    }

    private void dadoAlunoNascidoEm(long alunoId, LocalDate nascimento) {
        Pessoa pessoa = new Pessoa();
        pessoa.setId(alunoId);
        pessoa.setDataNascimento(nascimento);

        Aluno aluno = new Aluno();
        aluno.setId(alunoId);
        aluno.setPessoa(pessoa);

        given(alunoRepository.findById(alunoId)).willReturn(Optional.of(aluno));
    }

    private void dadoMatriculaExistente(long turmaId) {
        given(repository.findById(MATRICULA_ID))
                .willReturn(Optional.of(matricula(MATRICULA_ID, ALUNO_ID, turmaId)));
    }

    private static AlunoTurma novaMatricula(long alunoId, long turmaId) {
        AlunoTurma matricula = new AlunoTurma();
        matricula.setAluno(alunoComId(alunoId));
        matricula.setTurma(turmaComId(turmaId));
        return matricula;
    }

    private static AlunoTurma matricula(long matriculaId, long alunoId, long turmaId) {
        AlunoTurma matricula = new AlunoTurma();
        matricula.setId(matriculaId);
        matricula.setAluno(alunoComId(alunoId));
        matricula.setTurma(turmaComId(turmaId));
        return matricula;
    }

    private static Aluno alunoComId(long alunoId) {
        Aluno aluno = new Aluno();
        aluno.setId(alunoId);
        return aluno;
    }

    private static Turma turmaComId(long turmaId) {
        Turma turma = new Turma();
        turma.setId(turmaId);
        return turma;
    }

    private static Turma turmaAtiva(long turmaId) {
        Turma turma = turmaComId(turmaId);
        turma.setTitulo("Turma " + turmaId);
        turma.setStatus(StatusTurma.ATIVA);
        return turma;
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
