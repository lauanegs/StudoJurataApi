package studojurata_api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import studojurata_api.mapper.SimuladoMapper;
import studojurata_api.model.Disciplina;
import studojurata_api.model.Simulado;
import studojurata_api.model.Turma;
import studojurata_api.repository.DisciplinaRepository;
import studojurata_api.repository.PlanoEnsinoRepository;
import studojurata_api.repository.TurmaRepository;
import studojurata_api.service.SimuladoService;
import studojurata_api.support.AuthorizationTestSupport;
import studojurata_api.support.AutorizacaoComEscopoTestBase;

/**
 * C2.7d (passo 5) — as rotas de escrita de simulado com o {@code SecurityConfig}
 * real: aluno e anônimo são barrados antes do controller em todas as cinco
 * operações, e turma/disciplina vêm do banco pelo id do corpo.
 *
 * <p>A regra de escopo do professor é do {@code SimuladoAccessGuard} e está
 * coberta em {@code C2SimuladoEscritaEscopoTest}.
 */
@WebMvcTest(controllers = SimuladoController.class)
@Import(SimuladoMapper.class)
class C2SimuladoEscritaWebTest extends AutorizacaoComEscopoTestBase {

    private static final long ALUNO_ID = 7L;
    private static final long PROFESSOR_ID = 42L;
    private static final long SIMULADO_ID = 100L;
    private static final long TURMA_ID = 30L;
    private static final long DISC_A = 10L;

    @MockBean private SimuladoService simuladoService;
    @MockBean private DisciplinaRepository disciplinaRepository;
    @MockBean private PlanoEnsinoRepository planoEnsinoRepository;
    @MockBean private TurmaRepository turmaRepository;

    @AfterEach
    void limparContexto() {
        AuthorizationTestSupport.limparContexto();
    }

    @Test
    @DisplayName("sem autenticação: 403 na criação, edição, lançamento, encerramento e disponibilidade")
    void semAutenticacaoNaoEscreveSimulado() throws Exception {
        mockMvc.perform(post("/simulados").contentType(JSON).content("{}")).andExpect(status().isForbidden());
        mockMvc.perform(put("/simulados/{id}", SIMULADO_ID).contentType(JSON).content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/simulados/{id}/lancar", SIMULADO_ID)).andExpect(status().isForbidden());
        mockMvc.perform(post("/simulados/{id}/encerrar", SIMULADO_ID)).andExpect(status().isForbidden());
        mockMvc.perform(patch("/simulados/{id}/disponibilidade", SIMULADO_ID).contentType(JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("aluno: 403 na criação, edição, lançamento, encerramento e disponibilidade")
    void alunoNaoEscreveSimulado() throws Exception {
        mockMvc.perform(post("/simulados").with(comoAluno(ALUNO_ID)).contentType(JSON).content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/simulados/{id}", SIMULADO_ID).with(comoAluno(ALUNO_ID))
                        .contentType(JSON).content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/simulados/{id}/lancar", SIMULADO_ID).with(comoAluno(ALUNO_ID)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/simulados/{id}/encerrar", SIMULADO_ID).with(comoAluno(ALUNO_ID)))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/simulados/{id}/disponibilidade", SIMULADO_ID).with(comoAluno(ALUNO_ID))
                        .contentType(JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("professor cria simulado: turma e disciplina vêm do banco, pelo id do corpo")
    void professorCriaSimuladoComEntidadesDoBanco() throws Exception {
        Turma turmaDoBanco = turma(TURMA_ID);
        Disciplina disciplinaDoBanco = disciplina(DISC_A);
        given(turmaRepository.findById(TURMA_ID)).willReturn(Optional.of(turmaDoBanco));
        given(disciplinaRepository.findById(DISC_A)).willReturn(Optional.of(disciplinaDoBanco));
        given(simuladoService.salvar(any())).willAnswer(chamada -> chamada.getArgument(0));

        mockMvc.perform(post("/simulados")
                        .with(comoProfessor(PROFESSOR_ID))
                        .contentType(JSON)
                        .content("""
                                {"titulo":"Simulado novo","disciplinaId":10,"turmaId":30,"tipoDestinacao":"TODOS"}
                                """))
                .andExpect(status().isOk());

        ArgumentCaptor<Simulado> enviado = ArgumentCaptor.forClass(Simulado.class);
        verify(simuladoService).salvar(enviado.capture());

        assertThat(enviado.getValue().getTurma()).isSameAs(turmaDoBanco);
        assertThat(enviado.getValue().getDisciplina()).isSameAs(disciplinaDoBanco);
    }

    @Test
    @DisplayName("professor lança, encerra e estende a disponibilidade pelas rotas próprias")
    void professorOperaSimuladoProprio() throws Exception {
        given(simuladoService.lancar(any(), any())).willReturn(new Simulado());
        given(simuladoService.encerrar(any())).willReturn(new Simulado());
        LocalDateTime novaDataFim = LocalDateTime.of(2027, 1, 15, 23, 59);
        given(simuladoService.estenderDisponibilidade(any(), any())).willReturn(new Simulado());

        mockMvc.perform(post("/simulados/{id}/lancar", SIMULADO_ID).with(comoProfessor(PROFESSOR_ID)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/simulados/{id}/encerrar", SIMULADO_ID).with(comoProfessor(PROFESSOR_ID)))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/simulados/{id}/disponibilidade", SIMULADO_ID).with(comoProfessor(PROFESSOR_ID))
                        .contentType(JSON).content("{\"dataFim\":\"2027-01-15T23:59:00\"}"))
                .andExpect(status().isOk());

        verify(simuladoService).lancar(any(), any());
        verify(simuladoService).encerrar(SIMULADO_ID);
        verify(simuladoService).estenderDisponibilidade(SIMULADO_ID, novaDataFim);
    }

    @Test
    @DisplayName("administrador também passa pelas rotas de escrita")
    void administradorOperaSimulado() throws Exception {
        given(simuladoService.encerrar(any())).willReturn(new Simulado());

        mockMvc.perform(post("/simulados/{id}/encerrar", SIMULADO_ID).with(comoAdministrador()))
                .andExpect(status().isOk());

        verify(simuladoService).encerrar(SIMULADO_ID);
    }

    private static Turma turma(long id) {
        Turma turma = new Turma();
        turma.setId(id);
        return turma;
    }

    private static Disciplina disciplina(long id) {
        Disciplina disciplina = new Disciplina();
        disciplina.setId(id);
        return disciplina;
    }
}
