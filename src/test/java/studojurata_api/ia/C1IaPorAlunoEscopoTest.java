package studojurata_api.ia;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import studojurata_api.ia.controller.RecomendacaoController;
import studojurata_api.ia.controller.RevisaoConteudoController;
import studojurata_api.ia.mapper.RevisaoConteudoMapper;
import studojurata_api.ia.service.RecomendacaoService;
import studojurata_api.ia.service.RevisaoConteudoService;
import studojurata_api.support.AuthorizationTestSupport;
import studojurata_api.support.AutorizacaoComEscopoTestBase;

/**
 * C1 — recomendações e revisão de conteúdo por aluno. A rota {@code /ia/**} já
 * exige PROFESSOR/ADMIN no {@code SecurityConfig}; aqui entra o escopo por
 * turma do professor.
 */
@WebMvcTest(controllers = { RecomendacaoController.class, RevisaoConteudoController.class })
class C1IaPorAlunoEscopoTest extends AutorizacaoComEscopoTestBase {

    private static final long ALUNO_ID = 7L;
    private static final long OUTRO_ALUNO_ID = 8L;
    private static final long PROFESSOR_ID = 42L;
    private static final long TURMA_ID = 10L;
    private static final long OUTRA_TURMA_ID = 99L;

    @MockBean private RecomendacaoService recomendacaoService;
    @MockBean private RevisaoConteudoService revisaoConteudoService;
    @MockBean private RevisaoConteudoMapper revisaoConteudoMapper;

    @AfterEach
    void limparContexto() {
        AuthorizationTestSupport.limparContexto();
    }

    @Test
    @DisplayName("professor acessa recomendações de aluno das suas turmas")
    void professorAcessaRecomendacoesDeAlunoDoEscopo() throws Exception {
        given(recomendacaoService.recomendarParaAluno(ALUNO_ID)).willReturn(List.of());
        professorLecionaParaAluno(PROFESSOR_ID, ALUNO_ID, TURMA_ID);

        mockMvc.perform(get("/ia/recomendacoes/aluno/{alunoId}", ALUNO_ID).with(comoProfessor(PROFESSOR_ID)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("professor não acessa recomendações de aluno fora do escopo")
    void professorNaoAcessaRecomendacoesForaDoEscopo() throws Exception {
        professorEhTitular(PROFESSOR_ID, OUTRA_TURMA_ID);

        mockMvc.perform(get("/ia/recomendacoes/aluno/{alunoId}", ALUNO_ID).with(comoProfessor(PROFESSOR_ID)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("professor acessa revisões de conteúdo de aluno das suas turmas")
    void professorAcessaRevisoesDeAlunoDoEscopo() throws Exception {
        given(revisaoConteudoService.listarPorAluno(ALUNO_ID)).willReturn(List.of());
        professorLecionaParaAluno(PROFESSOR_ID, ALUNO_ID, TURMA_ID);

        mockMvc.perform(get("/ia/revisao-conteudo/aluno/{alunoId}", ALUNO_ID).with(comoProfessor(PROFESSOR_ID)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("professor não acessa revisões de conteúdo de aluno fora do escopo")
    void professorNaoAcessaRevisoesForaDoEscopo() throws Exception {
        professorEhTitular(PROFESSOR_ID, OUTRA_TURMA_ID);

        mockMvc.perform(get("/ia/revisao-conteudo/aluno/{alunoId}", ALUNO_ID).with(comoProfessor(PROFESSOR_ID)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("administrador acessa recomendações e revisões de qualquer aluno")
    void administradorAcessaQualquerAluno() throws Exception {
        given(recomendacaoService.recomendarParaAluno(OUTRO_ALUNO_ID)).willReturn(List.of());
        given(revisaoConteudoService.listarPorAluno(OUTRO_ALUNO_ID)).willReturn(List.of());

        mockMvc.perform(get("/ia/recomendacoes/aluno/{alunoId}", OUTRO_ALUNO_ID).with(comoAdministrador()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/ia/revisao-conteudo/aluno/{alunoId}", OUTRO_ALUNO_ID).with(comoAdministrador()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("aluno não acessa as rotas de IA")
    void alunoNaoAcessaRotasDeIa() throws Exception {
        mockMvc.perform(get("/ia/recomendacoes/aluno/{alunoId}", ALUNO_ID).with(comoAluno(ALUNO_ID)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("sem autenticação as rotas de IA são negadas")
    void semAutenticacaoNaoAcessaRotasDeIa() throws Exception {
        mockMvc.perform(get("/ia/recomendacoes/aluno/{alunoId}", ALUNO_ID)).andExpect(status().isForbidden());
    }
}
