package studojurata_api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import studojurata_api.model.Pessoa;
import studojurata_api.model.Responsavel;
import studojurata_api.service.PessoaService;
import studojurata_api.service.ResponsavelService;
import studojurata_api.support.AuthorizationTestSupport;
import studojurata_api.support.AutorizacaoWebTestBase;

/**
 * C2 — leitura do cadastro de pessoas e de responsáveis.
 *
 * <p>As duas rotas devolvem entidade crua, com dado pessoal (CPF, endereço,
 * contato e data de nascimento). Antes desta fatia, qualquer autenticado — aluno
 * incluído — lia o cadastro inteiro; agora {@code /responsaveis/**} é do
 * administrador e {@code /pessoas/**} é de professor ou administrador, porque a
 * Home do professor usa essa listagem para os aniversariantes da semana.
 *
 * <p>Rotas negadas são verificadas pelo filtro de segurança: o service não pode
 * ser chamado quando o acesso é recusado.
 */
@WebMvcTest(controllers = { PessoaController.class, ResponsavelController.class })
class C2CadastroPessoalEscopoTest extends AutorizacaoWebTestBase {

    private static final long ALUNO_ID = 7L;
    private static final long PROFESSOR_ID = 42L;
    private static final long RESPONSAVEL_ID = 300L;

    @MockBean private PessoaService pessoaService;
    @MockBean private ResponsavelService responsavelService;

    @AfterEach
    void limparContexto() {
        AuthorizationTestSupport.limparContexto();
    }

    // --- GET /pessoas -------------------------------------------------------

    @Test
    @DisplayName("professor lista pessoas (aniversariantes da Home) e o service é chamado")
    void professorListaPessoas() throws Exception {
        given(pessoaService.listar()).willReturn(List.of(new Pessoa()));

        mockMvc.perform(get("/pessoas").with(comoProfessor(PROFESSOR_ID)))
                .andExpect(status().isOk());

        verify(pessoaService).listar();
    }

    @Test
    @DisplayName("administrador lista pessoas")
    void administradorListaPessoas() throws Exception {
        given(pessoaService.listar()).willReturn(List.of(new Pessoa()));

        mockMvc.perform(get("/pessoas").with(comoAdministrador()))
                .andExpect(status().isOk());

        verify(pessoaService).listar();
    }

    @Test
    @DisplayName("aluno não lista pessoas — cadastro traz dado pessoal — e nada é consultado")
    void alunoNaoListaPessoas() throws Exception {
        mockMvc.perform(get("/pessoas").with(comoAluno(ALUNO_ID)))
                .andExpect(status().isForbidden());

        verify(pessoaService, never()).listar();
    }

    @Test
    @DisplayName("sem autenticação não lista pessoas")
    void semAutenticacaoNaoListaPessoas() throws Exception {
        mockMvc.perform(get("/pessoas"))
                .andExpect(status().isForbidden());

        verify(pessoaService, never()).listar();
    }

    // --- GET /responsaveis --------------------------------------------------

    @Test
    @DisplayName("administrador lista e busca responsáveis")
    void administradorLeResponsaveis() throws Exception {
        given(responsavelService.listar()).willReturn(List.of(new Responsavel()));
        given(responsavelService.buscar(RESPONSAVEL_ID)).willReturn(new Responsavel());

        mockMvc.perform(get("/responsaveis").with(comoAdministrador()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/responsaveis/{id}", RESPONSAVEL_ID).with(comoAdministrador()))
                .andExpect(status().isOk());

        verify(responsavelService).listar();
        verify(responsavelService).buscar(RESPONSAVEL_ID);
    }

    @Test
    @DisplayName("professor não lista nem busca responsáveis (rota do administrador)")
    void professorNaoLeResponsaveis() throws Exception {
        mockMvc.perform(get("/responsaveis").with(comoProfessor(PROFESSOR_ID)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/responsaveis/{id}", RESPONSAVEL_ID).with(comoProfessor(PROFESSOR_ID)))
                .andExpect(status().isForbidden());

        verify(responsavelService, never()).listar();
        verify(responsavelService, never()).buscar(any());
    }

    @Test
    @DisplayName("aluno não lista nem busca responsáveis")
    void alunoNaoLeResponsaveis() throws Exception {
        mockMvc.perform(get("/responsaveis").with(comoAluno(ALUNO_ID)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/responsaveis/{id}", RESPONSAVEL_ID).with(comoAluno(ALUNO_ID)))
                .andExpect(status().isForbidden());

        verify(responsavelService, never()).listar();
        verify(responsavelService, never()).buscar(any());
    }

    @Test
    @DisplayName("sem autenticação não lê responsáveis")
    void semAutenticacaoNaoLeResponsaveis() throws Exception {
        mockMvc.perform(get("/responsaveis"))
                .andExpect(status().isForbidden());

        verify(responsavelService, never()).listar();
    }

    // --- escrita continua restrita ao administrador -------------------------

    @Test
    @DisplayName("professor não cria pessoa nem responsável")
    void professorNaoEscreveCadastro() throws Exception {
        mockMvc.perform(comCorpoVazio(post("/pessoas")).with(comoProfessor(PROFESSOR_ID)))
                .andExpect(status().isForbidden());
        mockMvc.perform(comCorpoVazio(post("/responsaveis")).with(comoProfessor(PROFESSOR_ID)))
                .andExpect(status().isForbidden());

        verify(pessoaService, never()).salvar(any());
        verify(responsavelService, never()).salvar(any());
    }
}
