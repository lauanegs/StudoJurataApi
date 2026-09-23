package studojurata_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import studojurata_api.exception.RegraNegocioException;
import studojurata_api.model.Curso;
import studojurata_api.model.CursoDisciplina;
import studojurata_api.model.Disciplina;
import studojurata_api.model.Turma;
import studojurata_api.model.TurmaDisciplina;
import studojurata_api.model.enums.StatusAtivoInativo;
import studojurata_api.repository.AlunoTurmaRepository;
import studojurata_api.repository.CursoDisciplinaRepository;
import studojurata_api.repository.CursoRepository;
import studojurata_api.repository.DisciplinaRepository;
import studojurata_api.repository.PlanoAulaRepository;
import studojurata_api.repository.PlanoEnsinoRepository;
import studojurata_api.repository.TurmaDisciplinaRepository;
import studojurata_api.repository.TurmaRepository;
import studojurata_api.security.EscopoProfessor;
import studojurata_api.security.EscopoUsuario;
import studojurata_api.security.PlanejamentoAccessGuard;
import studojurata_api.security.UsuarioAutenticado;
import studojurata_api.support.AuthorizationTestSupport;

/**
 * Bloco A — disciplina inativada não vira vínculo novo.
 *
 * <p>O select do plano de ensino já escondia a disciplina inativa e o
 * {@code PlanoEnsinoService} já recusava o vínculo; faltava fechar as duas
 * portas que criam o vínculo antes disso: a grade curricular do curso e o
 * vínculo turma × disciplina. Sem elas, quem manda o id direto à API cria
 * linhas que nunca viram aula.
 */
@ExtendWith(MockitoExtension.class)
class C2DisciplinaInativaVinculoTest {

    private static final long CURSO_ID = 3L;
    private static final long TURMA_ID = 10L;
    private static final long DISCIPLINA_ID = 55L;

    @Mock private CursoDisciplinaRepository cursoDisciplinaRepository;
    @Mock private CursoRepository cursoRepository;
    @Mock private DisciplinaRepository disciplinaRepository;
    @Mock private TurmaDisciplinaRepository turmaDisciplinaRepository;
    @Mock private TurmaRepository turmaRepository;
    @Mock private PlanoEnsinoRepository planoEnsinoRepository;
    @Mock private PlanoAulaRepository planoAulaRepository;
    @Mock private AlunoTurmaRepository alunoTurmaRepository;
    @Mock private EscopoProfessor escopoProfessor;
    @Mock private EscopoUsuario escopoUsuario;

    @AfterEach
    void limparContexto() {
        AuthorizationTestSupport.limparContexto();
    }

    @Test
    @DisplayName("disciplina inativa não entra na grade curricular do curso")
    void disciplinaInativaNaoEntraNaGrade() {
        var service = new CursoDisciplinaService(cursoDisciplinaRepository, cursoRepository, disciplinaRepository);
        given(cursoRepository.findById(CURSO_ID)).willReturn(Optional.of(curso()));
        given(disciplinaRepository.findById(DISCIPLINA_ID)).willReturn(Optional.of(disciplinaInativa()));

        CursoDisciplina vinculo = new CursoDisciplina();
        vinculo.setCurso(cursoComId());
        vinculo.setDisciplina(disciplinaComId());
        vinculo.setCargaHoraria(40);

        try {
            service.salvar(vinculo);
            fail("esperava 409");
        } catch (RegraNegocioException esperada) {
            assertThat(esperada.getMessage()).contains("inativa");
        }

        verify(cursoDisciplinaRepository, never()).save(any(CursoDisciplina.class));
    }

    @Test
    @DisplayName("disciplina inativa não é vinculada a uma turma")
    void disciplinaInativaNaoEntraNaTurma() {
        AuthorizationTestSupport.autenticarComoAdministrador();
        var guard = new PlanejamentoAccessGuard(new UsuarioAutenticado(), escopoUsuario, escopoProfessor);
        var service = new TurmaDisciplinaService(turmaDisciplinaRepository, turmaRepository,
                cursoDisciplinaRepository, disciplinaRepository, planoEnsinoRepository, planoAulaRepository,
                alunoTurmaRepository, new UsuarioAutenticado(), guard);

        given(turmaRepository.findById(TURMA_ID)).willReturn(Optional.of(turma()));
        given(disciplinaRepository.findById(DISCIPLINA_ID)).willReturn(Optional.of(disciplinaInativa()));

        TurmaDisciplina vinculo = new TurmaDisciplina();
        vinculo.setTurma(turmaComId());
        vinculo.setDisciplina(disciplinaComId());

        try {
            service.salvar(vinculo);
            fail("esperava 409");
        } catch (RegraNegocioException esperada) {
            assertThat(esperada.getMessage()).contains("inativa");
        }

        verify(turmaDisciplinaRepository, never()).save(any(TurmaDisciplina.class));
    }

    // --- apoio --------------------------------------------------------------

    private static Curso curso() {
        Curso curso = cursoComId();
        curso.setNome("Curso A");
        curso.setStatus(StatusAtivoInativo.ATIVO);
        return curso;
    }

    private static Curso cursoComId() {
        Curso curso = new Curso();
        curso.setId(CURSO_ID);
        return curso;
    }

    private static Disciplina disciplinaInativa() {
        Disciplina disciplina = disciplinaComId();
        disciplina.setTitulo("Banco de Dados");
        disciplina.setStatus(StatusAtivoInativo.INATIVO);
        return disciplina;
    }

    private static Disciplina disciplinaComId() {
        Disciplina disciplina = new Disciplina();
        disciplina.setId(DISCIPLINA_ID);
        return disciplina;
    }

    private static Turma turma() {
        Turma turma = turmaComId();
        turma.setCurso(cursoComId());
        return turma;
    }

    private static Turma turmaComId() {
        Turma turma = new Turma();
        turma.setId(TURMA_ID);
        return turma;
    }
}
