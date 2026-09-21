package studojurata_api.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import studojurata_api.controller.gamificacao.GamificacaoController;
import studojurata_api.model.gamificacao.PontuacaoAluno;
import studojurata_api.model.gamificacao.SkinAluno;
import studojurata_api.service.gamificacao.PontuacaoAlunoService;
import studojurata_api.service.gamificacao.SkinService;
import studojurata_api.support.AuthorizationTestSupport;
import studojurata_api.support.AutorizacaoComEscopoTestBase;

/**
 * C1 — gamificação: leitura (pontuação/skins) escopada ao aluno e escrita
 * (comprar/equipar) restrita ao próprio aluno.
 */
@WebMvcTest(controllers = GamificacaoController.class)
class C1GamificacaoEscopoTest extends AutorizacaoComEscopoTestBase {

    private static final long ALUNO_ID = 7L;
    private static final long OUTRO_ALUNO_ID = 8L;
    private static final long PROFESSOR_ID = 42L;
    private static final long TURMA_ID = 10L;
    private static final long OUTRA_TURMA_ID = 99L;
    private static final long SKIN_ID = 3L;

    @MockBean private PontuacaoAlunoService pontuacaoAlunoService;
    @MockBean private SkinService skinService;

    @AfterEach
    void limparContexto() {
        AuthorizationTestSupport.limparContexto();
    }

    // --- Leitura -----------------------------------------------------------

    @Test
    @DisplayName("aluno lê a própria pontuação")
    void alunoLePropriaPontuacao() throws Exception {
        given(pontuacaoAlunoService.buscarOuCriar(ALUNO_ID)).willReturn(new PontuacaoAluno());

        mockMvc.perform(get("/gamificacao/aluno/{alunoId}/pontuacao", ALUNO_ID).with(comoAluno(ALUNO_ID)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("aluno não lê pontuação de outro aluno")
    void alunoNaoLePontuacaoDeOutro() throws Exception {
        mockMvc.perform(get("/gamificacao/aluno/{alunoId}/pontuacao", OUTRO_ALUNO_ID).with(comoAluno(ALUNO_ID)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("professor lê pontuação de aluno das suas turmas")
    void professorLePontuacaoDeAlunoDoEscopo() throws Exception {
        given(pontuacaoAlunoService.buscarOuCriar(ALUNO_ID)).willReturn(new PontuacaoAluno());
        professorLecionaParaAluno(PROFESSOR_ID, ALUNO_ID, TURMA_ID);

        mockMvc.perform(get("/gamificacao/aluno/{alunoId}/pontuacao", ALUNO_ID).with(comoProfessor(PROFESSOR_ID)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("professor não lê pontuação de aluno fora do escopo")
    void professorNaoLePontuacaoForaDoEscopo() throws Exception {
        professorEhTitular(PROFESSOR_ID, OUTRA_TURMA_ID);

        mockMvc.perform(get("/gamificacao/aluno/{alunoId}/pontuacao", ALUNO_ID).with(comoProfessor(PROFESSOR_ID)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("administrador lê pontuação de qualquer aluno")
    void administradorLePontuacaoDeQualquerAluno() throws Exception {
        given(pontuacaoAlunoService.buscarOuCriar(OUTRO_ALUNO_ID)).willReturn(new PontuacaoAluno());

        mockMvc.perform(get("/gamificacao/aluno/{alunoId}/pontuacao", OUTRO_ALUNO_ID).with(comoAdministrador()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("aluno lê as próprias skins")
    void alunoLePropriasSkins() throws Exception {
        mockMvc.perform(get("/gamificacao/aluno/{alunoId}/skins", ALUNO_ID).with(comoAluno(ALUNO_ID)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("sem autenticação a pontuação é negada")
    void semAutenticacaoNaoLePontuacao() throws Exception {
        mockMvc.perform(get("/gamificacao/aluno/{alunoId}/pontuacao", ALUNO_ID))
                .andExpect(status().isForbidden());
    }

    // --- Escrita -----------------------------------------------------------

    @Test
    @DisplayName("aluno compra skin para si mesmo")
    void alunoCompraSkinParaSi() throws Exception {
        given(skinService.comprar(ALUNO_ID, SKIN_ID)).willReturn(new SkinAluno());

        mockMvc.perform(post("/gamificacao/aluno/{alunoId}/skins/{skinId}/comprar", ALUNO_ID, SKIN_ID)
                        .with(comoAluno(ALUNO_ID)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("aluno não compra skin para outro aluno")
    void alunoNaoCompraSkinParaOutro() throws Exception {
        mockMvc.perform(post("/gamificacao/aluno/{alunoId}/skins/{skinId}/comprar", OUTRO_ALUNO_ID, SKIN_ID)
                        .with(comoAluno(ALUNO_ID)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("professor não compra skin em nome de aluno, mesmo com escopo")
    void professorNaoCompraSkinParaAluno() throws Exception {
        professorLecionaParaAluno(PROFESSOR_ID, ALUNO_ID, TURMA_ID);

        mockMvc.perform(post("/gamificacao/aluno/{alunoId}/skins/{skinId}/comprar", ALUNO_ID, SKIN_ID)
                        .with(comoProfessor(PROFESSOR_ID)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("professor não equipa skin em nome de aluno")
    void professorNaoEquipaSkinParaAluno() throws Exception {
        professorLecionaParaAluno(PROFESSOR_ID, ALUNO_ID, TURMA_ID);

        mockMvc.perform(post("/gamificacao/aluno/{alunoId}/skins/{skinId}/equipar", ALUNO_ID, SKIN_ID)
                        .with(comoProfessor(PROFESSOR_ID)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("aluno equipa skin para si mesmo")
    void alunoEquipaSkinParaSi() throws Exception {
        given(skinService.equipar(ALUNO_ID, SKIN_ID)).willReturn(new SkinAluno());

        mockMvc.perform(post("/gamificacao/aluno/{alunoId}/skins/{skinId}/equipar", ALUNO_ID, SKIN_ID)
                        .with(comoAluno(ALUNO_ID)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("administrador compra skin em nome de aluno")
    void administradorCompraSkinParaAluno() throws Exception {
        given(skinService.comprar(OUTRO_ALUNO_ID, SKIN_ID)).willReturn(new SkinAluno());

        mockMvc.perform(post("/gamificacao/aluno/{alunoId}/skins/{skinId}/comprar", OUTRO_ALUNO_ID, SKIN_ID)
                        .with(comoAdministrador()))
                .andExpect(status().isOk());
    }
}
