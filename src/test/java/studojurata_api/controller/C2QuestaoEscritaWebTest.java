package studojurata_api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import studojurata_api.mapper.QuestaoMapper;
import studojurata_api.model.Disciplina;
import studojurata_api.model.Questao;
import studojurata_api.repository.DisciplinaRepository;
import studojurata_api.service.QuestaoConteudoService;
import studojurata_api.service.QuestaoService;
import studojurata_api.support.AuthorizationTestSupport;
import studojurata_api.support.AutorizacaoComEscopoTestBase;

/**
 * C2.7d — a escrita de questão vista pela rota, com o {@code SecurityConfig}
 * real: quem não é professor nem administrador é barrado antes do controller, e
 * o que chega ao serviço não carrega origem/status vindos do cliente.
 *
 * <p>O {@code QuestaoMapper} entra de verdade (por isso o JSON é convertido do
 * mesmo jeito que em produção); a regra de disciplina/escopo é do
 * {@code QuestaoService} e está coberta em {@code C2QuestaoEscritaEscopoTest}.
 */
@WebMvcTest(controllers = QuestaoController.class)
@Import(QuestaoMapper.class)
class C2QuestaoEscritaWebTest extends AutorizacaoComEscopoTestBase {

    private static final long ALUNO_ID = 7L;
    private static final long PROFESSOR_ID = 42L;
    private static final long DISC_A = 10L;
    private static final long QUESTAO_ID = 100L;

    @MockBean private QuestaoService questaoService;
    @MockBean private QuestaoConteudoService questaoConteudoService;
    @MockBean private DisciplinaRepository disciplinaRepository;

    @AfterEach
    void limparContexto() {
        AuthorizationTestSupport.limparContexto();
    }

    // --- Negação por papel -------------------------------------------------

    @Test
    @DisplayName("sem autenticação não cria nem edita questão")
    void semAutenticacaoNaoEscreveQuestao() throws Exception {
        mockMvc.perform(post("/questoes").contentType(JSON).content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/questoes/{id}", QUESTAO_ID).contentType(JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("aluno não cria nem edita questão")
    void alunoNaoEscreveQuestao() throws Exception {
        mockMvc.perform(post("/questoes").contentType(JSON).content("{}").with(comoAluno(ALUNO_ID)))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/questoes/{id}", QUESTAO_ID).contentType(JSON).content("{}").with(comoAluno(ALUNO_ID)))
                .andExpect(status().isForbidden());
    }

    // --- Caminhos permitidos ----------------------------------------------

    @Test
    @DisplayName("professor cria questão: origem e status do corpo não chegam ao serviço")
    void professorCriaQuestaoSemDefinirModeracao() throws Exception {
        given(disciplinaRepository.findById(DISC_A)).willReturn(Optional.of(disciplina()));
        given(questaoService.salvar(any())).willAnswer(chamada -> chamada.getArgument(0));

        mockMvc.perform(post("/questoes")
                        .with(comoProfessor(PROFESSOR_ID))
                        .contentType(JSON)
                        .content("""
                                {"enunciado":"Quanto é 2 + 2?","tipo":"ALTERNATIVAS","disciplinaId":10,
                                 "origem":"IA","status":"APROVADA"}
                                """))
                .andExpect(status().isOk());

        ArgumentCaptor<Questao> enviada = ArgumentCaptor.forClass(Questao.class);
        verify(questaoService).salvar(enviada.capture());

        assertThat(enviada.getValue().getDisciplina().getId()).isEqualTo(DISC_A);
        assertThat(enviada.getValue().getOrigem()).isNull();
        assertThat(enviada.getValue().getStatus()).isNull();
    }

    @Test
    @DisplayName("professor edita questão: rota liberada, corpo sem origem/status")
    void professorEditaQuestao() throws Exception {
        given(disciplinaRepository.findById(DISC_A)).willReturn(Optional.of(disciplina()));
        given(questaoService.atualizar(any(), any())).willAnswer(chamada -> chamada.getArgument(1));

        mockMvc.perform(put("/questoes/{id}", QUESTAO_ID)
                        .with(comoProfessor(PROFESSOR_ID))
                        .contentType(JSON)
                        .content("""
                                {"enunciado":"Enunciado revisado","tipo":"ALTERNATIVAS","disciplinaId":10,
                                 "origem":"PROFESSOR"}
                                """))
                .andExpect(status().isOk());

        ArgumentCaptor<Questao> enviada = ArgumentCaptor.forClass(Questao.class);
        verify(questaoService).atualizar(any(), enviada.capture());

        assertThat(enviada.getValue().getOrigem()).isNull();
    }

    @Test
    @DisplayName("administrador cria questão sem disciplina (regra de disciplina é do serviço)")
    void administradorCriaQuestaoSemDisciplina() throws Exception {
        given(questaoService.salvar(any())).willAnswer(chamada -> chamada.getArgument(0));

        mockMvc.perform(post("/questoes")
                        .with(comoAdministrador())
                        .contentType(JSON)
                        .content("""
                                {"enunciado":"Questão sem disciplina","tipo":"VERDADEIRO_FALSO"}
                                """))
                .andExpect(status().isOk());

        ArgumentCaptor<Questao> enviada = ArgumentCaptor.forClass(Questao.class);
        verify(questaoService).salvar(enviada.capture());

        assertThat(enviada.getValue().getDisciplina()).isNull();
    }

    private static Disciplina disciplina() {
        Disciplina disciplina = new Disciplina();
        disciplina.setId(DISC_A);
        disciplina.setTitulo("Matemática");
        return disciplina;
    }
}
