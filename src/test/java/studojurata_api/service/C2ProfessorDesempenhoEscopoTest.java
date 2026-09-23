package studojurata_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import studojurata_api.model.Turma;
import studojurata_api.model.TurmaDisciplina;
import studojurata_api.model.enums.StatusSimuladoAluno;
import studojurata_api.repository.SimuladoAlunoRepository;
import studojurata_api.repository.TurmaDisciplinaRepository;
import studojurata_api.security.ProfessorAccessGuard;
import studojurata_api.support.AuthorizationTestSupport;

/**
 * C2 — desempenho do professor ({@code GET /professores/{id}/desempenho}).
 *
 * <p>O id do professor vem da URL, então o service precisa do
 * {@code ProfessorAccessGuard}: sem ele, bastava trocar o id para ler as
 * tentativas (notas e alunos) das turmas de outro professor. Identidade e
 * guarda são reais; só os repositórios dublam.
 */
@ExtendWith(MockitoExtension.class)
class C2ProfessorDesempenhoEscopoTest {

    private static final long ALUNO_ID = 7L;
    private static final long PROFESSOR_ID = 42L;
    private static final long OUTRO_PROFESSOR_ID = 43L;
    private static final long TURMA_A = 10L;

    @Mock private SimuladoAlunoRepository simuladoAlunoRepository;
    @Mock private TurmaDisciplinaRepository turmaDisciplinaRepository;

    private DesempenhoService service;

    @BeforeEach
    void setUp() {
        service = new DesempenhoService(simuladoAlunoRepository, turmaDisciplinaRepository,
                new ProfessorAccessGuard());
    }

    @AfterEach
    void limparContexto() {
        AuthorizationTestSupport.limparContexto();
    }

    @Test
    @DisplayName("professor lê o próprio desempenho")
    void professorLeProprioDesempenho() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(turmaDisciplinaRepository.findByProfessorId(PROFESSOR_ID)).willReturn(List.of(vinculo(TURMA_A)));
        given(simuladoAlunoRepository.findByStatusAndNotaIsNotNullAndSimulado_Turma_IdIn(
                StatusSimuladoAluno.CONCLUIDO, java.util.Set.of(TURMA_A))).willReturn(List.of());

        assertThat(service.tentativasDoProfessor(PROFESSOR_ID)).isEmpty();

        verify(simuladoAlunoRepository).findByStatusAndNotaIsNotNullAndSimulado_Turma_IdIn(
                StatusSimuladoAluno.CONCLUIDO, java.util.Set.of(TURMA_A));
    }

    @Test
    @DisplayName("professor não lê o desempenho de outro professor e nada é consultado")
    void professorNaoLeDesempenhoDeOutro() {
        AuthorizationTestSupport.autenticarComoProfessor(OUTRO_PROFESSOR_ID);

        assertForbidden(() -> service.tentativasDoProfessor(PROFESSOR_ID));

        verifyNoInteractions(turmaDisciplinaRepository, simuladoAlunoRepository);
    }

    @Test
    @DisplayName("aluno não lê desempenho de professor e nada é consultado")
    void alunoNaoLeDesempenhoDeProfessor() {
        AuthorizationTestSupport.autenticarComoAluno(ALUNO_ID);

        assertForbidden(() -> service.tentativasDoProfessor(PROFESSOR_ID));

        verifyNoInteractions(turmaDisciplinaRepository, simuladoAlunoRepository);
    }

    @Test
    @DisplayName("administrador lê o desempenho de qualquer professor")
    void administradorLeDesempenhoDeQualquerProfessor() {
        AuthorizationTestSupport.autenticarComoAdministrador();
        given(turmaDisciplinaRepository.findByProfessorId(OUTRO_PROFESSOR_ID)).willReturn(List.of());

        assertThat(service.tentativasDoProfessor(OUTRO_PROFESSOR_ID)).isEmpty();

        verify(turmaDisciplinaRepository).findByProfessorId(OUTRO_PROFESSOR_ID);
    }

    @Test
    @DisplayName("sem autenticação o desempenho não é consultado")
    void semAutenticacaoNaoLeDesempenho() {
        assertForbidden(() -> service.tentativasDoProfessor(PROFESSOR_ID));

        verifyNoInteractions(turmaDisciplinaRepository, simuladoAlunoRepository);
    }

    private static TurmaDisciplina vinculo(long turmaId) {
        Turma turma = new Turma();
        turma.setId(turmaId);

        TurmaDisciplina vinculo = new TurmaDisciplina();
        vinculo.setId(turmaId);
        vinculo.setTurma(turma);
        return vinculo;
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
