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

import studojurata_api.mapper.SimuladoAlunoMapper;
import studojurata_api.service.SimuladoAlunoService;
import studojurata_api.service.SimuladoService;
import studojurata_api.support.AuthorizationTestSupport;
import studojurata_api.support.AutorizacaoComEscopoTestBase;

/**
 * Tentativas de um simulado ({@code GET /simulado-aluno/simulado/{id}}).
 *
 * <p>Simulado <b>sem turma</b> não tem escopo verificável: só o administrador
 * consulta as tentativas dele — a mesma cautela da escrita. Simulado com turma
 * segue o escopo da turma, o que já mantém o aluno fora (ele consulta as
 * próprias tentativas pelas rotas do fluxo discente).
 */
@WebMvcTest(controllers = SimuladoAlunoController.class)
class C2SimuladoAlunoOrfaoEscopoTest extends AutorizacaoComEscopoTestBase {

    private static final long ALUNO_ID = 7L;
    private static final long PROFESSOR_ID = 42L;
    private static final long TURMA_ID = 10L;
    private static final long OUTRA_TURMA_ID = 99L;
    private static final long SIMULADO_ID = 300L;
    private static final long ORFAO_ID = 900L;

    @MockBean private SimuladoAlunoService simuladoAlunoService;
    @MockBean private SimuladoAlunoMapper simuladoAlunoMapper;
    @MockBean private SimuladoService simuladoService;

    @AfterEach
    void limparContexto() {
        AuthorizationTestSupport.limparContexto();
    }

    @Test
    @DisplayName("simulado órfão: administrador lista as tentativas")
    void administradorListaTentativasDeOrfao() throws Exception {
        dadoSimuladoOrfao();

        mockMvc.perform(get("/simulado-aluno/simulado/{id}", ORFAO_ID).with(comoAdministrador()))
                .andExpect(status().isOk());

        verify(simuladoAlunoService).listarPorSimulado(ORFAO_ID);
    }

    @Test
    @DisplayName("simulado órfão: professor é bloqueado e nada é consultado")
    void professorNaoListaTentativasDeOrfao() throws Exception {
        dadoSimuladoOrfao();

        mockMvc.perform(get("/simulado-aluno/simulado/{id}", ORFAO_ID).with(comoProfessor(PROFESSOR_ID)))
                .andExpect(status().isForbidden());

        verify(simuladoAlunoService, never()).listarPorSimulado(any());
    }

    @Test
    @DisplayName("simulado órfão: aluno é bloqueado, mesmo sendo dono de uma tentativa")
    void alunoNaoListaTentativasDeOrfao() throws Exception {
        dadoSimuladoOrfao();

        mockMvc.perform(get("/simulado-aluno/simulado/{id}", ORFAO_ID).with(comoAluno(ALUNO_ID)))
                .andExpect(status().isForbidden());

        verify(simuladoAlunoService, never()).listarPorSimulado(any());
    }

    @Test
    @DisplayName("simulado sem turma: sem autenticação não consulta nada")
    void semAutenticacaoNaoListaTentativasDeOrfao() throws Exception {
        dadoSimuladoOrfao();

        mockMvc.perform(get("/simulado-aluno/simulado/{id}", ORFAO_ID))
                .andExpect(status().isForbidden());

        verify(simuladoAlunoService, never()).listarPorSimulado(any());
    }

    @Test
    @DisplayName("simulado com turma: professor dono da turma lista as tentativas")
    void professorDonoDaTurmaListaTentativas() throws Exception {
        given(simuladoService.buscar(SIMULADO_ID)).willReturn(simulado(SIMULADO_ID, TURMA_ID));
        given(simuladoAlunoService.listarPorSimulado(SIMULADO_ID)).willReturn(List.of());
        professorEhTitular(PROFESSOR_ID, TURMA_ID);

        mockMvc.perform(get("/simulado-aluno/simulado/{id}", SIMULADO_ID).with(comoProfessor(PROFESSOR_ID)))
                .andExpect(status().isOk());

        verify(simuladoAlunoService).listarPorSimulado(SIMULADO_ID);
    }

    @Test
    @DisplayName("simulado com turma: professor de outra turma é bloqueado")
    void professorDeOutraTurmaNaoListaTentativas() throws Exception {
        given(simuladoService.buscar(SIMULADO_ID)).willReturn(simulado(SIMULADO_ID, TURMA_ID));
        professorEhTitular(PROFESSOR_ID, OUTRA_TURMA_ID);

        mockMvc.perform(get("/simulado-aluno/simulado/{id}", SIMULADO_ID).with(comoProfessor(PROFESSOR_ID)))
                .andExpect(status().isForbidden());

        verify(simuladoAlunoService, never()).listarPorSimulado(any());
    }

    @Test
    @DisplayName("simulado com turma: aluno não usa a rota administrativa")
    void alunoNaoListaTentativasDeSimuladoComTurma() throws Exception {
        given(simuladoService.buscar(SIMULADO_ID)).willReturn(simulado(SIMULADO_ID, TURMA_ID));

        mockMvc.perform(get("/simulado-aluno/simulado/{id}", SIMULADO_ID).with(comoAluno(ALUNO_ID)))
                .andExpect(status().isForbidden());

        verify(simuladoAlunoService, never()).listarPorSimulado(any());
    }

    private void dadoSimuladoOrfao() {
        given(simuladoService.buscar(ORFAO_ID)).willReturn(simulado(ORFAO_ID, null));
        given(simuladoAlunoService.listarPorSimulado(ORFAO_ID)).willReturn(List.of());
    }
}
