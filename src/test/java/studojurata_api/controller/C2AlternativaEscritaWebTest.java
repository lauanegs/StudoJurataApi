package studojurata_api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
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

import studojurata_api.mapper.AlternativaMapper;
import studojurata_api.model.Alternativa;
import studojurata_api.model.Questao;
import studojurata_api.repository.QuestaoRepository;
import studojurata_api.service.AlternativaService;
import studojurata_api.support.AuthorizationTestSupport;
import studojurata_api.support.AutorizacaoComEscopoTestBase;

/**
 * C2.7d (passo 2) — a escrita de alternativa vista pela rota, com o
 * {@code SecurityConfig} real: aluno e anônimo são barrados antes do controller,
 * e a questão vem do banco pelo id do corpo, nunca de um objeto montado pelo
 * cliente.
 *
 * <p>A regra de escopo (professor só nas disciplinas que leciona, inclusive para
 * a questão de destino) é do {@code AlternativaService} e está coberta em
 * {@code C2AlternativaEscritaEscopoTest}.
 */
@WebMvcTest(controllers = AlternativaController.class)
@Import(AlternativaMapper.class)
class C2AlternativaEscritaWebTest extends AutorizacaoComEscopoTestBase {

    private static final long ALUNO_ID = 7L;
    private static final long PROFESSOR_ID = 42L;
    private static final long QUESTAO_ID = 100L;

    @MockBean private AlternativaService alternativaService;
    @MockBean private QuestaoRepository questaoRepository;

    @AfterEach
    void limparContexto() {
        AuthorizationTestSupport.limparContexto();
    }

    @Test
    @DisplayName("sem autenticação não cria nem edita alternativa")
    void semAutenticacaoNaoEscreveAlternativa() throws Exception {
        mockMvc.perform(post("/alternativas").contentType(JSON).content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/alternativas/{id}", 1L).contentType(JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("aluno não cria nem edita alternativa (o gabarito não passa por ele)")
    void alunoNaoEscreveAlternativa() throws Exception {
        mockMvc.perform(post("/alternativas").contentType(JSON).content("{}").with(comoAluno(ALUNO_ID)))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/alternativas/{id}", 1L).contentType(JSON).content("{}").with(comoAluno(ALUNO_ID)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("professor cria alternativa: a questão é a do banco, resolvida pelo id do corpo")
    void professorCriaAlternativaComQuestaoDoBanco() throws Exception {
        Questao questaoDoBanco = questao(QUESTAO_ID);
        given(questaoRepository.findById(QUESTAO_ID)).willReturn(Optional.of(questaoDoBanco));
        given(alternativaService.salvar(any())).willAnswer(chamada -> chamada.getArgument(0));

        mockMvc.perform(post("/alternativas")
                        .with(comoProfessor(PROFESSOR_ID))
                        .contentType(JSON)
                        .content("""
                                {"questaoId":100,"texto":"4","correta":true,"ordem":1}
                                """))
                .andExpect(status().isOk());

        ArgumentCaptor<Alternativa> enviada = ArgumentCaptor.forClass(Alternativa.class);
        verify(alternativaService).salvar(enviada.capture());
        verify(questaoRepository).findById(QUESTAO_ID);

        assertThat(enviada.getValue().getQuestao()).isSameAs(questaoDoBanco);
        assertThat(enviada.getValue().getTexto()).isEqualTo("4");
    }

    @Test
    @DisplayName("professor edita alternativa: rota liberada e questão resolvida pelo corpo")
    void professorEditaAlternativa() throws Exception {
        Questao destino = questao(QUESTAO_ID);
        given(questaoRepository.findById(QUESTAO_ID)).willReturn(Optional.of(destino));
        given(alternativaService.atualizar(any(), any())).willAnswer(chamada -> chamada.getArgument(1));

        mockMvc.perform(put("/alternativas/{id}", 1L)
                        .with(comoProfessor(PROFESSOR_ID))
                        .contentType(JSON)
                        .content("""
                                {"questaoId":100,"texto":"Quatro","correta":false,"ordem":2}
                                """))
                .andExpect(status().isOk());

        ArgumentCaptor<Alternativa> enviada = ArgumentCaptor.forClass(Alternativa.class);
        verify(alternativaService).atualizar(any(), enviada.capture());

        assertThat(enviada.getValue().getQuestao()).isSameAs(destino);
    }

    @Test
    @DisplayName("questão inexistente responde 404 antes de chegar ao serviço")
    void questaoInexistenteResponde404() throws Exception {
        given(questaoRepository.findById(QUESTAO_ID)).willReturn(Optional.empty());

        mockMvc.perform(post("/alternativas")
                        .with(comoProfessor(PROFESSOR_ID))
                        .contentType(JSON)
                        .content("""
                                {"questaoId":100,"texto":"4","correta":true}
                                """))
                .andExpect(status().isNotFound());

        verify(alternativaService, never()).salvar(any());
    }

    private static Questao questao(long id) {
        Questao questao = new Questao();
        questao.setId(id);
        return questao;
    }
}
