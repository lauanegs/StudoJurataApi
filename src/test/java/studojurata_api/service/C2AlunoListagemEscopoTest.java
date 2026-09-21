package studojurata_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
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

import studojurata_api.model.Aluno;
import studojurata_api.model.AlunoTurma;
import studojurata_api.repository.AlunoRepository;
import studojurata_api.repository.AlunoTurmaRepository;
import studojurata_api.security.EscopoProfessor;
import studojurata_api.security.UsuarioAutenticado;
import studojurata_api.support.AuthorizationTestSupport;

/**
 * C2.5 — escopo da listagem de alunos.
 *
 * <p>Identidade e regra de administrador reais; repositorios e o recorte de
 * vinculos dublados. Matriculas de qualquer status contam (historico).
 */
@ExtendWith(MockitoExtension.class)
class C2AlunoListagemEscopoTest {

    private static final long ALUNO_ID = 7L;
    private static final long OUTRO_ALUNO_ID = 8L;
    private static final long PROFESSOR_ID = 42L;
    private static final long TURMA_A = 10L;
    private static final long TURMA_B = 20L;

    @Mock private AlunoRepository alunoRepository;
    @Mock private AlunoTurmaRepository alunoTurmaRepository;
    @Mock private EscopoProfessor escopoProfessor;

    private AlunoService service;

    @BeforeEach
    void setUp() {
        service = new AlunoService(alunoRepository, alunoTurmaRepository, new UsuarioAutenticado(), escopoProfessor);
    }

    @AfterEach
    void limparContexto() {
        AuthorizationTestSupport.limparContexto();
    }

    @Test
    @DisplayName("professor ve alunos das suas turmas, sem duplicar quem esta em duas")
    void professorVeAlunosSemDuplicidade() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(escopoProfessor.turmaIdsDoProfessor(PROFESSOR_ID)).willReturn(Set.of(TURMA_A, TURMA_B));
        given(alunoTurmaRepository.findByTurma_IdIn(Set.of(TURMA_A, TURMA_B))).willReturn(List.of(
                matricula(ALUNO_ID), matricula(OUTRO_ALUNO_ID), matricula(ALUNO_ID)));
        given(alunoRepository.findAllById(Set.of(ALUNO_ID, OUTRO_ALUNO_ID)))
                .willReturn(List.of(aluno(ALUNO_ID), aluno(OUTRO_ALUNO_ID)));

        assertThat(service.listar()).extracting(Aluno::getId)
                .containsExactlyInAnyOrder(ALUNO_ID, OUTRO_ALUNO_ID);

        // O conjunto de ids prova a ausencia de duplicidade.
        verify(alunoRepository).findAllById(Set.of(ALUNO_ID, OUTRO_ALUNO_ID));
        verify(alunoRepository, never()).findAll();
    }

    @Test
    @DisplayName("professor sem turmas nao consulta alunos")
    void professorSemTurmasNaoConsultaAlunos() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(escopoProfessor.turmaIdsDoProfessor(PROFESSOR_ID)).willReturn(Set.of());

        assertThat(service.listar()).isEmpty();

        verifyNoInteractions(alunoRepository, alunoTurmaRepository);
    }

    @Test
    @DisplayName("turmas sem matriculas resultam em lista vazia sem consultar alunos")
    void turmasSemMatriculasNaoConsultamAlunos() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(escopoProfessor.turmaIdsDoProfessor(PROFESSOR_ID)).willReturn(Set.of(TURMA_A));
        given(alunoTurmaRepository.findByTurma_IdIn(Set.of(TURMA_A))).willReturn(List.of());

        assertThat(service.listar()).isEmpty();

        verifyNoInteractions(alunoRepository);
    }

    @Test
    @DisplayName("professor ve aluno com matricula encerrada (historico preservado)")
    void professorVeAlunoComMatriculaEncerrada() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(escopoProfessor.turmaIdsDoProfessor(PROFESSOR_ID)).willReturn(Set.of(TURMA_A));
        given(alunoTurmaRepository.findByTurma_IdIn(Set.of(TURMA_A))).willReturn(List.of(matricula(ALUNO_ID)));
        given(alunoRepository.findAllById(Set.of(ALUNO_ID))).willReturn(List.of(aluno(ALUNO_ID)));

        assertThat(service.listar()).hasSize(1);

        // A consulta nao recebe status: matriculas CONCLUIDA/CANCELADA entram.
        verify(alunoTurmaRepository).findByTurma_IdIn(Set.of(TURMA_A));
    }

    @Test
    @DisplayName("aluno ve apenas o proprio cadastro")
    void alunoVeApenasASiMesmo() {
        AuthorizationTestSupport.autenticarComoAluno(ALUNO_ID);
        given(alunoRepository.findById(ALUNO_ID)).willReturn(Optional.of(aluno(ALUNO_ID)));

        assertThat(service.listar()).extracting(Aluno::getId).containsExactly(ALUNO_ID);

        verify(alunoRepository, never()).findAll();
    }

    @Test
    @DisplayName("aluno sem cadastro vinculado nao consulta alunos")
    void alunoSemVinculoNaoConsultaAlunos() {
        AuthorizationTestSupport.autenticarComoAlunoSemVinculo(AuthorizationTestSupport.PESSOA_ADMIN_ID);

        assertThat(service.listar()).isEmpty();

        verifyNoInteractions(alunoRepository, alunoTurmaRepository);
    }

    @Test
    @DisplayName("administrador continua vendo todos os alunos")
    void administradorListaTodosOsAlunos() {
        AuthorizationTestSupport.autenticarComoAdministrador();
        given(alunoRepository.findAll()).willReturn(List.of(aluno(ALUNO_ID), aluno(OUTRO_ALUNO_ID)));

        assertThat(service.listar()).hasSize(2);

        verifyNoInteractions(escopoProfessor);
    }

    @Test
    @DisplayName("sem autenticacao a listagem falha fechado (403)")
    void semAutenticacaoNaoListaAlunos() {
        AuthorizationTestSupport.limparContexto();

        assertForbidden(() -> service.listar());
        verifyNoInteractions(alunoRepository, alunoTurmaRepository);
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

    private static Aluno aluno(long id) {
        Aluno aluno = new Aluno();
        aluno.setId(id);
        return aluno;
    }

    private static AlunoTurma matricula(long alunoId) {
        AlunoTurma matricula = new AlunoTurma();
        matricula.setAluno(aluno(alunoId));
        return matricula;
    }
}
