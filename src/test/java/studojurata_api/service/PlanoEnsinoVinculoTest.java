package studojurata_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import studojurata_api.exception.RegraNegocioException;
import studojurata_api.model.Curso;
import studojurata_api.model.CursoDisciplina;
import studojurata_api.model.Disciplina;
import studojurata_api.model.PlanoEnsino;
import studojurata_api.model.TurmaDisciplina;
import studojurata_api.model.enums.StatusAtivoInativo;
import studojurata_api.repository.CursoDisciplinaRepository;
import studojurata_api.repository.CursoRepository;
import studojurata_api.repository.PlanoEnsinoRepository;
import studojurata_api.repository.TurmaDisciplinaRepository;
import studojurata_api.security.EscolaContext;
import studojurata_api.security.EscopoUsuario;
import studojurata_api.security.UsuarioAutenticado;
import studojurata_api.support.AuthorizationTestSupport;

/**
 * Passo 6 — disciplina inativada (na disciplina, no vínculo com a turma ou na
 * grade do curso) não pode receber plano de ensino novo.
 *
 * <p>O front esconde a opção no select, mas o back é quem decide: sem esta
 * validação, um POST/PUT direto continuaria vinculando o plano à disciplina
 * inativa. Plano que já existe e mantém o vínculo atual segue editável — o
 * histórico de quem usava a disciplina não fica travado.
 */
@ExtendWith(MockitoExtension.class)
class PlanoEnsinoVinculoTest {

    private static final long CURSO_ID = 1L;
    private static final long DISCIPLINA_ID = 10L;
    private static final long VINCULO_ID = 100L;
    private static final long OUTRO_VINCULO_ID = 200L;
    private static final long PLANO_ID = 300L;

    @Mock private PlanoEnsinoRepository planoEnsinoRepository;
    @Mock private CursoRepository cursoRepository;
    @Mock private TurmaDisciplinaRepository turmaDisciplinaRepository;
    @Mock private CursoDisciplinaRepository cursoDisciplinaRepository;
    @Mock private PlanoAulaService planoAulaService;
    @Mock private EscolaContext escolaContext;
    @Mock private EscopoUsuario escopoUsuario;
    @Mock private studojurata_api.security.EscopoProfessor escopoProfessor;

    private PlanoEnsinoService planoEnsinoService;

    @BeforeEach
    void setUp() {
        UsuarioAutenticado usuarioAutenticado = new UsuarioAutenticado();
        planoEnsinoService = new PlanoEnsinoService(planoEnsinoRepository, cursoRepository, turmaDisciplinaRepository,
                cursoDisciplinaRepository, planoAulaService, escolaContext, usuarioAutenticado, escopoUsuario,
                new studojurata_api.security.PlanejamentoAccessGuard(usuarioAutenticado, escopoUsuario, escopoProfessor));
        // Os cenários aqui são de regra estrutural: quem opera é o administrador.
        AuthorizationTestSupport.autenticarComoAdministrador();
    }

    @org.junit.jupiter.api.AfterEach
    void limparContexto() {
        AuthorizationTestSupport.limparContexto();
    }

    @Test
    @DisplayName("cria plano para vínculo ativo com disciplina na grade ativa do curso")
    void criaPlanoComVinculoUtilizavel() {
        dadoCursoAtivo();
        given(turmaDisciplinaRepository.findById(VINCULO_ID))
                .willReturn(Optional.of(vinculo(VINCULO_ID, StatusAtivoInativo.ATIVO, StatusAtivoInativo.ATIVO)));
        dadoGrade(StatusAtivoInativo.ATIVO);
        given(planoEnsinoRepository.save(any())).willAnswer(chamada -> chamada.getArgument(0));

        PlanoEnsino salvo = planoEnsinoService.salvar(plano(VINCULO_ID));

        assertThat(salvo.getTurmaDisciplina().getId()).isEqualTo(VINCULO_ID);
        verify(planoEnsinoRepository).save(any());
    }

    @Test
    @DisplayName("recusa plano para vínculo turma-disciplina inativado")
    void recusaVinculoInativado() {
        dadoCursoAtivo();
        given(turmaDisciplinaRepository.findById(VINCULO_ID))
                .willReturn(Optional.of(vinculo(VINCULO_ID, StatusAtivoInativo.INATIVO, StatusAtivoInativo.ATIVO)));

        assertThatThrownBy(() -> planoEnsinoService.salvar(plano(VINCULO_ID)))
                .isInstanceOf(RegraNegocioException.class);

        verify(planoEnsinoRepository, never()).save(any());
        verifyNoInteractions(cursoDisciplinaRepository);
    }

    @Test
    @DisplayName("recusa plano para disciplina inativada")
    void recusaDisciplinaInativada() {
        dadoCursoAtivo();
        given(turmaDisciplinaRepository.findById(VINCULO_ID))
                .willReturn(Optional.of(vinculo(VINCULO_ID, StatusAtivoInativo.ATIVO, StatusAtivoInativo.INATIVO)));

        assertThatThrownBy(() -> planoEnsinoService.salvar(plano(VINCULO_ID)))
                .isInstanceOf(RegraNegocioException.class);

        verify(planoEnsinoRepository, never()).save(any());
    }

    @Test
    @DisplayName("recusa plano para disciplina fora da grade ativa do curso")
    void recusaDisciplinaForaDaGradeAtiva() {
        dadoCursoAtivo();
        given(turmaDisciplinaRepository.findById(VINCULO_ID))
                .willReturn(Optional.of(vinculo(VINCULO_ID, StatusAtivoInativo.ATIVO, StatusAtivoInativo.ATIVO)));
        dadoGrade(StatusAtivoInativo.INATIVO);

        assertThatThrownBy(() -> planoEnsinoService.salvar(plano(VINCULO_ID)))
                .isInstanceOf(RegraNegocioException.class);

        verify(planoEnsinoRepository, never()).save(any());
    }

    @Test
    @DisplayName("plano genérico (sem vínculo) continua aceito")
    void aceitaPlanoGenerico() {
        dadoCursoAtivo();
        given(planoEnsinoRepository.save(any())).willAnswer(chamada -> chamada.getArgument(0));

        assertThat(planoEnsinoService.salvar(plano(null))).isNotNull();

        verifyNoInteractions(turmaDisciplinaRepository, cursoDisciplinaRepository);
    }

    @Test
    @DisplayName("editar plano que mantém o vínculo atual continua permitido")
    void edicaoMantendoVinculoNaoRevalida() {
        dadoCursoAtivo();
        PlanoEnsino existente = plano(VINCULO_ID);
        existente.setId(PLANO_ID);
        given(planoEnsinoRepository.findById(PLANO_ID)).willReturn(Optional.of(existente));
        given(planoEnsinoRepository.save(any())).willAnswer(chamada -> chamada.getArgument(0));

        // A disciplina pode ter sido inativada depois: o plano histórico segue editável.
        PlanoEnsino salvo = planoEnsinoService.atualizar(PLANO_ID, plano(VINCULO_ID));

        assertThat(salvo.getId()).isEqualTo(PLANO_ID);
        verifyNoInteractions(turmaDisciplinaRepository, cursoDisciplinaRepository);
    }

    @Test
    @DisplayName("editar trocando de vínculo valida o vínculo novo")
    void edicaoTrocandoVinculoValida() {
        dadoCursoAtivo();
        PlanoEnsino existente = plano(VINCULO_ID);
        existente.setId(PLANO_ID);
        given(planoEnsinoRepository.findById(PLANO_ID)).willReturn(Optional.of(existente));
        given(turmaDisciplinaRepository.findById(OUTRO_VINCULO_ID))
                .willReturn(Optional.of(vinculo(OUTRO_VINCULO_ID, StatusAtivoInativo.INATIVO, StatusAtivoInativo.ATIVO)));

        assertThatThrownBy(() -> planoEnsinoService.atualizar(PLANO_ID, plano(OUTRO_VINCULO_ID)))
                .isInstanceOf(RegraNegocioException.class);

        verify(planoEnsinoRepository, never()).save(any());
    }

    // --- helpers -----------------------------------------------------------

    private void dadoCursoAtivo() {
        Curso curso = new Curso();
        curso.setId(CURSO_ID);
        curso.setNome("Curso de Teste");
        curso.setStatus(StatusAtivoInativo.ATIVO);
        given(cursoRepository.findById(CURSO_ID)).willReturn(Optional.of(curso));
    }

    private void dadoGrade(StatusAtivoInativo status) {
        CursoDisciplina item = new CursoDisciplina();
        item.setId(1L);
        item.setCurso(curso());
        item.setDisciplina(disciplina(StatusAtivoInativo.ATIVO));
        item.setStatus(status);
        given(cursoDisciplinaRepository.findByCurso_Id(CURSO_ID)).willReturn(List.of(item));
    }

    private static PlanoEnsino plano(Long vinculoId) {
        PlanoEnsino plano = new PlanoEnsino();
        plano.setCurso(curso());
        if (vinculoId != null) {
            TurmaDisciplina vinculo = new TurmaDisciplina();
            vinculo.setId(vinculoId);
            plano.setTurmaDisciplina(vinculo);
        }
        return plano;
    }

    private static TurmaDisciplina vinculo(Long id, StatusAtivoInativo statusVinculo, StatusAtivoInativo statusDisciplina) {
        TurmaDisciplina vinculo = new TurmaDisciplina();
        vinculo.setId(id);
        vinculo.setStatus(statusVinculo);
        vinculo.setDisciplina(disciplina(statusDisciplina));
        return vinculo;
    }

    private static Disciplina disciplina(StatusAtivoInativo status) {
        Disciplina disciplina = new Disciplina();
        disciplina.setId(DISCIPLINA_ID);
        disciplina.setTitulo("Robótica");
        disciplina.setStatus(status);
        return disciplina;
    }

    private static Curso curso() {
        Curso curso = new Curso();
        curso.setId(CURSO_ID);
        curso.setNome("Curso de Teste");
        return curso;
    }
}
