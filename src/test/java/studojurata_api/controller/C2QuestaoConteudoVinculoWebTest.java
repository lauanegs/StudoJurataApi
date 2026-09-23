package studojurata_api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import studojurata_api.mapper.QuestaoMapper;
import studojurata_api.model.QuestaoConteudo;
import studojurata_api.service.QuestaoConteudoService;
import studojurata_api.service.QuestaoService;
import studojurata_api.support.AuthorizationTestSupport;
import studojurata_api.support.AutorizacaoComEscopoTestBase;

/**
 * C2.7d (passo 3) — as rotas do vínculo questão × conteúdo com o
 * {@code SecurityConfig} real.
 *
 * <p>Além do 403 de aluno e anônimo, o teste documenta a regra de rota que é
 * fácil quebrar ao mexer no arquivo: o DELETE de
 * {@code /questoes/{id}/conteudos/{conteudoPlanoId}} é de professor e
 * administrador, e precisa continuar vindo <b>antes</b> do
 * {@code DELETE /questoes/**}, que é só do administrador.
 */
@WebMvcTest(controllers = QuestaoController.class)
class C2QuestaoConteudoVinculoWebTest extends AutorizacaoComEscopoTestBase {

    private static final long ALUNO_ID = 7L;
    private static final long PROFESSOR_ID = 42L;
    private static final long QUESTAO_ID = 100L;
    private static final long CONTEUDO_ID = 300L;

    @MockBean private QuestaoService questaoService;
    @MockBean private QuestaoMapper questaoMapper;
    @MockBean private QuestaoConteudoService questaoConteudoService;

    @AfterEach
    void limparContexto() {
        AuthorizationTestSupport.limparContexto();
    }

    @Test
    @DisplayName("sem autenticação não vincula nem desvincula")
    void semAutenticacaoNaoAlteraVinculo() throws Exception {
        mockMvc.perform(post("/questoes/{id}/conteudos/{conteudoId}", QUESTAO_ID, CONTEUDO_ID))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/questoes/{id}/conteudos/{conteudoId}", QUESTAO_ID, CONTEUDO_ID))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("aluno não vincula nem desvincula")
    void alunoNaoAlteraVinculo() throws Exception {
        mockMvc.perform(post("/questoes/{id}/conteudos/{conteudoId}", QUESTAO_ID, CONTEUDO_ID)
                        .with(comoAluno(ALUNO_ID)))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/questoes/{id}/conteudos/{conteudoId}", QUESTAO_ID, CONTEUDO_ID)
                        .with(comoAluno(ALUNO_ID)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("professor vincula e desvincula conteúdo (rota não cai no DELETE só do administrador)")
    void professorAlteraVinculo() throws Exception {
        given(questaoConteudoService.vincular(QUESTAO_ID, CONTEUDO_ID)).willReturn(new QuestaoConteudo());

        mockMvc.perform(post("/questoes/{id}/conteudos/{conteudoId}", QUESTAO_ID, CONTEUDO_ID)
                        .with(comoProfessor(PROFESSOR_ID)))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/questoes/{id}/conteudos/{conteudoId}", QUESTAO_ID, CONTEUDO_ID)
                        .with(comoProfessor(PROFESSOR_ID)))
                .andExpect(status().isOk());

        verify(questaoConteudoService).vincular(QUESTAO_ID, CONTEUDO_ID);
        verify(questaoConteudoService).desvincular(QUESTAO_ID, CONTEUDO_ID);
    }

    @Test
    @DisplayName("administrador vincula e desvincula conteúdo")
    void administradorAlteraVinculo() throws Exception {
        given(questaoConteudoService.vincular(any(), any())).willReturn(new QuestaoConteudo());

        mockMvc.perform(post("/questoes/{id}/conteudos/{conteudoId}", QUESTAO_ID, CONTEUDO_ID)
                        .with(comoAdministrador()))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/questoes/{id}/conteudos/{conteudoId}", QUESTAO_ID, CONTEUDO_ID)
                        .with(comoAdministrador()))
                .andExpect(status().isOk());

        verify(questaoConteudoService).desvincular(QUESTAO_ID, CONTEUDO_ID);
    }
}
