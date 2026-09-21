package studojurata_api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import studojurata_api.model.Aluno;
import studojurata_api.model.AlunoTurma;
import studojurata_api.model.Turma;
import studojurata_api.model.TurmaDisciplina;
import studojurata_api.model.enums.StatusMatricula;
import studojurata_api.repository.AlunoTurmaRepository;
import studojurata_api.repository.TurmaDisciplinaRepository;

/**
 * Teste unitário do {@link EscopoProfessor}, com repositórios dublados: não
 * abre contexto Spring nem banco. Verifica o que o componente devolve e quais
 * consultas ele faz.
 */
@ExtendWith(MockitoExtension.class)
class EscopoProfessorTest {

    private static final long PROFESSOR_ID = 9L;
    private static final long TURMA_A = 10L;
    private static final long TURMA_B = 20L;

    @Mock
    private TurmaDisciplinaRepository turmaDisciplinaRepository;

    @Mock
    private AlunoTurmaRepository alunoTurmaRepository;

    @InjectMocks
    private EscopoProfessor escopo;

    @Test
    @DisplayName("devolve os ids dos vínculos turma+disciplina do professor")
    void devolveIdsDosVinculos() {
        given(turmaDisciplinaRepository.findByProfessorId(PROFESSOR_ID)).willReturn(List.of(
                vinculo(1L, TURMA_A),
                vinculo(2L, TURMA_B)));

        assertThat(escopo.turmaDisciplinaIdsDoProfessor(PROFESSOR_ID)).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    @DisplayName("não repete a turma quando há dois vínculos na mesma turma")
    void naoRepeteTurmaEmDoisVinculos() {
        given(turmaDisciplinaRepository.findByProfessorId(PROFESSOR_ID)).willReturn(List.of(
                vinculo(1L, TURMA_A),
                vinculo(2L, TURMA_A)));

        assertThat(escopo.turmaIdsDoProfessor(PROFESSOR_ID)).containsExactly(TURMA_A);
    }

    @Test
    @DisplayName("ignora vínculo sem turma em vez de estourar NullPointer")
    void ignoraVinculoSemTurma() {
        given(turmaDisciplinaRepository.findByProfessorId(PROFESSOR_ID)).willReturn(List.of(
                vinculo(1L, null),
                vinculo(2L, TURMA_A)));

        assertThat(escopo.turmaIdsDoProfessor(PROFESSOR_ID)).containsExactly(TURMA_A);
    }

    @Test
    @DisplayName("agrega alunos das turmas do professor sem repetir quem está em duas")
    void agregaAlunosDasTurmasSemRepetir() {
        given(turmaDisciplinaRepository.findByProfessorId(PROFESSOR_ID)).willReturn(List.of(
                vinculo(1L, TURMA_A),
                vinculo(2L, TURMA_B)));
        given(alunoTurmaRepository.findByTurmaIdAndStatus(TURMA_A, StatusMatricula.ATIVA))
                .willReturn(List.of(matricula(7L), matricula(8L)));
        given(alunoTurmaRepository.findByTurmaIdAndStatus(TURMA_B, StatusMatricula.ATIVA))
                .willReturn(List.of(matricula(7L)));

        assertThat(escopo.alunoIdsDoProfessor(PROFESSOR_ID)).containsExactlyInAnyOrder(7L, 8L);
    }

    @Test
    @DisplayName("consulta apenas matrículas ATIVAS ao montar o conjunto de alunos")
    void consultaApenasMatriculasAtivas() {
        given(turmaDisciplinaRepository.findByProfessorId(PROFESSOR_ID)).willReturn(List.of(vinculo(1L, TURMA_A)));
        given(alunoTurmaRepository.findByTurmaIdAndStatus(TURMA_A, StatusMatricula.ATIVA))
                .willReturn(List.of(matricula(7L)));

        escopo.alunoIdsDoProfessor(PROFESSOR_ID);

        verify(alunoTurmaRepository).findByTurmaIdAndStatus(TURMA_A, StatusMatricula.ATIVA);
    }

    @Test
    @DisplayName("professor sem vínculos devolve conjunto vazio e não consulta matrículas")
    void professorSemVinculosNaoConsultaMatriculas() {
        given(turmaDisciplinaRepository.findByProfessorId(PROFESSOR_ID)).willReturn(List.of());

        assertThat(escopo.turmaIdsDoProfessor(PROFESSOR_ID)).isEmpty();
        assertThat(escopo.turmaDisciplinaIdsDoProfessor(PROFESSOR_ID)).isEmpty();
        assertThat(escopo.alunoIdsDoProfessor(PROFESSOR_ID)).isEmpty();

        verifyNoInteractions(alunoTurmaRepository);
    }

    @Test
    @DisplayName("professorId nulo não consulta repositório nenhum")
    void professorIdNuloNaoConsultaRepositorios() {
        assertThat(escopo.turmaIdsDoProfessor(null)).isEmpty();
        assertThat(escopo.turmaDisciplinaIdsDoProfessor(null)).isEmpty();
        assertThat(escopo.alunoIdsDoProfessor(null)).isEmpty();

        verifyNoInteractions(turmaDisciplinaRepository, alunoTurmaRepository);
    }

    @Test
    @DisplayName("professor leciona para aluno de uma das suas turmas (custo fixo de 2 consultas)")
    void professorLecionaParaAlunoDoEscopo() {
        given(turmaDisciplinaRepository.findByProfessorId(PROFESSOR_ID)).willReturn(List.of(vinculo(1L, TURMA_A)));
        given(alunoTurmaRepository.existsByAluno_IdAndTurma_IdIn(7L, Set.of(TURMA_A))).willReturn(true);

        assertThat(escopo.lecionaPara(PROFESSOR_ID, 7L)).isTrue();

        verify(turmaDisciplinaRepository).findByProfessorId(PROFESSOR_ID);
        verify(alunoTurmaRepository).existsByAluno_IdAndTurma_IdIn(7L, Set.of(TURMA_A));
    }

    @Test
    @DisplayName("professor não leciona para aluno de turma alheia")
    void professorNaoLecionaParaAlunoDeTurmaAlheia() {
        given(turmaDisciplinaRepository.findByProfessorId(PROFESSOR_ID)).willReturn(List.of(vinculo(1L, TURMA_A)));
        given(alunoTurmaRepository.existsByAluno_IdAndTurma_IdIn(8L, Set.of(TURMA_A))).willReturn(false);

        assertThat(escopo.lecionaPara(PROFESSOR_ID, 8L)).isFalse();
    }

    @Test
    @DisplayName("pertencimento não filtra status de matrícula: histórico continua visível")
    void pertencimentoNaoFiltraStatusDeMatricula() {
        given(turmaDisciplinaRepository.findByProfessorId(PROFESSOR_ID)).willReturn(List.of(vinculo(1L, TURMA_A)));
        given(alunoTurmaRepository.existsByAluno_IdAndTurma_IdIn(7L, Set.of(TURMA_A))).willReturn(true);

        assertThat(escopo.lecionaPara(PROFESSOR_ID, 7L)).isTrue();

        // O único método do repositório chamado é o de pertencimento, que não
        // recebe status — por isso matrícula concluída/cancelada também conta.
        verifyNoMoreInteractions(alunoTurmaRepository);
    }

    @Test
    @DisplayName("professor sem turmas devolve false sem consultar pertencimento")
    void professorSemTurmasNaoConsultaPertencimento() {
        given(turmaDisciplinaRepository.findByProfessorId(PROFESSOR_ID)).willReturn(List.of());

        assertThat(escopo.lecionaPara(PROFESSOR_ID, 7L)).isFalse();

        verifyNoInteractions(alunoTurmaRepository);
    }

    @Test
    @DisplayName("ids nulos devolvem false sem consultar repositório")
    void idsNulosNaoConsultamPertencimento() {
        assertThat(escopo.lecionaPara(null, 7L)).isFalse();
        assertThat(escopo.lecionaPara(PROFESSOR_ID, null)).isFalse();

        verifyNoInteractions(turmaDisciplinaRepository, alunoTurmaRepository);
    }

    private static TurmaDisciplina vinculo(Long vinculoId, Long turmaId) {
        TurmaDisciplina vinculo = new TurmaDisciplina();
        vinculo.setId(vinculoId);

        if (turmaId != null) {
            Turma turma = new Turma();
            turma.setId(turmaId);
            vinculo.setTurma(turma);
        }

        return vinculo;
    }

    private static AlunoTurma matricula(Long alunoId) {
        Aluno aluno = new Aluno();
        aluno.setId(alunoId);

        AlunoTurma matricula = new AlunoTurma();
        matricula.setAluno(aluno);
        return matricula;
    }
}
