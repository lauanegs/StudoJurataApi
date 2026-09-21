package studojurata_api.support;

import static org.mockito.BDDMockito.given;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import studojurata_api.config.SecurityConfig;
import studojurata_api.model.Aluno;
import studojurata_api.model.Simulado;
import studojurata_api.model.SimuladoAluno;
import studojurata_api.model.Turma;
import studojurata_api.model.TurmaDisciplina;
import studojurata_api.repository.AlunoTurmaRepository;
import studojurata_api.repository.TurmaDisciplinaRepository;
import studojurata_api.security.AlunoAccessGuard;
import studojurata_api.security.CustomUserDetailsService;
import studojurata_api.security.EscopoProfessor;

/**
 * Base das fatias que exercitam o C1 com os <b>guardas reais</b>.
 *
 * <p>Diferença para {@link AutorizacaoWebTestBase}: lá o {@code AlunoAccessGuard}
 * é dublado, porque o objetivo é a matriz de papéis do {@code SecurityConfig}.
 * Aqui ele é importado de verdade (junto com o {@code EscopoProfessor}), e só
 * os repositórios de escopo entram como dublês — assim a decisão de autorização
 * é a mesma de produção, sem banco.
 *
 * <p>As subclasses devem declarar
 * {@code @WebMvcTest(controllers = ...)} e dublar os services do controller.
 */
@Import({ SecurityConfig.class, AlunoAccessGuard.class, EscopoProfessor.class })
public abstract class AutorizacaoComEscopoTestBase {

    protected static final MediaType JSON = MediaType.APPLICATION_JSON;

    @MockBean
    protected JpaMetamodelMappingContext jpaMappingContext;

    @MockBean
    protected CustomUserDetailsService customUserDetailsService;

    /** Consultas de escopo dubladas: nenhum teste toca o banco. */
    @MockBean
    protected TurmaDisciplinaRepository turmaDisciplinaRepository;

    @MockBean
    protected AlunoTurmaRepository alunoTurmaRepository;

    @Autowired
    protected MockMvc mockMvc;

    protected RequestPostProcessor comoAluno(long alunoId) {
        return SecurityMockMvcRequestPostProcessors.authentication(
                AuthorizationTestSupport.autenticacaoDeAluno(alunoId));
    }

    protected RequestPostProcessor comoProfessor(long professorId) {
        return SecurityMockMvcRequestPostProcessors.authentication(
                AuthorizationTestSupport.autenticacaoDeProfessor(professorId));
    }

    protected RequestPostProcessor comoAdministrador() {
        return SecurityMockMvcRequestPostProcessors.authentication(
                AuthorizationTestSupport.autenticacaoDeAdministrador());
    }

    // --- Cenários de escopo -------------------------------------------------

    /** O professor é titular destas turmas. */
    protected void professorEhTitular(long professorId, long... turmaIds) {
        List<TurmaDisciplina> vinculos = new ArrayList<>();
        for (long turmaId : turmaIds) {
            vinculos.add(vinculoDaTurma(turmaId));
        }
        given(turmaDisciplinaRepository.findByProfessorId(professorId)).willReturn(vinculos);
    }

    /**
     * O professor leciona para o aluno (matrícula de qualquer status em uma das
     * turmas informadas) — o mesmo conjunto que o guard monta a partir dos vínculos.
     */
    protected void professorLecionaParaAluno(long professorId, long alunoId, long... turmaIds) {
        professorEhTitular(professorId, turmaIds);

        Set<Long> turmas = new java.util.LinkedHashSet<>();
        for (long turmaId : turmaIds) {
            turmas.add(turmaId);
        }
        given(alunoTurmaRepository.existsByAluno_IdAndTurma_IdIn(alunoId, turmas)).willReturn(true);
    }

    // --- Construtores de entidades -----------------------------------------

    protected static TurmaDisciplina vinculoDaTurma(long turmaId) {
        TurmaDisciplina vinculo = new TurmaDisciplina();
        vinculo.setId(turmaId);
        vinculo.setTurma(turmaComId(turmaId));
        return vinculo;
    }

    protected static Turma turmaComId(long turmaId) {
        Turma turma = new Turma();
        turma.setId(turmaId);
        return turma;
    }

    protected static SimuladoAluno tentativa(long tentativaId, long alunoId) {
        Aluno aluno = new Aluno();
        aluno.setId(alunoId);

        SimuladoAluno tentativa = new SimuladoAluno();
        tentativa.setId(tentativaId);
        tentativa.setAluno(aluno);
        return tentativa;
    }

    /** Simulado com turma; {@code turmaId} nulo monta o simulado órfão. */
    protected static Simulado simulado(long simuladoId, Long turmaId) {
        Simulado simulado = new Simulado();
        simulado.setId(simuladoId);

        if (turmaId != null) {
            simulado.setTurma(turmaComId(turmaId));
        }

        return simulado;
    }
}
