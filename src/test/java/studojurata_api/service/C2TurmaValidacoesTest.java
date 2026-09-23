package studojurata_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import studojurata_api.exception.RequisicaoInvalidaException;
import studojurata_api.model.Curso;
import studojurata_api.model.Turma;
import studojurata_api.model.enums.StatusAtivoInativo;
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
 * Bloco A — capacidade e data de início obrigatórias na turma.
 *
 * <p>A tela já exigia os dois campos; o backend aceitava turma sem eles, então
 * quem chamasse a API direto (ou o formulário antigo) criava turma sem
 * capacidade e sem período. As regras vivem no service, no mesmo caminho de
 * criação e edição.
 */
@ExtendWith(MockitoExtension.class)
class C2TurmaValidacoesTest {

    private static final long CURSO_ID = 3L;

    @Mock private TurmaRepository repository;
    @Mock private AlunoTurmaRepository alunoTurmaRepository;
    @Mock private AlunoTurmaService alunoTurmaService;
    @Mock private CursoRepository cursoRepository;
    @Mock private EscolaContext escolaContext;
    @Mock private EscopoProfessor escopoProfessor;
    @Mock private EscopoUsuario escopoUsuario;

    private TurmaService service;

    @BeforeEach
    void setUp() {
        service = new TurmaService(repository, alunoTurmaRepository, alunoTurmaService, cursoRepository,
                escolaContext, escopoProfessor, new UsuarioAutenticado(), new AlunoAccessGuard(escopoProfessor),
                new PlanejamentoAccessGuard(new UsuarioAutenticado(), escopoUsuario, escopoProfessor));
        // Criar turma e ato de gestao: a validacao dos campos e do administrador.
        AuthorizationTestSupport.autenticarComoAdministrador();
    }

    @AfterEach
    void limparContexto() {
        AuthorizationTestSupport.limparContexto();
    }

    @Test
    @DisplayName("professor nao cria turma: turma nova nao tem vinculo de onde tirar o escopo")
    void professorNaoCriaTurma() {
        AuthorizationTestSupport.autenticarComoProfessor(42L);

        try {
            service.salvar(turmaValida());
            fail("esperava 403");
        } catch (ResponseStatusException esperada) {
            assertThat(esperada.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        verify(repository, never()).save(any(Turma.class));
    }

    @Test
    @DisplayName("turma sem capacidade máxima é recusada")
    void semCapacidadeEhRecusada() {
        Turma turma = turmaValida();
        turma.setCapacidadeMaxima(null);

        assertInvalida(() -> service.salvar(turma), "Capacidade");

        verify(repository, never()).save(any(Turma.class));
    }

    @Test
    @DisplayName("capacidade zero ou negativa é recusada")
    void capacidadeNaoPositivaEhRecusada() {
        Turma turma = turmaValida();
        turma.setCapacidadeMaxima(0);

        assertInvalida(() -> service.salvar(turma), "maior que zero");

        verify(repository, never()).save(any(Turma.class));
    }

    @Test
    @DisplayName("turma sem data de início é recusada")
    void semDataInicioEhRecusada() {
        Turma turma = turmaValida();
        turma.setDataInicio(null);

        assertInvalida(() -> service.salvar(turma), "Data de início");

        verify(repository, never()).save(any(Turma.class));
    }

    @Test
    @DisplayName("data de término anterior à de início é recusada")
    void periodoInvertidoEhRecusado() {
        Turma turma = turmaValida();
        turma.setDataInicio(LocalDate.of(2026, 3, 10));
        turma.setDataFim(LocalDate.of(2026, 3, 1));

        assertInvalida(() -> service.salvar(turma), "anterior");

        verify(repository, never()).save(any(Turma.class));
    }

    @Test
    @DisplayName("turma com capacidade e período é salva")
    void turmaCompletaEhSalva() {
        Turma turma = turmaValida();
        given(cursoRepository.findById(CURSO_ID)).willReturn(Optional.of(cursoAtivo()));
        given(repository.save(any(Turma.class))).willAnswer(chamada -> chamada.getArgument(0));

        assertThat(service.salvar(turma).getCapacidadeMaxima()).isEqualTo(30);

        verify(repository).save(any(Turma.class));
    }

    private static Turma turmaValida() {
        Curso curso = new Curso();
        curso.setId(CURSO_ID);

        Turma turma = new Turma();
        turma.setTitulo("Turma A");
        turma.setCurso(curso);
        turma.setCapacidadeMaxima(30);
        turma.setDataInicio(LocalDate.of(2026, 3, 1));
        return turma;
    }

    private static Curso cursoAtivo() {
        Curso curso = new Curso();
        curso.setId(CURSO_ID);
        curso.setNome("Curso A");
        curso.setStatus(StatusAtivoInativo.ATIVO);
        return curso;
    }

    private static void assertInvalida(Runnable acao, String trecho) {
        try {
            acao.run();
            fail("esperava 400");
        } catch (RequisicaoInvalidaException esperada) {
            assertThat(esperada.getMessage()).contains(trecho);
        }
    }
}
