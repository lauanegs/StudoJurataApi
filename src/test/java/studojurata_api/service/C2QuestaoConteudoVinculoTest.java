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
import studojurata_api.exception.RequisicaoInvalidaException;
import studojurata_api.model.ConteudoPlano;
import studojurata_api.model.Disciplina;
import studojurata_api.model.PlanoEnsino;
import studojurata_api.model.Questao;
import studojurata_api.model.QuestaoConteudo;
import studojurata_api.model.TurmaDisciplina;
import studojurata_api.model.enums.StatusAtivoInativo;
import studojurata_api.repository.ConteudoPlanoRepository;
import studojurata_api.repository.QuestaoConteudoRepository;
import studojurata_api.repository.QuestaoRepository;
import studojurata_api.security.EscopoProfessor;
import studojurata_api.security.UsuarioAutenticado;
import studojurata_api.support.AuthorizationTestSupport;

/**
 * C2.7d (passo 3) — escrita dos vínculos questão × conteúdo
 * (POST/DELETE /questoes/{id}/conteudos/{conteudoPlanoId}).
 *
 * <p>O vínculo cruza dois recursos: a questão (disciplina dela) e o conteúdo
 * (disciplina do plano de ensino). Basta uma das pontas fora do escopo para o
 * professor receber 403 — questão própria com conteúdo alheio é justamente o
 * buraco que este passo fecha.
 */
@ExtendWith(MockitoExtension.class)
class C2QuestaoConteudoVinculoTest {

    private static final long ALUNO_ID = 7L;
    private static final long PROFESSOR_ID = 42L;
    private static final long DISC_A = 10L;
    private static final long DISC_B = 20L;
    private static final long QUESTAO_ID = 100L;
    private static final long CONTEUDO_ID = 300L;

    @Mock private QuestaoConteudoRepository repository;
    @Mock private QuestaoRepository questaoRepository;
    @Mock private ConteudoPlanoRepository conteudoPlanoRepository;
    @Mock private EscopoProfessor escopoProfessor;

    private QuestaoConteudoService questaoConteudoService;

    @BeforeEach
    void setUp() {
        questaoConteudoService = new QuestaoConteudoService(
                repository, questaoRepository, conteudoPlanoRepository,
                new UsuarioAutenticado(), escopoProfessor);
    }

    @AfterEach
    void limparContexto() {
        AuthorizationTestSupport.limparContexto();
    }

    // --- POST /questoes/{id}/conteudos/{conteudoPlanoId} --------------------

    @Test
    @DisplayName("professor vincula questão e conteúdo das disciplinas que leciona")
    void professorVinculaQuestaoEConteudoProprios() {
        autenticarProfessorComDisciplinas(DISC_A);
        given(questaoRepository.findById(QUESTAO_ID)).willReturn(Optional.of(questao(QUESTAO_ID, DISC_A)));
        given(conteudoPlanoRepository.findById(CONTEUDO_ID))
                .willReturn(Optional.of(conteudo(CONTEUDO_ID, DISC_A, StatusAtivoInativo.ATIVO)));
        given(repository.save(any())).willAnswer(chamada -> chamada.getArgument(0));

        QuestaoConteudo vinculo = questaoConteudoService.vincular(QUESTAO_ID, CONTEUDO_ID);

        assertThat(vinculo.getQuestao().getId()).isEqualTo(QUESTAO_ID);
        assertThat(vinculo.getConteudoPlano().getId()).isEqualTo(CONTEUDO_ID);
        verify(repository).save(any());
        // Escopo resolvido uma única vez para as duas pontas.
        verify(escopoProfessor, times(1)).disciplinaIdsDoProfessor(PROFESSOR_ID);
    }

    @Test
    @DisplayName("professor não vincula questão de disciplina alheia")
    void professorNaoVinculaQuestaoForaDoEscopo() {
        autenticarProfessorComDisciplinas(DISC_A);
        given(questaoRepository.findById(QUESTAO_ID)).willReturn(Optional.of(questao(QUESTAO_ID, DISC_B)));
        given(conteudoPlanoRepository.findById(CONTEUDO_ID))
                .willReturn(Optional.of(conteudo(CONTEUDO_ID, DISC_A, StatusAtivoInativo.ATIVO)));

        assertForbidden(() -> questaoConteudoService.vincular(QUESTAO_ID, CONTEUDO_ID));

        verify(repository, never()).save(any());
        verify(repository, never()).existsByQuestao_IdAndConteudoPlano_Id(any(), any());
    }

    @Test
    @DisplayName("professor não vincula conteúdo de disciplina alheia")
    void professorNaoVinculaConteudoForaDoEscopo() {
        autenticarProfessorComDisciplinas(DISC_A);
        given(questaoRepository.findById(QUESTAO_ID)).willReturn(Optional.of(questao(QUESTAO_ID, DISC_A)));
        given(conteudoPlanoRepository.findById(CONTEUDO_ID))
                .willReturn(Optional.of(conteudo(CONTEUDO_ID, DISC_B, StatusAtivoInativo.ATIVO)));

        assertForbidden(() -> questaoConteudoService.vincular(QUESTAO_ID, CONTEUDO_ID));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("professor não vincula questão e conteúdo de disciplinas diferentes")
    void professorNaoVinculaDisciplinasDiferentes() {
        // Duas disciplinas do próprio professor: o recorte passa, a disciplina não casa.
        autenticarProfessorComDisciplinas(DISC_A, DISC_B);
        given(questaoRepository.findById(QUESTAO_ID)).willReturn(Optional.of(questao(QUESTAO_ID, DISC_A)));
        given(conteudoPlanoRepository.findById(CONTEUDO_ID))
                .willReturn(Optional.of(conteudo(CONTEUDO_ID, DISC_B, StatusAtivoInativo.ATIVO)));

        assertThatThrownBy(() -> questaoConteudoService.vincular(QUESTAO_ID, CONTEUDO_ID))
                .isInstanceOf(RequisicaoInvalidaException.class);

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("professor não vincula conteúdo inativado")
    void professorNaoVinculaConteudoInativo() {
        autenticarProfessorComDisciplinas(DISC_A);
        given(questaoRepository.findById(QUESTAO_ID)).willReturn(Optional.of(questao(QUESTAO_ID, DISC_A)));
        given(conteudoPlanoRepository.findById(CONTEUDO_ID))
                .willReturn(Optional.of(conteudo(CONTEUDO_ID, DISC_A, StatusAtivoInativo.INATIVO)));

        assertThatThrownBy(() -> questaoConteudoService.vincular(QUESTAO_ID, CONTEUDO_ID))
                .isInstanceOf(RegraNegocioException.class);

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("professor não vincula questão sem disciplina")
    void professorNaoVinculaQuestaoSemDisciplina() {
        autenticarProfessorComDisciplinas(DISC_A);
        given(questaoRepository.findById(QUESTAO_ID)).willReturn(Optional.of(questao(QUESTAO_ID, null)));
        given(conteudoPlanoRepository.findById(CONTEUDO_ID))
                .willReturn(Optional.of(conteudo(CONTEUDO_ID, DISC_A, StatusAtivoInativo.ATIVO)));

        assertForbidden(() -> questaoConteudoService.vincular(QUESTAO_ID, CONTEUDO_ID));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("professor não vincula conteúdo sem disciplina (plano genérico)")
    void professorNaoVinculaConteudoSemDisciplina() {
        autenticarProfessorComDisciplinas(DISC_A);
        given(questaoRepository.findById(QUESTAO_ID)).willReturn(Optional.of(questao(QUESTAO_ID, DISC_A)));
        given(conteudoPlanoRepository.findById(CONTEUDO_ID))
                .willReturn(Optional.of(conteudoSemPlano(CONTEUDO_ID, StatusAtivoInativo.ATIVO)));

        assertForbidden(() -> questaoConteudoService.vincular(QUESTAO_ID, CONTEUDO_ID));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("professor sem disciplinas não vincula nada")
    void professorSemDisciplinasNaoVincula() {
        autenticarProfessorComDisciplinas();
        given(questaoRepository.findById(QUESTAO_ID)).willReturn(Optional.of(questao(QUESTAO_ID, DISC_A)));
        given(conteudoPlanoRepository.findById(CONTEUDO_ID))
                .willReturn(Optional.of(conteudo(CONTEUDO_ID, DISC_A, StatusAtivoInativo.ATIVO)));

        assertForbidden(() -> questaoConteudoService.vincular(QUESTAO_ID, CONTEUDO_ID));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("vínculo repetido continua recusado")
    void vinculoRepetidoEhRecusado() {
        autenticarProfessorComDisciplinas(DISC_A);
        given(questaoRepository.findById(QUESTAO_ID)).willReturn(Optional.of(questao(QUESTAO_ID, DISC_A)));
        given(conteudoPlanoRepository.findById(CONTEUDO_ID))
                .willReturn(Optional.of(conteudo(CONTEUDO_ID, DISC_A, StatusAtivoInativo.ATIVO)));
        given(repository.existsByQuestao_IdAndConteudoPlano_Id(QUESTAO_ID, CONTEUDO_ID)).willReturn(true);

        assertThatThrownBy(() -> questaoConteudoService.vincular(QUESTAO_ID, CONTEUDO_ID))
                .isInstanceOf(RegraNegocioException.class);

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("questão inexistente responde 404 sem consultar escopo")
    void questaoInexistenteResponde404() {
        // Sem stub de escopo: o 404 acontece antes de qualquer consulta de recorte.
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(questaoRepository.findById(QUESTAO_ID)).willReturn(Optional.empty());

        try {
            questaoConteudoService.vincular(QUESTAO_ID, CONTEUDO_ID);
            fail("esperava RecursoNaoEncontradoException");
        } catch (RecursoNaoEncontradoException esperado) {
            verifyNoInteractions(escopoProfessor);
            verify(repository, never()).save(any());
        }
    }

    @Test
    @DisplayName("conteúdo inexistente responde 404 sem consultar escopo")
    void conteudoInexistenteResponde404() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(questaoRepository.findById(QUESTAO_ID)).willReturn(Optional.of(questao(QUESTAO_ID, DISC_A)));
        given(conteudoPlanoRepository.findById(CONTEUDO_ID)).willReturn(Optional.empty());

        try {
            questaoConteudoService.vincular(QUESTAO_ID, CONTEUDO_ID);
            fail("esperava RecursoNaoEncontradoException");
        } catch (RecursoNaoEncontradoException esperado) {
            verifyNoInteractions(escopoProfessor);
            verify(repository, never()).save(any());
        }
    }

    // --- DELETE /questoes/{id}/conteudos/{conteudoPlanoId} ------------------

    @Test
    @DisplayName("professor desvincula vínculo das suas disciplinas")
    void professorDesvinculaVinculoProprio() {
        autenticarProfessorComDisciplinas(DISC_A);
        given(questaoRepository.findById(QUESTAO_ID)).willReturn(Optional.of(questao(QUESTAO_ID, DISC_A)));
        given(conteudoPlanoRepository.findById(CONTEUDO_ID))
                .willReturn(Optional.of(conteudo(CONTEUDO_ID, DISC_A, StatusAtivoInativo.ATIVO)));

        questaoConteudoService.desvincular(QUESTAO_ID, CONTEUDO_ID);

        verify(repository).deleteByQuestao_IdAndConteudoPlano_Id(QUESTAO_ID, CONTEUDO_ID);
    }

    @Test
    @DisplayName("professor não desvincula questão de disciplina alheia")
    void professorNaoDesvinculaQuestaoForaDoEscopo() {
        autenticarProfessorComDisciplinas(DISC_A);
        given(questaoRepository.findById(QUESTAO_ID)).willReturn(Optional.of(questao(QUESTAO_ID, DISC_B)));
        given(conteudoPlanoRepository.findById(CONTEUDO_ID))
                .willReturn(Optional.of(conteudo(CONTEUDO_ID, DISC_B, StatusAtivoInativo.ATIVO)));

        assertForbidden(() -> questaoConteudoService.desvincular(QUESTAO_ID, CONTEUDO_ID));

        verify(repository, never()).deleteByQuestao_IdAndConteudoPlano_Id(any(), any());
    }

    @Test
    @DisplayName("professor não desvincula conteúdo de disciplina alheia")
    void professorNaoDesvinculaConteudoForaDoEscopo() {
        autenticarProfessorComDisciplinas(DISC_A);
        given(questaoRepository.findById(QUESTAO_ID)).willReturn(Optional.of(questao(QUESTAO_ID, DISC_A)));
        given(conteudoPlanoRepository.findById(CONTEUDO_ID))
                .willReturn(Optional.of(conteudo(CONTEUDO_ID, DISC_B, StatusAtivoInativo.ATIVO)));

        assertForbidden(() -> questaoConteudoService.desvincular(QUESTAO_ID, CONTEUDO_ID));

        verify(repository, never()).deleteByQuestao_IdAndConteudoPlano_Id(any(), any());
    }

    @Test
    @DisplayName("desvincular conteúdo inativado depois do vínculo continua permitido (limpeza)")
    void professorDesvinculaConteudoInativado() {
        autenticarProfessorComDisciplinas(DISC_A);
        given(questaoRepository.findById(QUESTAO_ID)).willReturn(Optional.of(questao(QUESTAO_ID, DISC_A)));
        given(conteudoPlanoRepository.findById(CONTEUDO_ID))
                .willReturn(Optional.of(conteudo(CONTEUDO_ID, DISC_A, StatusAtivoInativo.INATIVO)));

        questaoConteudoService.desvincular(QUESTAO_ID, CONTEUDO_ID);

        verify(repository).deleteByQuestao_IdAndConteudoPlano_Id(QUESTAO_ID, CONTEUDO_ID);
    }

    // --- ADMINISTRADOR -----------------------------------------------------

    @Test
    @DisplayName("administrador vincula e desvincula sem consultar escopo")
    void administradorVinculaEDesvincula() {
        AuthorizationTestSupport.autenticarComoAdministrador();
        given(questaoRepository.findById(QUESTAO_ID)).willReturn(Optional.of(questao(QUESTAO_ID, DISC_A)));
        given(conteudoPlanoRepository.findById(CONTEUDO_ID))
                .willReturn(Optional.of(conteudo(CONTEUDO_ID, DISC_A, StatusAtivoInativo.ATIVO)));
        given(repository.save(any())).willAnswer(chamada -> chamada.getArgument(0));

        assertThat(questaoConteudoService.vincular(QUESTAO_ID, CONTEUDO_ID).getConteudoPlano().getId())
                .isEqualTo(CONTEUDO_ID);
        questaoConteudoService.desvincular(QUESTAO_ID, CONTEUDO_ID);

        verify(repository).deleteByQuestao_IdAndConteudoPlano_Id(QUESTAO_ID, CONTEUDO_ID);
        verifyNoInteractions(escopoProfessor);
    }

    @Test
    @DisplayName("administrador também respeita a mesma disciplina")
    void administradorRespeitaMesmaDisciplina() {
        AuthorizationTestSupport.autenticarComoAdministrador();
        given(questaoRepository.findById(QUESTAO_ID)).willReturn(Optional.of(questao(QUESTAO_ID, DISC_A)));
        given(conteudoPlanoRepository.findById(CONTEUDO_ID))
                .willReturn(Optional.of(conteudo(CONTEUDO_ID, DISC_B, StatusAtivoInativo.ATIVO)));

        assertThatThrownBy(() -> questaoConteudoService.vincular(QUESTAO_ID, CONTEUDO_ID))
                .isInstanceOf(RequisicaoInvalidaException.class);
    }

    @Test
    @DisplayName("administrador não vincula conteúdo inativado")
    void administradorNaoVinculaConteudoInativo() {
        AuthorizationTestSupport.autenticarComoAdministrador();
        given(questaoRepository.findById(QUESTAO_ID)).willReturn(Optional.of(questao(QUESTAO_ID, DISC_A)));
        given(conteudoPlanoRepository.findById(CONTEUDO_ID))
                .willReturn(Optional.of(conteudo(CONTEUDO_ID, DISC_A, StatusAtivoInativo.INATIVO)));

        assertThatThrownBy(() -> questaoConteudoService.vincular(QUESTAO_ID, CONTEUDO_ID))
                .isInstanceOf(RegraNegocioException.class);
    }

    // --- Perfis sem permissão ----------------------------------------------

    @Test
    @DisplayName("aluno recebe 403 sem nenhuma consulta ao banco")
    void alunoNaoAlteraVinculo() {
        AuthorizationTestSupport.autenticarComoAluno(ALUNO_ID);

        assertForbidden(() -> questaoConteudoService.vincular(QUESTAO_ID, CONTEUDO_ID));
        assertForbidden(() -> questaoConteudoService.desvincular(QUESTAO_ID, CONTEUDO_ID));

        verifyNoInteractions(questaoRepository, conteudoPlanoRepository, repository, escopoProfessor);
    }

    @Test
    @DisplayName("sem autenticação: 403 sem nenhuma consulta ao banco")
    void semAutenticacaoNaoAlteraVinculo() {
        AuthorizationTestSupport.limparContexto();

        assertForbidden(() -> questaoConteudoService.vincular(QUESTAO_ID, CONTEUDO_ID));
        assertForbidden(() -> questaoConteudoService.desvincular(QUESTAO_ID, CONTEUDO_ID));

        verifyNoInteractions(questaoRepository, conteudoPlanoRepository, repository, escopoProfessor);
    }

    // --- helpers -----------------------------------------------------------

    private void autenticarProfessorComDisciplinas(Long... disciplinas) {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(escopoProfessor.disciplinaIdsDoProfessor(PROFESSOR_ID)).willReturn(Set.of(disciplinas));
    }

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
        Questao questao = new Questao();
        questao.setId(id);
        questao.setEnunciado("Enunciado " + id);
        if (disciplinaId != null) {
            questao.setDisciplina(disciplina(disciplinaId));
        }
        return questao;
    }

    private static ConteudoPlano conteudo(Long id, Long disciplinaId, StatusAtivoInativo status) {
        ConteudoPlano conteudo = new ConteudoPlano();
        conteudo.setId(id);
        conteudo.setTitulo("Conteúdo " + id);
        conteudo.setStatus(status);

        TurmaDisciplina turmaDisciplina = new TurmaDisciplina();
        turmaDisciplina.setId(id);
        turmaDisciplina.setDisciplina(disciplina(disciplinaId));

        PlanoEnsino planoEnsino = new PlanoEnsino();
        planoEnsino.setId(id);
        planoEnsino.setTurmaDisciplina(turmaDisciplina);
        conteudo.setPlanoEnsino(planoEnsino);
        return conteudo;
    }

    /** Conteúdo de plano genérico: sem turma, logo sem disciplina. */
    private static ConteudoPlano conteudoSemPlano(Long id, StatusAtivoInativo status) {
        ConteudoPlano conteudo = new ConteudoPlano();
        conteudo.setId(id);
        conteudo.setTitulo("Conteúdo " + id);
        conteudo.setStatus(status);
        return conteudo;
    }

    private static Disciplina disciplina(Long id) {
        Disciplina disciplina = new Disciplina();
        disciplina.setId(id);
        return disciplina;
    }
}
