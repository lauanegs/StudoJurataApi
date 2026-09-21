package studojurata_api.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import studojurata_api.mapper.QuestaoAlunoMapper;
import studojurata_api.model.Aluno;
import studojurata_api.model.AlunoTurma;
import studojurata_api.service.AlunoService;
import studojurata_api.service.AlunoTurmaService;
import studojurata_api.service.FrequenciaService;
import studojurata_api.service.NotaService;
import studojurata_api.service.QuestaoAlunoService;
import studojurata_api.service.SimuladoAlunoService;
import studojurata_api.support.AuthorizationTestSupport;
import studojurata_api.support.AutorizacaoComEscopoTestBase;

/**
 * C1 — demais endpoints individuais indexados por aluno ou por turma:
 * respostas de questão, frequência, histórico de notas, aluno e matrícula.
 */
@WebMvcTest(controllers = {
        QuestaoAlunoController.class,
        FrequenciaController.class,
        NotaController.class,
        AlunoController.class,
        AlunoTurmaController.class
})
class C1AlunoRelacionadosEscopoTest extends AutorizacaoComEscopoTestBase {

    private static final long ALUNO_ID = 7L;
    private static final long OUTRO_ALUNO_ID = 8L;
    private static final long PROFESSOR_ID = 42L;
    private static final long TURMA_ID = 10L;
    private static final long OUTRA_TURMA_ID = 99L;
    private static final long TENTATIVA_ID = 500L;
    private static final long MATRICULA_ID = 700L;

    @MockBean private QuestaoAlunoService questaoAlunoService;
    @MockBean private QuestaoAlunoMapper questaoAlunoMapper;
    @MockBean private SimuladoAlunoService simuladoAlunoService;
    @MockBean private FrequenciaService frequenciaService;
    @MockBean private NotaService notaService;
    @MockBean private AlunoService alunoService;
    @MockBean private AlunoTurmaService alunoTurmaService;

    @AfterEach
    void limparContexto() {
        AuthorizationTestSupport.limparContexto();
    }

    // --- GET /questao-aluno (listagem agregada de respostas) ---------------

    @Test
    @DisplayName("aluno não acessa a listagem agregada de respostas")
    void alunoNaoAcessaListagemAgregadaDeRespostas() throws Exception {
        mockMvc.perform(get("/questao-aluno").with(comoAluno(ALUNO_ID))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("professor acessa a listagem agregada de respostas (escopo por turma fica no C2)")
    void professorAcessaListagemAgregadaDeRespostas() throws Exception {
        given(questaoAlunoService.listar()).willReturn(List.of());

        mockMvc.perform(get("/questao-aluno").with(comoProfessor(PROFESSOR_ID))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("administrador acessa a listagem agregada de respostas")
    void administradorAcessaListagemAgregadaDeRespostas() throws Exception {
        given(questaoAlunoService.listar()).willReturn(List.of());

        mockMvc.perform(get("/questao-aluno").with(comoAdministrador())).andExpect(status().isOk());
    }

    // --- GET /questao-aluno/simulado-aluno/{id} ----------------------------

    @Test
    @DisplayName("aluno acessa respostas da própria tentativa")
    void alunoAcessaRespostasDaPropriaTentativa() throws Exception {
        given(simuladoAlunoService.buscar(TENTATIVA_ID)).willReturn(tentativa(TENTATIVA_ID, ALUNO_ID));
        given(questaoAlunoService.listarPorSimuladoAluno(TENTATIVA_ID)).willReturn(List.of());

        mockMvc.perform(get("/questao-aluno/simulado-aluno/{id}", TENTATIVA_ID).with(comoAluno(ALUNO_ID)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("aluno não acessa respostas de tentativa alheia")
    void alunoNaoAcessaRespostasDeTentativaAlheia() throws Exception {
        given(simuladoAlunoService.buscar(TENTATIVA_ID)).willReturn(tentativa(TENTATIVA_ID, OUTRO_ALUNO_ID));

        mockMvc.perform(get("/questao-aluno/simulado-aluno/{id}", TENTATIVA_ID).with(comoAluno(ALUNO_ID)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("professor acessa respostas de aluno das suas turmas")
    void professorAcessaRespostasDeAlunoDoEscopo() throws Exception {
        given(simuladoAlunoService.buscar(TENTATIVA_ID)).willReturn(tentativa(TENTATIVA_ID, ALUNO_ID));
        given(questaoAlunoService.listarPorSimuladoAluno(TENTATIVA_ID)).willReturn(List.of());
        professorLecionaParaAluno(PROFESSOR_ID, ALUNO_ID, TURMA_ID);

        mockMvc.perform(get("/questao-aluno/simulado-aluno/{id}", TENTATIVA_ID).with(comoProfessor(PROFESSOR_ID)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("professor não acessa respostas de aluno fora do escopo")
    void professorNaoAcessaRespostasForaDoEscopo() throws Exception {
        given(simuladoAlunoService.buscar(TENTATIVA_ID)).willReturn(tentativa(TENTATIVA_ID, OUTRO_ALUNO_ID));
        professorEhTitular(PROFESSOR_ID, OUTRA_TURMA_ID);

        mockMvc.perform(get("/questao-aluno/simulado-aluno/{id}", TENTATIVA_ID).with(comoProfessor(PROFESSOR_ID)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("administrador acessa respostas de qualquer tentativa")
    void administradorAcessaRespostasDeQualquerTentativa() throws Exception {
        given(simuladoAlunoService.buscar(TENTATIVA_ID)).willReturn(tentativa(TENTATIVA_ID, OUTRO_ALUNO_ID));
        given(questaoAlunoService.listarPorSimuladoAluno(TENTATIVA_ID)).willReturn(List.of());

        mockMvc.perform(get("/questao-aluno/simulado-aluno/{id}", TENTATIVA_ID).with(comoAdministrador()))
                .andExpect(status().isOk());
    }

    // --- GET /frequencia/aluno/{alunoId} -----------------------------------

    @Test
    @DisplayName("aluno acessa a própria frequência")
    void alunoAcessaPropriaFrequencia() throws Exception {
        given(frequenciaService.listarPorAluno(ALUNO_ID)).willReturn(List.of());

        mockMvc.perform(get("/frequencia/aluno/{alunoId}", ALUNO_ID).with(comoAluno(ALUNO_ID)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("aluno não acessa frequência de outro aluno")
    void alunoNaoAcessaFrequenciaDeOutro() throws Exception {
        mockMvc.perform(get("/frequencia/aluno/{alunoId}", OUTRO_ALUNO_ID).with(comoAluno(ALUNO_ID)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("professor acessa frequência de aluno das suas turmas")
    void professorAcessaFrequenciaDeAlunoDoEscopo() throws Exception {
        given(frequenciaService.listarPorAluno(ALUNO_ID)).willReturn(List.of());
        professorLecionaParaAluno(PROFESSOR_ID, ALUNO_ID, TURMA_ID);

        mockMvc.perform(get("/frequencia/aluno/{alunoId}", ALUNO_ID).with(comoProfessor(PROFESSOR_ID)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("professor não acessa frequência de aluno fora do escopo")
    void professorNaoAcessaFrequenciaForaDoEscopo() throws Exception {
        professorEhTitular(PROFESSOR_ID, OUTRA_TURMA_ID);

        mockMvc.perform(get("/frequencia/aluno/{alunoId}", ALUNO_ID).with(comoProfessor(PROFESSOR_ID)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("administrador acessa frequência de qualquer aluno")
    void administradorAcessaFrequenciaDeQualquerAluno() throws Exception {
        given(frequenciaService.listarPorAluno(OUTRO_ALUNO_ID)).willReturn(List.of());

        mockMvc.perform(get("/frequencia/aluno/{alunoId}", OUTRO_ALUNO_ID).with(comoAdministrador()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("sem autenticação a frequência é negada")
    void semAutenticacaoNaoAcessaFrequencia() throws Exception {
        mockMvc.perform(get("/frequencia/aluno/{alunoId}", ALUNO_ID)).andExpect(status().isForbidden());
    }

    // --- GET /notas/aluno/{alunoId}/historico ------------------------------

    @Test
    @DisplayName("aluno acessa o próprio histórico de notas")
    void alunoAcessaProprioHistoricoDeNotas() throws Exception {
        given(notaService.historicoPorAluno(ALUNO_ID)).willReturn(List.of());

        mockMvc.perform(get("/notas/aluno/{alunoId}/historico", ALUNO_ID).with(comoAluno(ALUNO_ID)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("aluno não acessa histórico de notas de outro aluno")
    void alunoNaoAcessaHistoricoDeOutro() throws Exception {
        mockMvc.perform(get("/notas/aluno/{alunoId}/historico", OUTRO_ALUNO_ID).with(comoAluno(ALUNO_ID)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("professor acessa histórico de aluno das suas turmas")
    void professorAcessaHistoricoDeAlunoDoEscopo() throws Exception {
        given(notaService.historicoPorAluno(ALUNO_ID)).willReturn(List.of());
        professorLecionaParaAluno(PROFESSOR_ID, ALUNO_ID, TURMA_ID);

        mockMvc.perform(get("/notas/aluno/{alunoId}/historico", ALUNO_ID).with(comoProfessor(PROFESSOR_ID)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("professor não acessa histórico de aluno fora do escopo")
    void professorNaoAcessaHistoricoForaDoEscopo() throws Exception {
        professorEhTitular(PROFESSOR_ID, OUTRA_TURMA_ID);

        mockMvc.perform(get("/notas/aluno/{alunoId}/historico", ALUNO_ID).with(comoProfessor(PROFESSOR_ID)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("administrador acessa histórico de qualquer aluno")
    void administradorAcessaHistoricoDeQualquerAluno() throws Exception {
        given(notaService.historicoPorAluno(OUTRO_ALUNO_ID)).willReturn(List.of());

        mockMvc.perform(get("/notas/aluno/{alunoId}/historico", OUTRO_ALUNO_ID).with(comoAdministrador()))
                .andExpect(status().isOk());
    }

    // --- GET /alunos/{id} --------------------------------------------------

    @Test
    @DisplayName("aluno acessa o próprio cadastro")
    void alunoAcessaProprioCadastro() throws Exception {
        given(alunoService.buscar(ALUNO_ID)).willReturn(aluno(ALUNO_ID));

        mockMvc.perform(get("/alunos/{id}", ALUNO_ID).with(comoAluno(ALUNO_ID))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("aluno não acessa cadastro de outro aluno")
    void alunoNaoAcessaCadastroDeOutro() throws Exception {
        mockMvc.perform(get("/alunos/{id}", OUTRO_ALUNO_ID).with(comoAluno(ALUNO_ID)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("professor acessa cadastro de aluno das suas turmas")
    void professorAcessaCadastroDeAlunoDoEscopo() throws Exception {
        given(alunoService.buscar(ALUNO_ID)).willReturn(aluno(ALUNO_ID));
        professorLecionaParaAluno(PROFESSOR_ID, ALUNO_ID, TURMA_ID);

        mockMvc.perform(get("/alunos/{id}", ALUNO_ID).with(comoProfessor(PROFESSOR_ID))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("professor não acessa cadastro de aluno fora do escopo")
    void professorNaoAcessaCadastroForaDoEscopo() throws Exception {
        professorEhTitular(PROFESSOR_ID, OUTRA_TURMA_ID);

        mockMvc.perform(get("/alunos/{id}", ALUNO_ID).with(comoProfessor(PROFESSOR_ID)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("administrador acessa cadastro de qualquer aluno")
    void administradorAcessaCadastroDeQualquerAluno() throws Exception {
        given(alunoService.buscar(OUTRO_ALUNO_ID)).willReturn(aluno(OUTRO_ALUNO_ID));

        mockMvc.perform(get("/alunos/{id}", OUTRO_ALUNO_ID).with(comoAdministrador())).andExpect(status().isOk());
    }

    // --- GET /aluno-turma --------------------------------------------------

    @Test
    @DisplayName("aluno não acessa a listagem agregada de matrículas")
    void alunoNaoAcessaListagemDeMatriculas() throws Exception {
        mockMvc.perform(get("/aluno-turma").with(comoAluno(ALUNO_ID))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("professor acessa a listagem agregada de matrículas (escopo por turma fica no C2)")
    void professorAcessaListagemDeMatriculas() throws Exception {
        given(alunoTurmaService.listar()).willReturn(List.of());

        mockMvc.perform(get("/aluno-turma").with(comoProfessor(PROFESSOR_ID))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("administrador acessa a listagem agregada de matrículas")
    void administradorAcessaListagemDeMatriculas() throws Exception {
        given(alunoTurmaService.listar()).willReturn(List.of());

        mockMvc.perform(get("/aluno-turma").with(comoAdministrador())).andExpect(status().isOk());
    }

    @Test
    @DisplayName("aluno acessa a própria matrícula")
    void alunoAcessaPropriaMatricula() throws Exception {
        given(alunoTurmaService.buscar(MATRICULA_ID)).willReturn(matricula(MATRICULA_ID, ALUNO_ID));

        mockMvc.perform(get("/aluno-turma/{id}", MATRICULA_ID).with(comoAluno(ALUNO_ID)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("aluno não acessa matrícula de outro aluno")
    void alunoNaoAcessaMatriculaDeOutro() throws Exception {
        given(alunoTurmaService.buscar(MATRICULA_ID)).willReturn(matricula(MATRICULA_ID, OUTRO_ALUNO_ID));

        mockMvc.perform(get("/aluno-turma/{id}", MATRICULA_ID).with(comoAluno(ALUNO_ID)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("professor acessa matrícula de aluno das suas turmas")
    void professorAcessaMatriculaDeAlunoDoEscopo() throws Exception {
        given(alunoTurmaService.buscar(MATRICULA_ID)).willReturn(matricula(MATRICULA_ID, ALUNO_ID));
        professorLecionaParaAluno(PROFESSOR_ID, ALUNO_ID, TURMA_ID);

        mockMvc.perform(get("/aluno-turma/{id}", MATRICULA_ID).with(comoProfessor(PROFESSOR_ID)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("professor não acessa matrícula de aluno fora do escopo")
    void professorNaoAcessaMatriculaForaDoEscopo() throws Exception {
        given(alunoTurmaService.buscar(MATRICULA_ID)).willReturn(matricula(MATRICULA_ID, ALUNO_ID));
        professorEhTitular(PROFESSOR_ID, OUTRA_TURMA_ID);

        mockMvc.perform(get("/aluno-turma/{id}", MATRICULA_ID).with(comoProfessor(PROFESSOR_ID)))
                .andExpect(status().isForbidden());
    }

    // --- GET /aluno-turma/turma/{turmaId}/ativos|historico -----------------

    @Test
    @DisplayName("professor lista alunos ativos da sua turma")
    void professorListaAlunosAtivosDaSuaTurma() throws Exception {
        given(alunoTurmaService.ativosPorTurma(TURMA_ID)).willReturn(List.of());
        professorEhTitular(PROFESSOR_ID, TURMA_ID);

        mockMvc.perform(get("/aluno-turma/turma/{turmaId}/ativos", TURMA_ID).with(comoProfessor(PROFESSOR_ID)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("professor lista histórico da sua turma")
    void professorListaHistoricoDaSuaTurma() throws Exception {
        given(alunoTurmaService.historicoPorTurma(TURMA_ID)).willReturn(List.of());
        professorEhTitular(PROFESSOR_ID, TURMA_ID);

        mockMvc.perform(get("/aluno-turma/turma/{turmaId}/historico", TURMA_ID).with(comoProfessor(PROFESSOR_ID)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("professor não lista alunos de turma alheia")
    void professorNaoListaAlunosDeTurmaAlheia() throws Exception {
        professorEhTitular(PROFESSOR_ID, OUTRA_TURMA_ID);

        mockMvc.perform(get("/aluno-turma/turma/{turmaId}/ativos", TURMA_ID).with(comoProfessor(PROFESSOR_ID)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("aluno não lista alunos de turma")
    void alunoNaoListaAlunosDeTurma() throws Exception {
        mockMvc.perform(get("/aluno-turma/turma/{turmaId}/ativos", TURMA_ID).with(comoAluno(ALUNO_ID)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("administrador lista alunos e histórico de qualquer turma")
    void administradorListaAlunosDeQualquerTurma() throws Exception {
        given(alunoTurmaService.ativosPorTurma(OUTRA_TURMA_ID)).willReturn(List.of());
        given(alunoTurmaService.historicoPorTurma(OUTRA_TURMA_ID)).willReturn(List.of());

        mockMvc.perform(get("/aluno-turma/turma/{turmaId}/ativos", OUTRA_TURMA_ID).with(comoAdministrador()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/aluno-turma/turma/{turmaId}/historico", OUTRA_TURMA_ID).with(comoAdministrador()))
                .andExpect(status().isOk());
    }

    private static Aluno aluno(long alunoId) {
        Aluno aluno = new Aluno();
        aluno.setId(alunoId);
        return aluno;
    }

    private static AlunoTurma matricula(long matriculaId, long alunoId) {
        AlunoTurma matricula = new AlunoTurma();
        matricula.setId(matriculaId);
        matricula.setAluno(aluno(alunoId));
        return matricula;
    }
}
