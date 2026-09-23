package studojurata_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import studojurata_api.dto.LancarSimuladoRequest;
import studojurata_api.exception.RequisicaoInvalidaException;
import studojurata_api.ia.model.SimuladoGeradoIA;
import studojurata_api.ia.repository.SimuladoGeradoIARepository;
import studojurata_api.model.Aluno;
import studojurata_api.model.AlunoTurma;
import studojurata_api.model.Disciplina;
import studojurata_api.model.Questao;
import studojurata_api.model.Simulado;
import studojurata_api.model.SimuladoAluno;
import studojurata_api.model.SimuladoQuestao;
import studojurata_api.model.Turma;
import studojurata_api.model.enums.StatusQuestao;
import studojurata_api.model.enums.StatusSimulado;
import studojurata_api.model.enums.StatusSimuladoAluno;
import studojurata_api.model.enums.StatusSimuladoQuestao;
import studojurata_api.model.enums.TipoDestinacaoSimulado;
import studojurata_api.repository.AlunoRepository;
import studojurata_api.repository.SimuladoAlunoRepository;
import studojurata_api.repository.SimuladoQuestaoRepository;
import studojurata_api.repository.SimuladoRepository;
import studojurata_api.security.EscopoProfessor;
import studojurata_api.security.SimuladoAccessGuard;
import studojurata_api.security.UsuarioAutenticado;
import studojurata_api.support.AuthorizationTestSupport;

/**
 * Lançamento de simulado gerado pela IA (C1).
 *
 * <p>A IA cria o simulado como ESPECIFICO e grava um vínculo aluno × conteúdo.
 * O lançamento usa esse vínculo como fonte do destinatário: o professor aprova
 * as questões e o simulado vai para o aluno que a IA indicou — o corpo da
 * requisição não escolhe (nem troca) quem recebe.
 */
@ExtendWith(MockitoExtension.class)
class C2SimuladoLancamentoIATest {

    private static final long PROFESSOR_ID = 42L;
    private static final long TURMA_ID = 10L;
    private static final long DISC_A = 5L;
    private static final long SIMULADO_ID = 300L;
    private static final long ALUNO_ID = 7L;
    private static final long OUTRO_ALUNO_ID = 8L;
    private static final long QUESTAO_ID = 99L;

    @Mock private SimuladoRepository repository;
    @Mock private SimuladoQuestaoRepository simuladoQuestaoRepository;
    @Mock private SimuladoAlunoRepository simuladoAlunoRepository;
    @Mock private SimuladoGeradoIARepository simuladoGeradoIARepository;
    @Mock private AlunoTurmaService alunoTurmaService;
    @Mock private AlunoRepository alunoRepository;
    @Mock private EscopoProfessor escopoProfessor;

    private SimuladoService simuladoService;

    @BeforeEach
    void setUp() {
        UsuarioAutenticado usuarioAutenticado = new UsuarioAutenticado();
        SimuladoAccessGuard guard = new SimuladoAccessGuard(usuarioAutenticado, escopoProfessor);
        simuladoService = new SimuladoService(repository, simuladoQuestaoRepository, simuladoAlunoRepository,
                simuladoGeradoIARepository, alunoTurmaService, alunoRepository, usuarioAutenticado, escopoProfessor,
                guard);
    }

    @AfterEach
    void limparContexto() {
        AuthorizationTestSupport.limparContexto();
    }

    @Test
    @DisplayName("lança simulado de IA para o aluno do vínculo, mesmo sem alunoIds no corpo")
    void lancaSimuladoIAparaAlunoDoVinculo() {
        dadoSimuladoDeIA();
        dadoAluno(ALUNO_ID);

        Simulado lancado = simuladoService.lancar(SIMULADO_ID, null);

        assertThat(lancado.getStatus()).isEqualTo(StatusSimulado.PUBLICADO);

        ArgumentCaptor<SimuladoAluno> tentativa = ArgumentCaptor.forClass(SimuladoAluno.class);
        verify(simuladoAlunoRepository).save(tentativa.capture());
        assertThat(tentativa.getValue().getAluno().getId()).isEqualTo(ALUNO_ID);
        assertThat(tentativa.getValue().getStatus()).isEqualTo(StatusSimuladoAluno.PENDENTE);
    }

    @Test
    @DisplayName("repetir o lançamento não cria tentativa duplicada")
    void naoDuplicaTentativa() {
        dadoSimuladoDeIA();
        dadoAluno(ALUNO_ID);
        given(simuladoAlunoRepository.existsBySimuladoIdAndAlunoId(SIMULADO_ID, ALUNO_ID)).willReturn(true);

        Simulado lancado = simuladoService.lancar(SIMULADO_ID, null);

        assertThat(lancado.getStatus()).isEqualTo(StatusSimulado.PUBLICADO);
        verify(simuladoAlunoRepository, never()).save(any());
    }

    @Test
    @DisplayName("professor não redireciona o simulado de IA para outro aluno")
    void naoRedirecionaSimuladoIA() {
        dadoSimuladoDeIA();

        LancarSimuladoRequest pedido = new LancarSimuladoRequest();
        pedido.setAlunoIds(List.of(OUTRO_ALUNO_ID));

        try {
            simuladoService.lancar(SIMULADO_ID, pedido);
            fail("esperava recusa de destinatário");
        } catch (RequisicaoInvalidaException esperada) {
            assertThat(esperada.getMessage()).contains("IA");
        }

        verify(simuladoAlunoRepository, never()).save(any());
    }

    @Test
    @DisplayName("simulado manual ESPECIFICO continua exigindo a lista de alunos")
    void simuladoManualEspecificoContinuaExigindoAlunos() {
        Simulado rascunho = dadoRascunhoComQuestaoAprovada();
        rascunho.setTipoDestinacao(TipoDestinacaoSimulado.ESPECIFICO);
        dadoEscopoDoProfessor();
        given(simuladoGeradoIARepository.findBySimulado_Id(SIMULADO_ID)).willReturn(List.of());

        try {
            simuladoService.lancar(SIMULADO_ID, null);
            fail("esperava exigência de alunos");
        } catch (RequisicaoInvalidaException esperada) {
            assertThat(esperada.getMessage()).contains("ESPECIFICO");
        }

        verify(simuladoAlunoRepository, never()).save(any());
    }

    @Test
    @DisplayName("simulado manual com destinação TODOS continua funcionando")
    void simuladoManualTodosContinuaFuncionando() {
        Simulado rascunho = dadoRascunhoComQuestaoAprovada();
        rascunho.setTipoDestinacao(TipoDestinacaoSimulado.TODOS);
        dadoEscopoDoProfessor();
        given(alunoTurmaService.ativosPorTurma(TURMA_ID)).willReturn(List.of(matricula(ALUNO_ID)));

        Simulado lancado = simuladoService.lancar(SIMULADO_ID, null);

        assertThat(lancado.getStatus()).isEqualTo(StatusSimulado.PUBLICADO);
        verify(simuladoAlunoRepository).save(any());
    }

    // --- apoio --------------------------------------------------------------

    private void dadoSimuladoDeIA() {
        Simulado rascunho = dadoRascunhoComQuestaoAprovada();
        rascunho.setTipoDestinacao(TipoDestinacaoSimulado.ESPECIFICO);
        dadoEscopoDoProfessor();

        SimuladoGeradoIA vinculoDeIA = new SimuladoGeradoIA();
        vinculoDeIA.setId(1L);
        vinculoDeIA.setSimulado(rascunho);
        vinculoDeIA.setAluno(aluno(ALUNO_ID));
        given(simuladoGeradoIARepository.findBySimulado_Id(SIMULADO_ID)).willReturn(List.of(vinculoDeIA));
    }

    private Simulado dadoRascunhoComQuestaoAprovada() {
        Simulado rascunho = new Simulado();
        rascunho.setId(SIMULADO_ID);
        rascunho.setTitulo("Reforço automático");
        rascunho.setStatus(StatusSimulado.RASCUNHO);
        rascunho.setTurma(turma(TURMA_ID));
        rascunho.setDisciplina(disciplina(DISC_A));
        given(repository.findById(SIMULADO_ID)).willReturn(Optional.of(rascunho));
        // Lenient: os testes de recusa (destinatário trocado) não chegam a salvar.
        lenient().when(repository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));
        given(simuladoQuestaoRepository.findBySimuladoIdAndStatusOrderByOrdem(SIMULADO_ID, StatusSimuladoQuestao.ATIVA))
                .willReturn(List.of(vinculoDeQuestao()));
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        return rascunho;
    }

    private void dadoEscopoDoProfessor() {
        given(escopoProfessor.turmaIdsDoProfessor(PROFESSOR_ID)).willReturn(Set.of(TURMA_ID));
        given(escopoProfessor.disciplinaIdsDoProfessor(PROFESSOR_ID)).willReturn(Set.of(DISC_A));
    }

    private void dadoAluno(long alunoId) {
        given(alunoRepository.findById(alunoId)).willReturn(Optional.of(aluno(alunoId)));
    }

    private static SimuladoQuestao vinculoDeQuestao() {
        Questao questao = new Questao();
        questao.setId(QUESTAO_ID);
        questao.setStatus(StatusQuestao.APROVADA);

        SimuladoQuestao vinculo = new SimuladoQuestao();
        vinculo.setQuestao(questao);
        return vinculo;
    }

    private static AlunoTurma matricula(long alunoId) {
        AlunoTurma matricula = new AlunoTurma();
        matricula.setAluno(aluno(alunoId));
        return matricula;
    }

    private static Aluno aluno(long alunoId) {
        Aluno aluno = new Aluno();
        aluno.setId(alunoId);
        return aluno;
    }

    private static Turma turma(long turmaId) {
        Turma turma = new Turma();
        turma.setId(turmaId);
        return turma;
    }

    private static Disciplina disciplina(long disciplinaId) {
        Disciplina disciplina = new Disciplina();
        disciplina.setId(disciplinaId);
        return disciplina;
    }
}
