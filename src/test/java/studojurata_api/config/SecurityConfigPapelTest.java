package studojurata_api.config;

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

import studojurata_api.controller.AlunoController;
import studojurata_api.service.AlunoService;
import studojurata_api.support.AuthorizationTestSupport;
import studojurata_api.support.AutorizacaoWebTestBase;

/**
 * Matriz de autorização por papel exatamente como está hoje no
 * {@link SecurityConfig}.
 *
 * <p>Este teste <b>documenta o estado atual</b>, não o estado desejado: as
 * regras são por rota e por papel, e o recorte "cada um só vê o que é seu"
 * ainda não existe no backend (é o objeto dos blocos seguintes). Por isso os
 * casos abaixo cobrem somente permissão por papel — nada de posse ou escopo.
 *
 * <p>Rotas negadas são verificadas sem carregar o controller correspondente:
 * o filtro de segurança decide antes do dispatch, então basta a rota existir
 * nas regras do {@code SecurityConfig}.
 */
@WebMvcTest(controllers = AlunoController.class)
class SecurityConfigPapelTest extends AutorizacaoWebTestBase {

    private static final long ALUNO_ID = 7L;
    private static final long PROFESSOR_ID = 42L;

    @MockBean
    private AlunoService alunoService;

    @AfterEach
    void limparContexto() {
        AuthorizationTestSupport.limparContexto();
    }

    @Test
    @DisplayName("requisição sem autenticação é negada")
    void semAutenticacaoEhNegada() throws Exception {
        mockMvc.perform(get("/alunos")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("aluno não pode criar turma (escrita de gestão pedagógica)")
    void alunoNaoPodeCriarTurma() throws Exception {
        mockMvc.perform(comCorpoVazio(post("/turmas")).with(comoAluno(ALUNO_ID)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("aluno não pode listar usuários")
    void alunoNaoPodeListarUsuarios() throws Exception {
        mockMvc.perform(get("/usuarios").with(comoAluno(ALUNO_ID)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("professor não pode listar usuários (rota é do administrador)")
    void professorNaoPodeListarUsuarios() throws Exception {
        mockMvc.perform(get("/usuarios").with(comoProfessor(PROFESSOR_ID)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("aluno não pode listar a nota de todos")
    void alunoNaoPodeListarNotasGerais() throws Exception {
        mockMvc.perform(get("/notas").with(comoAluno(ALUNO_ID)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("aluno não pode cadastrar aluno")
    void alunoNaoPodeCadastrarAluno() throws Exception {
        mockMvc.perform(comCorpoVazio(post("/alunos")).with(comoAluno(ALUNO_ID)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("aluno pode consultar a lista de alunos (rota liberada hoje; recorte é do frontend)")
    void alunoPodeConsultarListaDeAlunos() throws Exception {
        given(alunoService.listar()).willReturn(List.of());

        mockMvc.perform(get("/alunos").with(comoAluno(ALUNO_ID))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("professor pode consultar a lista de alunos")
    void professorPodeConsultarListaDeAlunos() throws Exception {
        given(alunoService.listar()).willReturn(List.of());

        mockMvc.perform(get("/alunos").with(comoProfessor(PROFESSOR_ID))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("administrador pode cadastrar aluno")
    void administradorPodeCadastrarAluno() throws Exception {
        mockMvc.perform(comCorpoVazio(post("/alunos")).with(comoAdministrador()))
                .andExpect(status().isOk());
    }
}
