package studojurata_api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import studojurata_api.dto.FinalizarSimuladoRequest;
import studojurata_api.dto.SimuladoAlunoResponseDTO;
import studojurata_api.mapper.SimuladoAlunoMapper;
import studojurata_api.service.SimuladoAlunoService;
import studojurata_api.service.SimuladoService;
import studojurata_api.support.AuthorizationTestSupport;
import studojurata_api.support.AutorizacaoComEscopoTestBase;

/**
 * C1 — tentativas de simulado indexadas por aluno, com os guardas reais.
 *
 * <p>O {@code POST /{id}/finalizar} aparece aqui apenas para provar a ligação
 * (o fluxo legítimo do dono). O bloqueio de escrita é testado onde ele mora:
 * {@code SimuladoAlunoFinalizacaoTest} (service, com verificação de que nada é
 * persistido).
 */
@WebMvcTest(controllers = SimuladoAlunoController.class)
class C1SimuladoAlunoEscopoTest extends AutorizacaoComEscopoTestBase {

    private static final long ALUNO_ID = 7L;
    private static final long OUTRO_ALUNO_ID = 8L;
    private static final long PROFESSOR_ID = 42L;
    private static final long TURMA_ID = 10L;
    private static final long OUTRA_TURMA_ID = 99L;
    private static final long TENTATIVA_ID = 500L;
    private static final long SIMULADO_ID = 300L;

    @MockBean
    private SimuladoAlunoService simuladoAlunoService;

    @MockBean
    private SimuladoAlunoMapper simuladoAlunoMapper;

    @MockBean
    private SimuladoService simuladoService;

    @AfterEach
    void limparContexto() {
        AuthorizationTestSupport.limparContexto();
    }

    // --- GET /simulado-aluno/{id} ------------------------------------------

    @Test
    @DisplayName("aluno acessa a própria tentativa")
    void alunoAcessaPropriaTentativa() throws Exception {
        given(simuladoAlunoService.buscar(TENTATIVA_ID)).willReturn(tentativa(TENTATIVA_ID, ALUNO_ID));

        mockMvc.perform(get("/simulado-aluno/{id}", TENTATIVA_ID).with(comoAluno(ALUNO_ID)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("aluno acessando tentativa de outro aluno recebe 403")
    void alunoNaoAcessaTentativaDeOutro() throws Exception {
        given(simuladoAlunoService.buscar(TENTATIVA_ID)).willReturn(tentativa(TENTATIVA_ID, OUTRO_ALUNO_ID));

        mockMvc.perform(get("/simulado-aluno/{id}", TENTATIVA_ID).with(comoAluno(ALUNO_ID)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("professor acessa tentativa de aluno das suas turmas")
    void professorAcessaTentativaDeAlunoDoEscopo() throws Exception {
        given(simuladoAlunoService.buscar(TENTATIVA_ID)).willReturn(tentativa(TENTATIVA_ID, ALUNO_ID));
        professorLecionaParaAluno(PROFESSOR_ID, ALUNO_ID, TURMA_ID);

        mockMvc.perform(get("/simulado-aluno/{id}", TENTATIVA_ID).with(comoProfessor(PROFESSOR_ID)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("professor acessando tentativa fora do escopo recebe 403")
    void professorNaoAcessaTentativaForaDoEscopo() throws Exception {
        given(simuladoAlunoService.buscar(TENTATIVA_ID)).willReturn(tentativa(TENTATIVA_ID, OUTRO_ALUNO_ID));
        professorEhTitular(PROFESSOR_ID, TURMA_ID);

        mockMvc.perform(get("/simulado-aluno/{id}", TENTATIVA_ID).with(comoProfessor(PROFESSOR_ID)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("administrador acessa tentativa de qualquer aluno")
    void administradorAcessaTentativaDeQualquerAluno() throws Exception {
        given(simuladoAlunoService.buscar(TENTATIVA_ID)).willReturn(tentativa(TENTATIVA_ID, OUTRO_ALUNO_ID));

        mockMvc.perform(get("/simulado-aluno/{id}", TENTATIVA_ID).with(comoAdministrador()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("sem autenticação a tentativa é negada")
    void semAutenticacaoNaoAcessaTentativa() throws Exception {
        mockMvc.perform(get("/simulado-aluno/{id}", TENTATIVA_ID)).andExpect(status().isForbidden());
    }

    // --- GET /simulado-aluno/aluno/{alunoId} -------------------------------

    @Test
    @DisplayName("aluno lista as próprias tentativas")
    void alunoListaAsPropriasTentativas() throws Exception {
        given(simuladoAlunoService.listarPorAluno(ALUNO_ID)).willReturn(List.of());

        mockMvc.perform(get("/simulado-aluno/aluno/{alunoId}", ALUNO_ID).with(comoAluno(ALUNO_ID)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("aluno não lista tentativas de outro aluno")
    void alunoNaoListaTentativasDeOutro() throws Exception {
        mockMvc.perform(get("/simulado-aluno/aluno/{alunoId}", OUTRO_ALUNO_ID).with(comoAluno(ALUNO_ID)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("professor lista tentativas de aluno das suas turmas (matrícula encerrada também conta)")
    void professorListaTentativasDeAlunoDoEscopo() throws Exception {
        given(simuladoAlunoService.listarPorAluno(ALUNO_ID)).willReturn(List.of());
        professorLecionaParaAluno(PROFESSOR_ID, ALUNO_ID, TURMA_ID);

        mockMvc.perform(get("/simulado-aluno/aluno/{alunoId}", ALUNO_ID).with(comoProfessor(PROFESSOR_ID)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("professor não lista tentativas de aluno fora do escopo")
    void professorNaoListaTentativasForaDoEscopo() throws Exception {
        professorEhTitular(PROFESSOR_ID, OUTRA_TURMA_ID);

        mockMvc.perform(get("/simulado-aluno/aluno/{alunoId}", ALUNO_ID).with(comoProfessor(PROFESSOR_ID)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("administrador lista tentativas de qualquer aluno")
    void administradorListaTentativasDeQualquerAluno() throws Exception {
        given(simuladoAlunoService.listarPorAluno(OUTRO_ALUNO_ID)).willReturn(List.of());

        mockMvc.perform(get("/simulado-aluno/aluno/{alunoId}", OUTRO_ALUNO_ID).with(comoAdministrador()))
                .andExpect(status().isOk());
    }

    // --- GET /simulado-aluno/simulado/{simuladoId} -------------------------

    @Test
    @DisplayName("professor lista tentativas de simulado da sua turma")
    void professorListaTentativasDeSimuladoDaSuaTurma() throws Exception {
        given(simuladoService.buscar(SIMULADO_ID)).willReturn(simulado(SIMULADO_ID, TURMA_ID));
        given(simuladoAlunoService.listarPorSimulado(SIMULADO_ID)).willReturn(List.of());
        professorEhTitular(PROFESSOR_ID, TURMA_ID);

        mockMvc.perform(get("/simulado-aluno/simulado/{simuladoId}", SIMULADO_ID).with(comoProfessor(PROFESSOR_ID)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("professor não lista tentativas de simulado de turma alheia")
    void professorNaoListaTentativasDeSimuladoDeTurmaAlheia() throws Exception {
        given(simuladoService.buscar(SIMULADO_ID)).willReturn(simulado(SIMULADO_ID, OUTRA_TURMA_ID));
        professorEhTitular(PROFESSOR_ID, TURMA_ID);

        mockMvc.perform(get("/simulado-aluno/simulado/{simuladoId}", SIMULADO_ID).with(comoProfessor(PROFESSOR_ID)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("aluno não lista tentativas de simulado (não há uso legítimo)")
    void alunoNaoListaTentativasDeSimulado() throws Exception {
        given(simuladoService.buscar(SIMULADO_ID)).willReturn(simulado(SIMULADO_ID, TURMA_ID));

        mockMvc.perform(get("/simulado-aluno/simulado/{simuladoId}", SIMULADO_ID).with(comoAluno(ALUNO_ID)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("EXCEÇÃO DOCUMENTADA: simulado órfão mantém o comportamento atual para professor fora de escopo")
    void simuladoOrfaoMantemComportamentoAtualParaProfessor() throws Exception {
        given(simuladoService.buscar(SIMULADO_ID)).willReturn(simulado(SIMULADO_ID, null));
        given(simuladoAlunoService.listarPorSimulado(SIMULADO_ID)).willReturn(List.of());
        professorEhTitular(PROFESSOR_ID, OUTRA_TURMA_ID);

        mockMvc.perform(get("/simulado-aluno/simulado/{simuladoId}", SIMULADO_ID).with(comoProfessor(PROFESSOR_ID)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("EXCEÇÃO DOCUMENTADA: simulado órfão mantém o comportamento atual para aluno")
    void simuladoOrfaoMantemComportamentoAtualParaAluno() throws Exception {
        given(simuladoService.buscar(SIMULADO_ID)).willReturn(simulado(SIMULADO_ID, null));
        given(simuladoAlunoService.listarPorSimulado(SIMULADO_ID)).willReturn(List.of());

        mockMvc.perform(get("/simulado-aluno/simulado/{simuladoId}", SIMULADO_ID).with(comoAluno(ALUNO_ID)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("administrador lista tentativas de qualquer simulado")
    void administradorListaTentativasDeQualquerSimulado() throws Exception {
        given(simuladoService.buscar(SIMULADO_ID)).willReturn(simulado(SIMULADO_ID, OUTRA_TURMA_ID));
        given(simuladoAlunoService.listarPorSimulado(SIMULADO_ID)).willReturn(List.of());

        mockMvc.perform(get("/simulado-aluno/simulado/{simuladoId}", SIMULADO_ID).with(comoAdministrador()))
                .andExpect(status().isOk());
    }

    // --- POST /{id}/finalizar (ligação) ------------------------------------

    @Test
    @DisplayName("aluno dono finaliza a própria tentativa (regra de escrita vive no service)")
    void alunoDonoFinalizaPropriaTentativa() throws Exception {
        given(simuladoAlunoService.finalizar(eq(TENTATIVA_ID), any(FinalizarSimuladoRequest.class)))
                .willReturn(new SimuladoAlunoService.ResultadoFinalizacao(tentativa(TENTATIVA_ID, ALUNO_ID), 7));
        given(simuladoAlunoMapper.toResponseDTO(any())).willReturn(new SimuladoAlunoResponseDTO());

        mockMvc.perform(post("/simulado-aluno/{id}/finalizar", TENTATIVA_ID)
                        .contentType(JSON)
                        .content("{}")
                        .with(comoAluno(ALUNO_ID)))
                .andExpect(status().isOk());
    }
}
