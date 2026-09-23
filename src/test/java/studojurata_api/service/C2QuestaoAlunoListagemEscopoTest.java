package studojurata_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
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
import studojurata_api.model.QuestaoAluno;
import studojurata_api.model.Simulado;
import studojurata_api.model.SimuladoAluno;
import studojurata_api.model.Turma;
import studojurata_api.repository.QuestaoAlunoRepository;
import studojurata_api.repository.SimuladoAlunoRepository;
import studojurata_api.security.UsuarioAutenticado;
import studojurata_api.support.AuthorizationTestSupport;

/**
 * C2 - escopo da listagem agregada de respostas (GET /questao-aluno).
 *
 * <p>A resposta carrega a alternativa marcada e o acerto do aluno; ver a resposta
 * de outro aluno (de outra turma ou de outro professor) tambem revela o gabarito
 * da questao. Por isso o recorte tem de ficar no servidor: o professor recebe
 * apenas as respostas das tentativas dos simulados que ja pode acessar.
 */
@ExtendWith(MockitoExtension.class)
class C2QuestaoAlunoListagemEscopoTest {

    private static final long ALUNO_ID = 7L;
    private static final long PROFESSOR_ID = 42L;
    private static final long SIMULADO_ID = 300L;
    private static final long TENTATIVA_ID = 500L;

    @Mock private QuestaoAlunoRepository questaoAlunoRepository;
    @Mock private SimuladoAlunoRepository simuladoAlunoRepository;
    @Mock private SimuladoService simuladoService;

    private QuestaoAlunoService service;

    @BeforeEach
    void setUp() {
        service = new QuestaoAlunoService(questaoAlunoRepository, simuladoAlunoRepository,
                simuladoService, new UsuarioAutenticado());
    }

    @AfterEach
    void limparContexto() {
        AuthorizationTestSupport.limparContexto();
    }

    @Test
    @DisplayName("administrador recebe todas as respostas, sem consultar escopo")
    void administradorRecebeTodasAsRespostas() {
        AuthorizationTestSupport.autenticarComoAdministrador();
        given(questaoAlunoRepository.findAll()).willReturn(List.of(resposta(1L, TENTATIVA_ID)));

        assertThat(service.listar()).extracting(QuestaoAluno::getId).containsExactly(1L);

        verifyNoInteractions(simuladoService, simuladoAlunoRepository);
    }

    @Test
    @DisplayName("professor recebe apenas as respostas dos simulados das suas turmas")
    void professorRecebeApenasRespostasDoEscopo() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(simuladoService.simuladoIdsVisiveis()).willReturn(Set.of(SIMULADO_ID));
        given(simuladoAlunoRepository.findBySimulado_IdIn(Set.of(SIMULADO_ID)))
                .willReturn(List.of(tentativa(TENTATIVA_ID, SIMULADO_ID, ALUNO_ID)));
        given(questaoAlunoRepository.findBySimuladoAluno_IdIn(Set.of(TENTATIVA_ID)))
                .willReturn(List.of(resposta(1L, TENTATIVA_ID)));

        assertThat(service.listar()).extracting(QuestaoAluno::getId).containsExactly(1L);

        verify(questaoAlunoRepository, never()).findAll();
    }

    @Test
    @DisplayName("professor sem simulados no escopo recebe lista vazia e nao consulta respostas")
    void professorSemSimuladosNaoConsultaRespostas() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(simuladoService.simuladoIdsVisiveis()).willReturn(Set.of());

        assertThat(service.listar()).isEmpty();

        verifyNoInteractions(questaoAlunoRepository, simuladoAlunoRepository);
    }

    @Test
    @DisplayName("professor com simulados sem tentativa recebe lista vazia e nao consulta respostas")
    void professorSemTentativasNaoConsultaRespostas() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(simuladoService.simuladoIdsVisiveis()).willReturn(Set.of(SIMULADO_ID));
        given(simuladoAlunoRepository.findBySimulado_IdIn(Set.of(SIMULADO_ID))).willReturn(List.of());

        assertThat(service.listar()).isEmpty();

        verifyNoInteractions(questaoAlunoRepository);
    }

    @Test
    @DisplayName("aluno nunca recebe a listagem global: sem escopo, lista vazia")
    void alunoNuncaRecebeAListagemGlobal() {
        AuthorizationTestSupport.autenticarComoAluno(ALUNO_ID);
        given(simuladoService.simuladoIdsVisiveis()).willReturn(Set.of());

        assertThat(service.listar()).isEmpty();

        verify(questaoAlunoRepository, never()).findAll();
        verifyNoInteractions(simuladoAlunoRepository);
    }

    @Test
    @DisplayName("sem autenticacao a listagem falha fechado (403) e nada e consultado")
    void semAutenticacaoNaoListaRespostas() {
        AuthorizationTestSupport.limparContexto();
        // Sem usuario reconhecido o proprio escopo de simulados falha fechado.
        given(simuladoService.simuladoIdsVisiveis())
                .willThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Nao autenticado."));

        try {
            service.listar();
            fail("esperava ResponseStatusException 403, mas a chamada foi permitida");
        } catch (ResponseStatusException ex) {
            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        verifyNoInteractions(questaoAlunoRepository, simuladoAlunoRepository);
    }

    // --- helpers -----------------------------------------------------------

    private static QuestaoAluno resposta(long id, long simuladoAlunoId) {
        QuestaoAluno resposta = new QuestaoAluno();
        resposta.setId(id);
        resposta.setSimuladoAluno(tentativa(simuladoAlunoId, SIMULADO_ID, ALUNO_ID));
        return resposta;
    }

    private static SimuladoAluno tentativa(long id, long simuladoId, long alunoId) {
        Aluno aluno = new Aluno();
        aluno.setId(alunoId);

        Turma turma = new Turma();
        turma.setId(10L);

        Simulado simulado = new Simulado();
        simulado.setId(simuladoId);
        simulado.setTurma(turma);

        SimuladoAluno tentativa = new SimuladoAluno();
        tentativa.setId(id);
        tentativa.setAluno(aluno);
        tentativa.setSimulado(simulado);
        return tentativa;
    }
}
