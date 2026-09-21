package studojurata_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
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

import studojurata_api.model.Aluno;
import studojurata_api.model.AlunoTurma;
import studojurata_api.model.Nota;
import studojurata_api.repository.AlunoRepository;
import studojurata_api.repository.AlunoTurmaRepository;
import studojurata_api.repository.DisciplinaRepository;
import studojurata_api.repository.NotaRepository;
import studojurata_api.repository.SimuladoAlunoRepository;
import studojurata_api.repository.TurmaRepository;
import studojurata_api.security.EscopoProfessor;
import studojurata_api.security.UsuarioAutenticado;
import studojurata_api.support.AuthorizationTestSupport;

/**
 * C2.6 — escopo da listagem de notas.
 *
 * <p>Decisao do bloco: notas sem turma (historicas) continuam visiveis ao
 * professor quando o aluno pertence ao escopo das turmas dele. Consultas em
 * lote, sem N+1, e resultado sem duplicidade por id.
 */
@ExtendWith(MockitoExtension.class)
class C2NotaListagemEscopoTest {

    private static final long ALUNO_ID = 7L;
    private static final long OUTRO_ALUNO_ID = 8L;
    private static final long PROFESSOR_ID = 42L;
    private static final long TURMA_A = 10L;

    @Mock private NotaRepository notaRepository;
    @Mock private AlunoRepository alunoRepository;
    @Mock private DisciplinaRepository disciplinaRepository;
    @Mock private TurmaRepository turmaRepository;
    @Mock private AlunoTurmaRepository alunoTurmaRepository;
    @Mock private SimuladoAlunoRepository simuladoAlunoRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private EscopoProfessor escopoProfessor;

    private NotaService service;

    @BeforeEach
    void setUp() {
        service = new NotaService(notaRepository, alunoRepository, disciplinaRepository, turmaRepository,
                alunoTurmaRepository, simuladoAlunoRepository, auditLogService, new UsuarioAutenticado(),
                escopoProfessor);
    }

    @AfterEach
    void limparContexto() {
        AuthorizationTestSupport.limparContexto();
    }

    @Test
    @DisplayName("professor ve apenas as notas das suas turmas")
    void professorVeApenasNotasDasSuasTurmas() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(escopoProfessor.turmaIdsDoProfessor(PROFESSOR_ID)).willReturn(Set.of(TURMA_A));
        given(notaRepository.findByTurma_IdIn(Set.of(TURMA_A))).willReturn(List.of(nota(1L, TURMA_A)));
        given(alunoTurmaRepository.findByTurma_IdIn(Set.of(TURMA_A))).willReturn(List.of(matricula(ALUNO_ID)));
        given(notaRepository.findByTurmaIsNullAndAluno_IdIn(Set.of(ALUNO_ID))).willReturn(List.of());

        assertThat(service.listar()).extracting(Nota::getId).containsExactly(1L);

        verify(notaRepository, never()).findAll();
    }

    @Test
    @DisplayName("professor ve notas sem turma de alunos do seu escopo")
    void professorVeNotasOrfasDeAlunoDoEscopo() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(escopoProfessor.turmaIdsDoProfessor(PROFESSOR_ID)).willReturn(Set.of(TURMA_A));
        given(notaRepository.findByTurma_IdIn(Set.of(TURMA_A))).willReturn(List.of(nota(1L, TURMA_A)));
        given(alunoTurmaRepository.findByTurma_IdIn(Set.of(TURMA_A))).willReturn(List.of(matricula(ALUNO_ID)));
        given(notaRepository.findByTurmaIsNullAndAluno_IdIn(Set.of(ALUNO_ID)))
                .willReturn(List.of(nota(2L, null)));

        assertThat(service.listar()).extracting(Nota::getId).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    @DisplayName("nota sem turma de aluno fora do escopo nao entra: a consulta e filtrada pelos alunos do escopo")
    void professorNaoVeNotasOrfasDeAlunoForaDoEscopo() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(escopoProfessor.turmaIdsDoProfessor(PROFESSOR_ID)).willReturn(Set.of(TURMA_A));
        given(notaRepository.findByTurma_IdIn(Set.of(TURMA_A))).willReturn(List.of());
        given(alunoTurmaRepository.findByTurma_IdIn(Set.of(TURMA_A)))
                .willReturn(List.of(matricula(ALUNO_ID), matricula(ALUNO_ID)));
        given(notaRepository.findByTurmaIsNullAndAluno_IdIn(Set.of(ALUNO_ID))).willReturn(List.of(nota(3L, null)));

        assertThat(service.listar()).extracting(Nota::getId).containsExactly(3L);

        // A consulta recebe apenas o aluno do escopo (nunca OUTRO_ALUNO_ID).
        verify(notaRepository).findByTurmaIsNullAndAluno_IdIn(Set.of(ALUNO_ID));
    }

    @Test
    @DisplayName("nota que aparece nas duas origens volta uma unica vez")
    void notaRepetidaNaoDuplica() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(escopoProfessor.turmaIdsDoProfessor(PROFESSOR_ID)).willReturn(Set.of(TURMA_A));
        given(notaRepository.findByTurma_IdIn(Set.of(TURMA_A))).willReturn(List.of(nota(1L, TURMA_A)));
        given(alunoTurmaRepository.findByTurma_IdIn(Set.of(TURMA_A))).willReturn(List.of(matricula(ALUNO_ID)));
        given(notaRepository.findByTurmaIsNullAndAluno_IdIn(Set.of(ALUNO_ID)))
                .willReturn(List.of(nota(1L, TURMA_A)));

        assertThat(service.listar()).extracting(Nota::getId).containsExactly(1L);
    }

    @Test
    @DisplayName("professor sem turmas recebe lista vazia sem consultar notas")
    void professorSemTurmasNaoConsultaNotas() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(escopoProfessor.turmaIdsDoProfessor(PROFESSOR_ID)).willReturn(Set.of());

        assertThat(service.listar()).isEmpty();

        verifyNoInteractions(notaRepository, alunoTurmaRepository);
    }

    @Test
    @DisplayName("consultas em lote: uma por origem, nunca uma por turma ou por aluno")
    void consultasEmLoteSemN1() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(escopoProfessor.turmaIdsDoProfessor(PROFESSOR_ID)).willReturn(Set.of(TURMA_A, 20L));
        given(notaRepository.findByTurma_IdIn(Set.of(TURMA_A, 20L))).willReturn(List.of(nota(1L, TURMA_A)));
        given(alunoTurmaRepository.findByTurma_IdIn(Set.of(TURMA_A, 20L)))
                .willReturn(List.of(matricula(ALUNO_ID), matricula(ALUNO_ID)));
        given(notaRepository.findByTurmaIsNullAndAluno_IdIn(Set.of(ALUNO_ID))).willReturn(List.of());

        service.listar();

        verify(notaRepository, times(1)).findByTurma_IdIn(any());
        verify(alunoTurmaRepository, times(1)).findByTurma_IdIn(any());
        verify(notaRepository, times(1)).findByTurmaIsNullAndAluno_IdIn(any());
    }

    @Test
    @DisplayName("aluno nao lista notas pela listagem geral (403) e nada e consultado")
    void alunoNaoListaNotasGerais() {
        AuthorizationTestSupport.autenticarComoAluno(ALUNO_ID);

        assertForbidden(() -> service.listar());
        verifyNoInteractions(notaRepository, alunoTurmaRepository);
    }

    @Test
    @DisplayName("sem autenticacao a listagem falha fechado (403)")
    void semAutenticacaoNaoListaNotas() {
        AuthorizationTestSupport.limparContexto();

        assertForbidden(() -> service.listar());
        verifyNoInteractions(notaRepository);
    }

    @Test
    @DisplayName("administrador continua vendo todas as notas")
    void administradorListaTodasAsNotas() {
        AuthorizationTestSupport.autenticarComoAdministrador();
        given(notaRepository.findAll()).willReturn(List.of(nota(1L, TURMA_A), nota(2L, null)));

        assertThat(service.listar()).hasSize(2);

        verifyNoInteractions(escopoProfessor);
    }

    // --- helpers -----------------------------------------------------------

    private void assertForbidden(Runnable acao) {
        try {
            acao.run();
        } catch (ResponseStatusException ex) {
            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            return;
        }
        fail("esperava ResponseStatusException 403, mas a chamada foi permitida");
    }

    private static Nota nota(long id, Long turmaId) {
        Nota nota = new Nota();
        nota.setId(id);
        if (turmaId != null) {
            studojurata_api.model.Turma turma = new studojurata_api.model.Turma();
            turma.setId(turmaId);
            nota.setTurma(turma);
        }
        return nota;
    }

    private static AlunoTurma matricula(long alunoId) {
        Aluno aluno = new Aluno();
        aluno.setId(alunoId);

        AlunoTurma matricula = new AlunoTurma();
        matricula.setAluno(aluno);
        return matricula;
    }
}
