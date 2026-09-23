package studojurata_api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import studojurata_api.model.ResponsavelAluno;
import studojurata_api.service.ResponsavelAlunoService;
import studojurata_api.support.AuthorizationTestSupport;
import studojurata_api.support.AutorizacaoComEscopoTestBase;

/**
 * Passo 11 — escopo das leituras de responsável × aluno.
 *
 * <p>O vínculo expõe dados pessoais do responsável, então a rota por aluno segue
 * o mesmo guard das demais rotas indexadas por aluno (próprio aluno, professor
 * que leciona para ele, administrador) e a rota por responsável é do
 * administrador — os únicos consumidores no frontend são telas administrativas.
 */
@WebMvcTest(controllers = ResponsavelAlunoController.class)
class C2ResponsavelAlunoEscopoTest extends AutorizacaoComEscopoTestBase {

    private static final long ALUNO_ID = 7L;
    private static final long OUTRO_ALUNO_ID = 8L;
    private static final long PROFESSOR_ID = 42L;
    private static final long TURMA_ID = 10L;
    private static final long OUTRA_TURMA_ID = 99L;
    private static final long RESPONSAVEL_ID = 300L;

    @MockBean private ResponsavelAlunoService responsavelAlunoService;

    @AfterEach
    void limparContexto() {
        AuthorizationTestSupport.limparContexto();
    }

    @Test
    @DisplayName("administrador acessa os responsáveis de qualquer aluno")
    void administradorAcessaResponsaveisDeQualquerAluno() throws Exception {
        dadoVinculos();

        mockMvc.perform(get("/responsavel-aluno/por-aluno/{alunoId}", OUTRO_ALUNO_ID).with(comoAdministrador()))
                .andExpect(status().isOk());

        verify(responsavelAlunoService).porAluno(OUTRO_ALUNO_ID);
    }

    @Test
    @DisplayName("professor acessa responsáveis de aluno das suas turmas")
    void professorAcessaResponsaveisDeAlunoDoEscopo() throws Exception {
        dadoVinculos();
        professorLecionaParaAluno(PROFESSOR_ID, ALUNO_ID, TURMA_ID);

        mockMvc.perform(get("/responsavel-aluno/por-aluno/{alunoId}", ALUNO_ID).with(comoProfessor(PROFESSOR_ID)))
                .andExpect(status().isOk());

        verify(responsavelAlunoService).porAluno(ALUNO_ID);
    }

    @Test
    @DisplayName("professor não acessa responsáveis de aluno fora do escopo, e nada é consultado")
    void professorNaoAcessaResponsaveisForaDoEscopo() throws Exception {
        professorEhTitular(PROFESSOR_ID, OUTRA_TURMA_ID);

        mockMvc.perform(get("/responsavel-aluno/por-aluno/{alunoId}", ALUNO_ID).with(comoProfessor(PROFESSOR_ID)))
                .andExpect(status().isForbidden());

        verify(responsavelAlunoService, never()).porAluno(any());
    }

    @Test
    @DisplayName("aluno acessa os próprios responsáveis")
    void alunoAcessaPropriosResponsaveis() throws Exception {
        dadoVinculos();

        mockMvc.perform(get("/responsavel-aluno/por-aluno/{alunoId}", ALUNO_ID).with(comoAluno(ALUNO_ID)))
                .andExpect(status().isOk());

        verify(responsavelAlunoService).porAluno(ALUNO_ID);
    }

    @Test
    @DisplayName("aluno não acessa responsáveis de outro aluno")
    void alunoNaoAcessaResponsaveisDeOutro() throws Exception {
        mockMvc.perform(get("/responsavel-aluno/por-aluno/{alunoId}", OUTRO_ALUNO_ID).with(comoAluno(ALUNO_ID)))
                .andExpect(status().isForbidden());

        verify(responsavelAlunoService, never()).porAluno(any());
    }

    @Test
    @DisplayName("sem autenticação não lista responsáveis e não consulta o serviço")
    void semAutenticacaoNaoListaResponsaveis() throws Exception {
        mockMvc.perform(get("/responsavel-aluno/por-aluno/{alunoId}", ALUNO_ID))
                .andExpect(status().isForbidden());

        verify(responsavelAlunoService, never()).porAluno(any());
    }

    @Test
    @DisplayName("por responsável é rota do administrador: professor e aluno recebem 403")
    void porResponsavelEhDoAdministrador() throws Exception {
        given(responsavelAlunoService.porResponsavel(RESPONSAVEL_ID)).willReturn(List.of(new ResponsavelAluno()));

        mockMvc.perform(get("/responsavel-aluno/por-responsavel/{id}", RESPONSAVEL_ID).with(comoAdministrador()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/responsavel-aluno/por-responsavel/{id}", RESPONSAVEL_ID).with(comoProfessor(PROFESSOR_ID)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/responsavel-aluno/por-responsavel/{id}", RESPONSAVEL_ID).with(comoAluno(ALUNO_ID)))
                .andExpect(status().isForbidden());

        verify(responsavelAlunoService).porResponsavel(RESPONSAVEL_ID);
    }

    private void dadoVinculos() {
        given(responsavelAlunoService.porAluno(any())).willReturn(List.of(new ResponsavelAluno()));
    }
}
