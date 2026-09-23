package studojurata_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import studojurata_api.exception.RegraNegocioException;
import studojurata_api.model.Alternativa;
import studojurata_api.model.Disciplina;
import studojurata_api.model.Questao;
import studojurata_api.model.enums.TipoQuestao;
import studojurata_api.repository.AlternativaRepository;
import studojurata_api.security.EscopoProfessor;
import studojurata_api.security.UsuarioAutenticado;
import studojurata_api.support.AuthorizationTestSupport;

/**
 * C2.7d (passo 2) — escrita de alternativas (POST/PUT /alternativas).
 *
 * <p>Complementa o {@code C2QuestaoAlternativaEscopoTest} (C2.7a, leitura) com o
 * lado da escrita: a alternativa herda o escopo da questão (a disciplina é da
 * questão, não da alternativa) e o PUT carrega a alternativa existente antes de
 * alterar, para que um id não sirva de passe para editar alternativa de outro
 * professor nem para mover o gabarito para fora do escopo.
 */
@ExtendWith(MockitoExtension.class)
class C2AlternativaEscritaEscopoTest {

    private static final long ALUNO_ID = 7L;
    private static final long PROFESSOR_ID = 42L;
    private static final long DISC_A = 10L;
    private static final long DISC_B = 20L;
    private static final long QUESTAO_ID = 100L;
    private static final long OUTRA_QUESTAO_ID = 200L;
    private static final long ALTERNATIVA_ID = 500L;

    @Mock private AlternativaRepository alternativaRepository;
    @Mock private EscopoProfessor escopoProfessor;

    private AlternativaService alternativaService;

    @BeforeEach
    void setUp() {
        alternativaService = new AlternativaService(
                alternativaRepository, new UsuarioAutenticado(), escopoProfessor);
    }

    @AfterEach
    void limparContexto() {
        AuthorizationTestSupport.limparContexto();
    }

    // --- POST /alternativas ------------------------------------------------

    @Test
    @DisplayName("professor cria alternativa em questão da disciplina que leciona")
    void professorCriaAlternativaEmQuestaoPropria() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(escopoProfessor.disciplinaIdsDoProfessor(PROFESSOR_ID)).willReturn(Set.of(DISC_A));
        given(alternativaRepository.save(any())).willAnswer(chamada -> chamada.getArgument(0));

        Alternativa salva = alternativaService.salvar(alternativa(null, questao(QUESTAO_ID, DISC_A)));

        assertThat(salva.getQuestao().getId()).isEqualTo(QUESTAO_ID);
        assertThat(salva.getQuestao().getDisciplina().getId()).isEqualTo(DISC_A);
        verify(alternativaRepository).save(any());
    }

    @Test
    @DisplayName("professor cria a alternativa correta quando a questão ainda não tem uma")
    void professorCriaAlternativaCorreta() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(escopoProfessor.disciplinaIdsDoProfessor(PROFESSOR_ID)).willReturn(Set.of(DISC_A));
        given(alternativaRepository.findByQuestaoIdAndCorretaTrue(QUESTAO_ID)).willReturn(List.of());
        given(alternativaRepository.save(any())).willAnswer(chamada -> chamada.getArgument(0));

        Alternativa enviada = alternativa(null, questao(QUESTAO_ID, DISC_A));
        enviada.setCorreta(true);

        assertThat(alternativaService.salvar(enviada).getCorreta()).isTrue();
    }

    @Test
    @DisplayName("segunda alternativa correta na mesma questão continua recusada")
    void segundaCorretaEhRecusada() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(escopoProfessor.disciplinaIdsDoProfessor(PROFESSOR_ID)).willReturn(Set.of(DISC_A));
        given(alternativaRepository.findByQuestaoIdAndCorretaTrue(QUESTAO_ID))
                .willReturn(List.of(alternativa(90L, questao(QUESTAO_ID, DISC_A))));

        Alternativa enviada = alternativa(null, questao(QUESTAO_ID, DISC_A));
        enviada.setCorreta(true);

        assertThatThrownBy(() -> alternativaService.salvar(enviada))
                .isInstanceOf(RegraNegocioException.class);
        verify(alternativaRepository, never()).save(any());
    }

    @Test
    @DisplayName("em questão VERDADEIRO_FALSO várias afirmações podem ser verdadeiras")
    void verdadeiroFalsoAceitaVariasVerdadeiras() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(escopoProfessor.disciplinaIdsDoProfessor(PROFESSOR_ID)).willReturn(Set.of(DISC_A));
        given(alternativaRepository.save(any())).willAnswer(chamada -> chamada.getArgument(0));

        Alternativa enviada = alternativa(null, questao(QUESTAO_ID, DISC_A, TipoQuestao.VERDADEIRO_FALSO));
        enviada.setCorreta(true);

        assertThat(alternativaService.salvar(enviada).getCorreta()).isTrue();

        // A regra da correta única nem consulta o repositório em V/F.
        verify(alternativaRepository, never()).findByQuestaoIdAndCorretaTrue(any());
    }

    @Test
    @DisplayName("professor não cria alternativa em questão de disciplina alheia")
    void professorNaoCriaAlternativaEmDisciplinaAlheia() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(escopoProfessor.disciplinaIdsDoProfessor(PROFESSOR_ID)).willReturn(Set.of(DISC_A));

        assertForbidden(() -> alternativaService.salvar(alternativa(null, questao(QUESTAO_ID, DISC_B))));

        verify(alternativaRepository, never()).save(any());
    }

    @Test
    @DisplayName("professor não cria alternativa em questão sem disciplina (sem consultar escopo)")
    void professorNaoCriaAlternativaEmQuestaoSemDisciplina() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);

        assertForbidden(() -> alternativaService.salvar(alternativa(null, questao(QUESTAO_ID, null))));

        verify(alternativaRepository, never()).save(any());
        verifyNoInteractions(escopoProfessor);
    }

    @Test
    @DisplayName("professor não cria alternativa sem questão")
    void professorNaoCriaAlternativaSemQuestao() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);

        assertForbidden(() -> alternativaService.salvar(alternativa(null, null)));

        verify(alternativaRepository, never()).save(any());
        verifyNoInteractions(escopoProfessor);
    }

    @Test
    @DisplayName("professor sem disciplinas não cria alternativa")
    void professorSemDisciplinasNaoCriaAlternativa() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(escopoProfessor.disciplinaIdsDoProfessor(PROFESSOR_ID)).willReturn(Set.of());

        assertForbidden(() -> alternativaService.salvar(alternativa(null, questao(QUESTAO_ID, DISC_A))));

        verify(alternativaRepository, never()).save(any());
    }

    // --- PUT /alternativas/{id} --------------------------------------------

    @Test
    @DisplayName("professor atualiza a própria alternativa sem repetir a consulta de escopo")
    void professorAtualizaAlternativaPropria() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(escopoProfessor.disciplinaIdsDoProfessor(PROFESSOR_ID)).willReturn(Set.of(DISC_A));
        given(alternativaRepository.findById(ALTERNATIVA_ID))
                .willReturn(Optional.of(alternativa(ALTERNATIVA_ID, questao(QUESTAO_ID, DISC_A))));
        given(alternativaRepository.save(any())).willAnswer(chamada -> chamada.getArgument(0));

        Alternativa enviada = alternativa(null, questao(QUESTAO_ID, DISC_A));
        enviada.setTexto("Texto revisado");

        Alternativa salva = alternativaService.atualizar(ALTERNATIVA_ID, enviada);

        assertThat(salva.getId()).isEqualTo(ALTERNATIVA_ID);
        assertThat(salva.getTexto()).isEqualTo("Texto revisado");
        assertThat(salva.getQuestao().getId()).isEqualTo(QUESTAO_ID);
        // Mesma questão no corpo e no banco: uma ida ao repositório de vínculos.
        verify(escopoProfessor, times(1)).disciplinaIdsDoProfessor(PROFESSOR_ID);
    }

    @Test
    @DisplayName("corpo sem questaoId preserva o vínculo atual da alternativa")
    void corpoSemQuestaoMantemVinculo() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(escopoProfessor.disciplinaIdsDoProfessor(PROFESSOR_ID)).willReturn(Set.of(DISC_A));
        given(alternativaRepository.findById(ALTERNATIVA_ID))
                .willReturn(Optional.of(alternativa(ALTERNATIVA_ID, questao(QUESTAO_ID, DISC_A))));
        given(alternativaRepository.save(any())).willAnswer(chamada -> chamada.getArgument(0));

        Alternativa enviada = new Alternativa();
        enviada.setTexto("Só o texto mudou");

        Alternativa salva = alternativaService.atualizar(ALTERNATIVA_ID, enviada);

        assertThat(salva.getQuestao().getId()).isEqualTo(QUESTAO_ID);
        assertThat(salva.getTexto()).isEqualTo("Só o texto mudou");
    }

    @Test
    @DisplayName("professor não atualiza alternativa de questão alheia")
    void professorNaoAtualizaAlternativaDeQuestaoAlheia() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(escopoProfessor.disciplinaIdsDoProfessor(PROFESSOR_ID)).willReturn(Set.of(DISC_A));
        given(alternativaRepository.findById(ALTERNATIVA_ID))
                .willReturn(Optional.of(alternativa(ALTERNATIVA_ID, questao(QUESTAO_ID, DISC_B))));

        assertForbidden(() -> alternativaService.atualizar(
                ALTERNATIVA_ID, alternativa(null, questao(QUESTAO_ID, DISC_B))));

        verify(alternativaRepository, never()).save(any());
    }

    @Test
    @DisplayName("professor não move a alternativa para questão alheia")
    void professorNaoMoveAlternativaParaQuestaoAlheia() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(escopoProfessor.disciplinaIdsDoProfessor(PROFESSOR_ID)).willReturn(Set.of(DISC_A));
        given(alternativaRepository.findById(ALTERNATIVA_ID))
                .willReturn(Optional.of(alternativa(ALTERNATIVA_ID, questao(QUESTAO_ID, DISC_A))));

        assertForbidden(() -> alternativaService.atualizar(
                ALTERNATIVA_ID, alternativa(null, questao(OUTRA_QUESTAO_ID, DISC_B))));

        verify(alternativaRepository, never()).save(any());
    }

    @Test
    @DisplayName("professor não move a alternativa para questão sem disciplina")
    void professorNaoMoveAlternativaParaQuestaoSemDisciplina() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(escopoProfessor.disciplinaIdsDoProfessor(PROFESSOR_ID)).willReturn(Set.of(DISC_A));
        given(alternativaRepository.findById(ALTERNATIVA_ID))
                .willReturn(Optional.of(alternativa(ALTERNATIVA_ID, questao(QUESTAO_ID, DISC_A))));

        assertForbidden(() -> alternativaService.atualizar(
                ALTERNATIVA_ID, alternativa(null, questao(OUTRA_QUESTAO_ID, null))));

        verify(alternativaRepository, never()).save(any());
    }

    @Test
    @DisplayName("professor move a alternativa entre duas questões que leciona")
    void professorMoveAlternativaDentroDoEscopo() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(escopoProfessor.disciplinaIdsDoProfessor(PROFESSOR_ID)).willReturn(Set.of(DISC_A, DISC_B));
        given(alternativaRepository.findById(ALTERNATIVA_ID))
                .willReturn(Optional.of(alternativa(ALTERNATIVA_ID, questao(QUESTAO_ID, DISC_A))));
        given(alternativaRepository.save(any())).willAnswer(chamada -> chamada.getArgument(0));

        Alternativa salva = alternativaService.atualizar(
                ALTERNATIVA_ID, alternativa(null, questao(OUTRA_QUESTAO_ID, DISC_B)));

        assertThat(salva.getQuestao().getId()).isEqualTo(OUTRA_QUESTAO_ID);
        // Escopo da questão atual e da questão de destino.
        verify(escopoProfessor, times(2)).disciplinaIdsDoProfessor(PROFESSOR_ID);
    }

    @Test
    @DisplayName("atualização não promove uma segunda alternativa correta na mesma questão")
    void atualizacaoNaoCriaSegundaCorreta() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(escopoProfessor.disciplinaIdsDoProfessor(PROFESSOR_ID)).willReturn(Set.of(DISC_A));
        given(alternativaRepository.findById(ALTERNATIVA_ID))
                .willReturn(Optional.of(alternativa(ALTERNATIVA_ID, questao(QUESTAO_ID, DISC_A))));
        given(alternativaRepository.findByQuestaoIdAndCorretaTrue(QUESTAO_ID))
                .willReturn(List.of(alternativa(91L, questao(QUESTAO_ID, DISC_A))));

        Alternativa enviada = alternativa(null, questao(QUESTAO_ID, DISC_A));
        enviada.setCorreta(true);

        assertThatThrownBy(() -> alternativaService.atualizar(ALTERNATIVA_ID, enviada))
                .isInstanceOf(RegraNegocioException.class);
        verify(alternativaRepository, never()).save(any());
    }

    @Test
    @DisplayName("atualizar a própria alternativa correta não conflita consigo mesma")
    void atualizacaoDaPropriaCorretaEhPermitida() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(escopoProfessor.disciplinaIdsDoProfessor(PROFESSOR_ID)).willReturn(Set.of(DISC_A));
        Alternativa existente = alternativa(ALTERNATIVA_ID, questao(QUESTAO_ID, DISC_A));
        existente.setCorreta(true);
        given(alternativaRepository.findById(ALTERNATIVA_ID)).willReturn(Optional.of(existente));
        given(alternativaRepository.findByQuestaoIdAndCorretaTrue(QUESTAO_ID)).willReturn(List.of(existente));
        given(alternativaRepository.save(any())).willAnswer(chamada -> chamada.getArgument(0));

        Alternativa enviada = alternativa(null, questao(QUESTAO_ID, DISC_A));
        enviada.setCorreta(true);
        enviada.setTexto("Enunciado da correta, revisado");

        assertThat(alternativaService.atualizar(ALTERNATIVA_ID, enviada).getTexto())
                .isEqualTo("Enunciado da correta, revisado");
    }

    @Test
    @DisplayName("alternativa inexistente responde 404 sem consultar escopo")
    void alternativaInexistenteResponde404() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(alternativaRepository.findById(ALTERNATIVA_ID)).willReturn(Optional.empty());

        try {
            alternativaService.atualizar(ALTERNATIVA_ID, alternativa(null, questao(QUESTAO_ID, DISC_A)));
            fail("esperava RecursoNaoEncontradoException");
        } catch (RecursoNaoEncontradoException esperado) {
            verifyNoInteractions(escopoProfessor);
            verify(alternativaRepository, never()).save(any());
        }
    }

    // --- ADMINISTRADOR -----------------------------------------------------

    @Test
    @DisplayName("administrador cria alternativa em questão sem disciplina, sem consultar escopo")
    void administradorCriaAlternativaSemDisciplina() {
        AuthorizationTestSupport.autenticarComoAdministrador();
        given(alternativaRepository.save(any())).willAnswer(chamada -> chamada.getArgument(0));

        Alternativa salva = alternativaService.salvar(alternativa(null, questao(QUESTAO_ID, null)));

        assertThat(salva.getQuestao().getId()).isEqualTo(QUESTAO_ID);
        verifyNoInteractions(escopoProfessor);
    }

    @Test
    @DisplayName("administrador atualiza e move alternativa livremente")
    void administradorAtualizaAlternativa() {
        AuthorizationTestSupport.autenticarComoAdministrador();
        given(alternativaRepository.findById(ALTERNATIVA_ID))
                .willReturn(Optional.of(alternativa(ALTERNATIVA_ID, questao(QUESTAO_ID, DISC_B))));
        given(alternativaRepository.save(any())).willAnswer(chamada -> chamada.getArgument(0));

        Alternativa salva = alternativaService.atualizar(
                ALTERNATIVA_ID, alternativa(null, questao(OUTRA_QUESTAO_ID, null)));

        assertThat(salva.getQuestao().getId()).isEqualTo(OUTRA_QUESTAO_ID);
        verifyNoInteractions(escopoProfessor);
    }

    // --- Perfis sem permissão ----------------------------------------------

    @Test
    @DisplayName("aluno recebe 403 ao criar e ao editar alternativa, sem tocar no repositório")
    void alunoNaoEscreveAlternativa() {
        AuthorizationTestSupport.autenticarComoAluno(ALUNO_ID);

        assertForbidden(() -> alternativaService.salvar(alternativa(null, questao(QUESTAO_ID, DISC_A))));
        assertForbidden(() -> alternativaService.atualizar(ALTERNATIVA_ID, alternativa(null, questao(QUESTAO_ID, DISC_A))));

        verifyNoInteractions(alternativaRepository, escopoProfessor);
    }

    @Test
    @DisplayName("sem autenticação: 403 ao criar e ao editar alternativa")
    void semAutenticacaoNaoEscreveAlternativa() {
        AuthorizationTestSupport.limparContexto();

        assertForbidden(() -> alternativaService.salvar(alternativa(null, questao(QUESTAO_ID, DISC_A))));
        assertForbidden(() -> alternativaService.atualizar(ALTERNATIVA_ID, alternativa(null, questao(QUESTAO_ID, DISC_A))));

        verifyNoInteractions(alternativaRepository, escopoProfessor);
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

    private static Questao questao(Long id, Long disciplinaId) {
        return questao(id, disciplinaId, TipoQuestao.ALTERNATIVAS);
    }

    private static Questao questao(Long id, Long disciplinaId, TipoQuestao tipo) {
        Questao questao = new Questao();
        questao.setId(id);
        questao.setEnunciado("Enunciado " + id);
        questao.setTipo(tipo);
        if (disciplinaId != null) {
            Disciplina disciplina = new Disciplina();
            disciplina.setId(disciplinaId);
            questao.setDisciplina(disciplina);
        }
        return questao;
    }

    private static Alternativa alternativa(Long id, Questao questao) {
        Alternativa alternativa = new Alternativa();
        alternativa.setId(id);
        alternativa.setQuestao(questao);
        alternativa.setTexto("Alternativa " + id);
        alternativa.setCorreta(Boolean.FALSE);
        alternativa.setOrdem(1);
        return alternativa;
    }
}
