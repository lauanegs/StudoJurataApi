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

import studojurata_api.ia.controller.GeracaoIAController;
import studojurata_api.ia.dto.SimuladoGeradoIAResponseDTO;
import studojurata_api.ia.mapper.SimuladoGeradoIAMapper;
import studojurata_api.ia.model.SimuladoGeradoIA;
import studojurata_api.ia.service.GeracaoSimuladoIAService;
import studojurata_api.mapper.SimuladoMapper;
import studojurata_api.support.AuthorizationTestSupport;
import studojurata_api.support.AutorizacaoWebTestBase;

/**
 * Papel de quem pode consultar a listagem de simulados gerados pela IA.
 *
 * <p>O <b>escopo</b> de cada registro é decidido no service (ver
 * {@code C2GeracaoIAListagemEscopoTest}); aqui o que se prova é a regra de rota
 * do {@code SecurityConfig}: aluno não entra nessa listagem e requisição sem
 * autenticação não chega ao service.
 */
@WebMvcTest(controllers = GeracaoIAController.class)
class C2GeracaoIAListagemWebTest extends AutorizacaoWebTestBase {

    private static final long ALUNO_ID = 7L;
    private static final long PROFESSOR_ID = 42L;

    @MockBean private GeracaoSimuladoIAService geracaoSimuladoIAService;
    @MockBean private SimuladoMapper simuladoMapper;
    @MockBean private SimuladoGeradoIAMapper simuladoGeradoIAMapper;

    @AfterEach
    void limparContexto() {
        AuthorizationTestSupport.limparContexto();
    }

    @Test
    @DisplayName("professor lista os vínculos de IA do seu escopo")
    void professorListaVinculosDoEscopo() throws Exception {
        dadoVinculo();

        mockMvc.perform(get("/ia/geracao/simulado").with(comoProfessor(PROFESSOR_ID)))
                .andExpect(status().isOk());

        verify(geracaoSimuladoIAService).listarGerados();
    }

    @Test
    @DisplayName("administrador lista os vínculos de IA")
    void administradorListaVinculos() throws Exception {
        dadoVinculo();

        mockMvc.perform(get("/ia/geracao/simulado").with(comoAdministrador()))
                .andExpect(status().isOk());

        verify(geracaoSimuladoIAService).listarGerados();
    }

    @Test
    @DisplayName("aluno não lista vínculos de IA e o service não é chamado")
    void alunoNaoListaVinculos() throws Exception {
        mockMvc.perform(get("/ia/geracao/simulado").with(comoAluno(ALUNO_ID)))
                .andExpect(status().isForbidden());

        verify(geracaoSimuladoIAService, never()).listarGerados();
    }

    @Test
    @DisplayName("sem autenticação a listagem de IA não é consultada")
    void semAutenticacaoNaoListaVinculos() throws Exception {
        mockMvc.perform(get("/ia/geracao/simulado"))
                .andExpect(status().isForbidden());

        verify(geracaoSimuladoIAService, never()).listarGerados();
    }

    private void dadoVinculo() {
        given(geracaoSimuladoIAService.listarGerados()).willReturn(List.of(new SimuladoGeradoIA()));
        given(simuladoGeradoIAMapper.toResponseDTO(any(SimuladoGeradoIA.class)))
                .willReturn(new SimuladoGeradoIAResponseDTO());
    }
}
