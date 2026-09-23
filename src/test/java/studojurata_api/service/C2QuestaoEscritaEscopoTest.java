package studojurata_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

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

import studojurata_api.model.Disciplina;
import studojurata_api.model.Questao;
import studojurata_api.model.enums.OrigemQuestao;
import studojurata_api.model.enums.StatusQuestao;
import studojurata_api.model.enums.TipoQuestao;
import studojurata_api.repository.QuestaoRepository;
import studojurata_api.security.EscopoProfessor;
import studojurata_api.security.UsuarioAutenticado;
import studojurata_api.support.AuthorizationTestSupport;

/**
 * C2.7d — escrita de questoes (POST/PUT /questoes).
 *
 * <p>Complementa o {@code C2QuestaoAlternativaEscopoTest} (C2.7a, leitura):
 * aqui o objeto e o outro lado da balanca — quem pode <b>gravar</b>. O
 * professor so escreve nas disciplinas que leciona e nao define origem nem
 * status de moderacao; o administrador segue livre; aluno e anonimo recebem 403.
 */
@ExtendWith(MockitoExtension.class)
class C2QuestaoEscritaEscopoTest {

    private static final long ALUNO_ID = 7L;
    private static final long PROFESSOR_ID = 42L;
    private static final long DISC_A = 10L;
    private static final long DISC_B = 20L;
    private static final long QUESTAO_ID = 100L;

    @Mock private QuestaoRepository questaoRepository;
    @Mock private EscopoProfessor escopoProfessor;

    private QuestaoService questaoService;

    @BeforeEach
    void setUp() {
        questaoService = new QuestaoService(questaoRepository, new UsuarioAutenticado(), escopoProfessor);
    }

    @AfterEach
    void limparContexto() {
        AuthorizationTestSupport.limparContexto();
    }

    // --- POST /questoes ----------------------------------------------------

    @Test
    @DisplayName("professor cria questao na disciplina que leciona")
    void professorCriaQuestaoNaPropriaDisciplina() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(escopoProfessor.disciplinaIdsDoProfessor(PROFESSOR_ID)).willReturn(Set.of(DISC_A, DISC_B));
        given(questaoRepository.save(any())).willAnswer(chamada -> chamada.getArgument(0));

        Questao salva = questaoService.salvar(novaQuestao(DISC_B));

        assertThat(salva.getDisciplina().getId()).isEqualTo(DISC_B);
        verify(questaoRepository).save(any());
    }

    @Test
    @DisplayName("origem e status enviados pelo cliente sao ignorados na criacao")
    void origemEStatusDoSaoDoServidor() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(escopoProfessor.disciplinaIdsDoProfessor(PROFESSOR_ID)).willReturn(Set.of(DISC_A));
        given(questaoRepository.save(any())).willAnswer(chamada -> chamada.getArgument(0));

        Questao enviada = novaQuestao(DISC_A);
        enviada.setOrigem(OrigemQuestao.IA);
        enviada.setStatus(StatusQuestao.PENDENTE);

        Questao salva = questaoService.salvar(enviada);

        // Regra do servidor para a criacao manual: PROFESSOR nasce APROVADA.
        assertThat(salva.getOrigem()).isEqualTo(OrigemQuestao.PROFESSOR);
        assertThat(salva.getStatus()).isEqualTo(StatusQuestao.APROVADA);
    }

    @Test
    @DisplayName("professor nao cria questao em disciplina alheia")
    void professorNaoCriaQuestaoEmDisciplinaAlheia() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(escopoProfessor.disciplinaIdsDoProfessor(PROFESSOR_ID)).willReturn(Set.of(DISC_A));

        assertForbidden(() -> questaoService.salvar(novaQuestao(DISC_B)));

        verify(questaoRepository, never()).save(any());
    }

    @Test
    @DisplayName("professor nao cria questao sem disciplina (sem consultar escopo)")
    void professorNaoCriaQuestaoSemDisciplina() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);

        assertForbidden(() -> questaoService.salvar(novaQuestao(null)));

        verify(questaoRepository, never()).save(any());
        verifyNoInteractions(escopoProfessor);
    }

    @Test
    @DisplayName("professor sem disciplinas nao cria questao")
    void professorSemDisciplinasNaoCriaQuestao() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(escopoProfessor.disciplinaIdsDoProfessor(PROFESSOR_ID)).willReturn(Set.of());

        assertForbidden(() -> questaoService.salvar(novaQuestao(DISC_A)));

        verify(questaoRepository, never()).save(any());
    }

    @Test
    @DisplayName("administrador cria questao sem disciplina e sem consultar escopo")
    void administradorCriaQuestaoSemDisciplina() {
        AuthorizationTestSupport.autenticarComoAdministrador();
        given(questaoRepository.save(any())).willAnswer(chamada -> chamada.getArgument(0));

        Questao enviada = novaQuestao(null);
        enviada.setOrigem(OrigemQuestao.IA);
        Questao salva = questaoService.salvar(enviada);

        assertThat(salva.getDisciplina()).isNull();
        assertThat(salva.getOrigem()).isEqualTo(OrigemQuestao.PROFESSOR);
        assertThat(salva.getStatus()).isEqualTo(StatusQuestao.APROVADA);
        verifyNoInteractions(escopoProfessor);
    }

    @Test
    @DisplayName("administrador cria questao em qualquer disciplina")
    void administradorCriaQuestaoEmDisciplinaDeTerceiros() {
        AuthorizationTestSupport.autenticarComoAdministrador();
        given(questaoRepository.save(any())).willAnswer(chamada -> chamada.getArgument(0));

        assertThat(questaoService.salvar(novaQuestao(DISC_B)).getDisciplina().getId()).isEqualTo(DISC_B);

        verifyNoInteractions(escopoProfessor);
    }

    // --- PUT /questoes/{id} ------------------------------------------------

    @Test
    @DisplayName("professor edita a propria questao sem alterar origem e status")
    void professorNaoAlteraOrigemNemStatusNaEdicao() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(escopoProfessor.disciplinaIdsDoProfessor(PROFESSOR_ID)).willReturn(Set.of(DISC_A));
        given(questaoRepository.findById(QUESTAO_ID))
                .willReturn(Optional.of(questao(QUESTAO_ID, DISC_A, OrigemQuestao.IA, StatusQuestao.PENDENTE)));
        given(questaoRepository.save(any())).willAnswer(chamada -> chamada.getArgument(0));

        Questao enviada = novaQuestao(DISC_A);
        enviada.setEnunciado("Enunciado revisado");
        enviada.setOrigem(OrigemQuestao.PROFESSOR);
        enviada.setStatus(StatusQuestao.APROVADA);

        Questao salva = questaoService.atualizar(QUESTAO_ID, enviada);

        assertThat(salva.getId()).isEqualTo(QUESTAO_ID);
        assertThat(salva.getEnunciado()).isEqualTo("Enunciado revisado");
        assertThat(salva.getOrigem()).isEqualTo(OrigemQuestao.IA);
        assertThat(salva.getStatus()).isEqualTo(StatusQuestao.PENDENTE);
    }

    @Test
    @DisplayName("professor nao move a propria questao para disciplina alheia")
    void professorNaoMoveQuestaoParaDisciplinaAlheia() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(escopoProfessor.disciplinaIdsDoProfessor(PROFESSOR_ID)).willReturn(Set.of(DISC_A));
        given(questaoRepository.findById(QUESTAO_ID)).willReturn(Optional.of(questaoExistente(QUESTAO_ID, DISC_A)));

        assertForbidden(() -> questaoService.atualizar(QUESTAO_ID, novaQuestao(DISC_B)));

        verify(questaoRepository, never()).save(any());
    }

    @Test
    @DisplayName("professor nao edita questao de disciplina alheia (escopo do C2.7a preservado)")
    void professorNaoEditaQuestaoDeDisciplinaAlheia() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(escopoProfessor.disciplinaIdsDoProfessor(PROFESSOR_ID)).willReturn(Set.of(DISC_A));
        given(questaoRepository.findById(QUESTAO_ID)).willReturn(Optional.of(questaoExistente(QUESTAO_ID, DISC_B)));

        assertForbidden(() -> questaoService.atualizar(QUESTAO_ID, novaQuestao(DISC_A)));

        verify(questaoRepository, never()).save(any());
    }

    @Test
    @DisplayName("administrador edita questao de qualquer disciplina; origem/status seguem o registro")
    void administradorEditaLivremente() {
        AuthorizationTestSupport.autenticarComoAdministrador();
        given(questaoRepository.findById(QUESTAO_ID))
                .willReturn(Optional.of(questao(QUESTAO_ID, null, OrigemQuestao.IA, StatusQuestao.REJEITADA)));
        given(questaoRepository.save(any())).willAnswer(chamada -> chamada.getArgument(0));

        Questao enviada = novaQuestao(DISC_B);
        enviada.setOrigem(OrigemQuestao.PROFESSOR);
        enviada.setStatus(StatusQuestao.APROVADA);

        Questao salva = questaoService.atualizar(QUESTAO_ID, enviada);

        assertThat(salva.getDisciplina().getId()).isEqualTo(DISC_B);
        assertThat(salva.getOrigem()).isEqualTo(OrigemQuestao.IA);
        assertThat(salva.getStatus()).isEqualTo(StatusQuestao.REJEITADA);
        verifyNoInteractions(escopoProfessor);
    }

    // --- Perfis sem permissao ----------------------------------------------

    @Test
    @DisplayName("aluno recebe 403 ao criar e ao editar questao")
    void alunoNaoEscreveQuestao() {
        AuthorizationTestSupport.autenticarComoAluno(ALUNO_ID);
        given(questaoRepository.findById(QUESTAO_ID)).willReturn(Optional.of(questaoExistente(QUESTAO_ID, DISC_A)));

        assertForbidden(() -> questaoService.salvar(novaQuestao(DISC_A)));
        assertForbidden(() -> questaoService.atualizar(QUESTAO_ID, novaQuestao(DISC_A)));

        verify(questaoRepository, never()).save(any());
        verifyNoInteractions(escopoProfessor);
    }

    @Test
    @DisplayName("sem autenticacao: 403 ao criar e ao editar questao")
    void semAutenticacaoNaoEscreveQuestao() {
        AuthorizationTestSupport.limparContexto();
        given(questaoRepository.findById(QUESTAO_ID)).willReturn(Optional.of(questaoExistente(QUESTAO_ID, DISC_A)));

        assertForbidden(() -> questaoService.salvar(novaQuestao(DISC_A)));
        assertForbidden(() -> questaoService.atualizar(QUESTAO_ID, novaQuestao(DISC_A)));

        verify(questaoRepository, never()).save(any());
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

    /** Questao enviada pelo cliente na criacao: sem id, sem origem/status. */
    private static Questao novaQuestao(Long disciplinaId) {
        return questao(null, disciplinaId, null, null);
    }

    /** Questao como esta no banco. */
    private static Questao questaoExistente(Long id, Long disciplinaId) {
        return questao(id, disciplinaId, null, null);
    }

    private static Questao questao(Long id, Long disciplinaId, OrigemQuestao origem, StatusQuestao status) {
        Questao questao = new Questao();
        questao.setId(id);
        questao.setEnunciado("Enunciado " + disciplinaId);
        questao.setTipo(TipoQuestao.ALTERNATIVAS);
        if (disciplinaId != null) {
            Disciplina disciplina = new Disciplina();
            disciplina.setId(disciplinaId);
            questao.setDisciplina(disciplina);
        }
        questao.setOrigem(origem);
        questao.setStatus(status);
        return questao;
    }
}
