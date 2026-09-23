package studojurata_api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import studojurata_api.mapper.SimuladoQuestaoMapper;
import studojurata_api.model.Questao;
import studojurata_api.model.Simulado;
import studojurata_api.model.SimuladoQuestao;
import studojurata_api.repository.QuestaoRepository;
import studojurata_api.repository.SimuladoRepository;
import studojurata_api.service.SimuladoQuestaoService;
import studojurata_api.support.AuthorizationTestSupport;
import studojurata_api.support.AutorizacaoComEscopoTestBase;

/**
 * C2.7d (passo 4) — a rota do vínculo simulado × questão com o
 * {@code SecurityConfig} real: aluno e anônimo são barrados antes do controller,
 * e simulado/questão são resolvidos no banco pelos ids do corpo (nada vem
 * montado do cliente).
 *
 * <p>A regra de escopo é do {@code SimuladoQuestaoService} e está coberta em
 * {@code C2SimuladoQuestaoVinculoTest}.
 */
@WebMvcTest(controllers = SimuladoQuestaoController.class)
@Import(SimuladoQuestaoMapper.class)
class C2SimuladoQuestaoVinculoWebTest extends AutorizacaoComEscopoTestBase {

    private static final long ALUNO_ID = 7L;
    private static final long PROFESSOR_ID = 42L;
    private static final long SIMULADO_ID = 100L;
    private static final long QUESTAO_ID = 200L;

    @MockBean private SimuladoQuestaoService simuladoQuestaoService;
    @MockBean private SimuladoRepository simuladoRepository;
    @MockBean private QuestaoRepository questaoRepository;

    @AfterEach
    void limparContexto() {
        AuthorizationTestSupport.limparContexto();
    }

    @Test
    @DisplayName("sem autenticação não vincula questão a simulado")
    void semAutenticacaoNaoVincula() throws Exception {
        mockMvc.perform(post("/simulado-questao")
                        .contentType(JSON)
                        .content("{\"simuladoId\":100,\"questaoId\":200}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("aluno não vincula questão a simulado")
    void alunoNaoVincula() throws Exception {
        mockMvc.perform(post("/simulado-questao")
                        .with(comoAluno(ALUNO_ID))
                        .contentType(JSON)
                        .content("{\"simuladoId\":100,\"questaoId\":200}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("professor vincula: simulado e questão vêm do banco, pelo id do corpo")
    void professorVinculaComEntidadesDoBanco() throws Exception {
        Simulado simuladoDoBanco = simulado(SIMULADO_ID);
        Questao questaoDoBanco = questao(QUESTAO_ID);
        given(simuladoRepository.findById(SIMULADO_ID)).willReturn(Optional.of(simuladoDoBanco));
        given(questaoRepository.findById(QUESTAO_ID)).willReturn(Optional.of(questaoDoBanco));
        given(simuladoQuestaoService.salvar(any())).willAnswer(chamada -> chamada.getArgument(0));

        mockMvc.perform(post("/simulado-questao")
                        .with(comoProfessor(PROFESSOR_ID))
                        .contentType(JSON)
                        .content("{\"simuladoId\":100,\"questaoId\":200,\"ordem\":1,\"pontuacao\":2.0}"))
                .andExpect(status().isOk());

        ArgumentCaptor<SimuladoQuestao> enviado = ArgumentCaptor.forClass(SimuladoQuestao.class);
        verify(simuladoQuestaoService).salvar(enviado.capture());

        assertThat(enviado.getValue().getSimulado()).isSameAs(simuladoDoBanco);
        assertThat(enviado.getValue().getQuestao()).isSameAs(questaoDoBanco);
        assertThat(enviado.getValue().getOrdem()).isEqualTo(1);
    }

    @Test
    @DisplayName("administrador também passa pela rota")
    void administradorVincula() throws Exception {
        given(simuladoRepository.findById(SIMULADO_ID)).willReturn(Optional.of(simulado(SIMULADO_ID)));
        given(questaoRepository.findById(QUESTAO_ID)).willReturn(Optional.of(questao(QUESTAO_ID)));
        given(simuladoQuestaoService.salvar(any())).willAnswer(chamada -> chamada.getArgument(0));

        mockMvc.perform(post("/simulado-questao")
                        .with(comoAdministrador())
                        .contentType(JSON)
                        .content("{\"simuladoId\":100,\"questaoId\":200}"))
                .andExpect(status().isOk());

        verify(simuladoQuestaoService).salvar(any());
    }

    private static Simulado simulado(long id) {
        Simulado simulado = new Simulado();
        simulado.setId(id);
        return simulado;
    }

    private static Questao questao(long id) {
        Questao questao = new Questao();
        questao.setId(id);
        return questao;
    }
}
