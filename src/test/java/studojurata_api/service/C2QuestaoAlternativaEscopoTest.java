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

import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.model.Alternativa;
import studojurata_api.model.Disciplina;
import studojurata_api.model.Questao;
import studojurata_api.model.enums.StatusQuestao;
import studojurata_api.model.enums.TipoQuestao;
import studojurata_api.repository.AlternativaRepository;
import studojurata_api.repository.QuestaoRepository;
import studojurata_api.security.EscopoProfessor;
import studojurata_api.security.UsuarioAutenticado;
import studojurata_api.support.AuthorizationTestSupport;

/**
 * C2.7a — escopo de leitura de questoes e alternativas.
 *
 * <p>As regressoes do C2.7b+c (dono da tentativa sem "correta"/"gabarito" e
 * com gabarito depois de concluir) ficam em {@code C2TentativaQuestoesTest},
 * reexecutado na suite completa.
 */
@ExtendWith(MockitoExtension.class)
class C2QuestaoAlternativaEscopoTest {

    private static final long ALUNO_ID = 7L;
    private static final long PROFESSOR_ID = 42L;
    private static final long DISC_A = 10L;
    private static final long DISC_B = 20L;
    private static final long QUESTAO_ID = 100L;

    @Mock private QuestaoRepository questaoRepository;
    @Mock private AlternativaRepository alternativaRepository;
    @Mock private EscopoProfessor escopoProfessor;

    private QuestaoService questaoService;
    private AlternativaService alternativaService;

    @BeforeEach
    void setUp() {
        UsuarioAutenticado usuarioAutenticado = new UsuarioAutenticado();
        questaoService = new QuestaoService(questaoRepository, usuarioAutenticado, escopoProfessor);
        alternativaService = new AlternativaService(alternativaRepository, usuarioAutenticado, escopoProfessor);
    }

    @AfterEach
    void limparContexto() {
        AuthorizationTestSupport.limparContexto();
    }

    @Test
    @DisplayName("professor com duas disciplinas ve somente as questoes delas, em uma consulta")
    void professorVeQuestoesDasSuasDisciplinas() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(escopoProfessor.disciplinaIdsDoProfessor(PROFESSOR_ID)).willReturn(Set.of(DISC_A, DISC_B));
        given(questaoRepository.findByDisciplina_IdIn(Set.of(DISC_A, DISC_B)))
                .willReturn(List.of(questao(1L, DISC_A), questao(2L, DISC_B)));

        assertThat(questaoService.listar()).hasSize(2);

        verify(questaoRepository, times(1)).findByDisciplina_IdIn(any());
        verify(questaoRepository, never()).findAll();
    }

    @Test
    @DisplayName("professor sem disciplinas recebe lista vazia sem consulta global")
    void professorSemDisciplinasNaoConsultaQuestoes() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(escopoProfessor.disciplinaIdsDoProfessor(PROFESSOR_ID)).willReturn(Set.of());

        assertThat(questaoService.listar()).isEmpty();

        verifyNoInteractions(questaoRepository);
    }

    @Test
    @DisplayName("aluno recebe 403 em /questoes sem consultar nada")
    void alunoNaoListaQuestoes() {
        AuthorizationTestSupport.autenticarComoAluno(ALUNO_ID);

        assertForbidden(() -> questaoService.listar());
        verifyNoInteractions(questaoRepository, escopoProfessor);
    }

    @Test
    @DisplayName("aluno recebe 403 em /alternativas sem consultar nada")
    void alunoNaoListaAlternativas() {
        AuthorizationTestSupport.autenticarComoAluno(ALUNO_ID);

        assertForbidden(() -> alternativaService.listar());
        verifyNoInteractions(alternativaRepository, escopoProfessor);
    }

    @Test
    @DisplayName("sem autenticacao: 403 em questoes e em alternativas")
    void semAutenticacaoRecehe403() {
        AuthorizationTestSupport.limparContexto();

        assertForbidden(() -> questaoService.listar());
        assertForbidden(() -> alternativaService.listar());
        verifyNoInteractions(questaoRepository, alternativaRepository);
    }

    @Test
    @DisplayName("administrador mantem o acesso administrativo atual")
    void administradorMantemAcesso() {
        AuthorizationTestSupport.autenticarComoAdministrador();
        given(questaoRepository.findAll()).willReturn(List.of(questao(1L, DISC_A), questao(2L, null)));
        given(alternativaRepository.findAll()).willReturn(List.of(new Alternativa()));

        assertThat(questaoService.listar()).hasSize(2);
        assertThat(alternativaService.listar()).hasSize(1);

        verifyNoInteractions(escopoProfessor);
    }

    @Test
    @DisplayName("questao sem disciplina e exclusiva do administrador")
    void questaoSemDisciplinaEhSoDoAdministrador() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(questaoRepository.findById(QUESTAO_ID)).willReturn(Optional.of(questao(QUESTAO_ID, null)));

        assertForbidden(() -> questaoService.buscar(QUESTAO_ID));

        // Sem disciplina nao ha o que comparar: o guard recusa antes de consultar o escopo.
        verifyNoInteractions(escopoProfessor);
    }

    @Test
    @DisplayName("professor nao acessa questao de disciplina alheia por ID")
    void professorNaoAcessaQuestaoDeDisciplinaAlheia() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(escopoProfessor.disciplinaIdsDoProfessor(PROFESSOR_ID)).willReturn(Set.of(DISC_A));
        given(questaoRepository.findById(QUESTAO_ID)).willReturn(Optional.of(questao(QUESTAO_ID, DISC_B)));

        assertForbidden(() -> questaoService.buscar(QUESTAO_ID));
    }

    @Test
    @DisplayName("professor acessa questao da propria disciplina")
    void professorAcessaQuestaoDaPropriaDisciplina() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(escopoProfessor.disciplinaIdsDoProfessor(PROFESSOR_ID)).willReturn(Set.of(DISC_A));
        given(questaoRepository.findById(QUESTAO_ID)).willReturn(Optional.of(questao(QUESTAO_ID, DISC_A)));

        assertThat(questaoService.buscar(QUESTAO_ID).getId()).isEqualTo(QUESTAO_ID);
    }

    @Test
    @DisplayName("questao inexistente continua 404 (base do guard de /{id}/conteudos)")
    void questaoInexistenteResponde404() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(questaoRepository.findById(QUESTAO_ID)).willReturn(Optional.empty());

        try {
            questaoService.buscar(QUESTAO_ID);
            fail("esperava RecursoNaoEncontradoException");
        } catch (RecursoNaoEncontradoException esperado) {
            verifyNoInteractions(escopoProfessor);
        }
    }

    @Test
    @DisplayName("pendentes sao filtradas pelas disciplinas do professor")
    void pendentesFiltradasPorDisciplina() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(escopoProfessor.disciplinaIdsDoProfessor(PROFESSOR_ID)).willReturn(Set.of(DISC_A));
        given(questaoRepository.findByStatusAndDisciplina_IdIn(StatusQuestao.PENDENTE, Set.of(DISC_A)))
                .willReturn(List.of(questao(1L, DISC_A)));

        assertThat(questaoService.listarPendentes()).hasSize(1);

        verify(questaoRepository, never()).findByStatus(any());
    }

    @Test
    @DisplayName("pendentes: administrador mantem a visao completa")
    void pendentesAdministrador() {
        AuthorizationTestSupport.autenticarComoAdministrador();
        given(questaoRepository.findByStatus(StatusQuestao.PENDENTE)).willReturn(List.of(new Questao()));

        assertThat(questaoService.listarPendentes()).hasSize(1);
    }

    @Test
    @DisplayName("alternativas sao filtradas pelas disciplinas do professor, em uma consulta")
    void alternativasFiltradasPorDisciplina() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(escopoProfessor.disciplinaIdsDoProfessor(PROFESSOR_ID)).willReturn(Set.of(DISC_A, DISC_B));
        given(alternativaRepository.findByQuestao_Disciplina_IdIn(Set.of(DISC_A, DISC_B)))
                .willReturn(List.of(new Alternativa(), new Alternativa()));

        assertThat(alternativaService.listar()).hasSize(2);

        verify(alternativaRepository, times(1)).findByQuestao_Disciplina_IdIn(any());
        verify(alternativaRepository, never()).findAll();
    }

    @Test
    @DisplayName("professor sem disciplinas nao consulta alternativas")
    void professorSemDisciplinasNaoConsultaAlternativas() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(escopoProfessor.disciplinaIdsDoProfessor(PROFESSOR_ID)).willReturn(Set.of());

        assertThat(alternativaService.listar()).isEmpty();

        verifyNoInteractions(alternativaRepository);
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

    private static Questao questao(long id, Long disciplinaId) {
        Questao questao = new Questao();
        questao.setId(id);
        questao.setEnunciado("Enunciado " + id);
        questao.setTipo(TipoQuestao.ALTERNATIVAS);
        if (disciplinaId != null) {
            Disciplina disciplina = new Disciplina();
            disciplina.setId(disciplinaId);
            questao.setDisciplina(disciplina);
        }
        return questao;
    }
}
