package studojurata_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import studojurata_api.dto.ChamadaRequest;
import studojurata_api.model.Aula;
import studojurata_api.model.ConteudoPlano;
import studojurata_api.model.Curso;
import studojurata_api.model.Disciplina;
import studojurata_api.model.Escola;
import studojurata_api.model.PlanoAula;
import studojurata_api.model.PlanoEnsino;
import studojurata_api.model.Turma;
import studojurata_api.model.TurmaDisciplina;
import studojurata_api.model.enums.StatusPlano;
import studojurata_api.repository.AlunoRepository;
import studojurata_api.repository.AlunoTurmaRepository;
import studojurata_api.repository.AulaConteudoRepository;
import studojurata_api.repository.AulaRepository;
import studojurata_api.repository.ConteudoPlanoRepository;
import studojurata_api.repository.CursoDisciplinaRepository;
import studojurata_api.repository.CursoRepository;
import studojurata_api.repository.DisciplinaRepository;
import studojurata_api.repository.FrequenciaRepository;
import studojurata_api.repository.PlanoAulaRepository;
import studojurata_api.repository.PlanoEnsinoRepository;
import studojurata_api.repository.TurmaDisciplinaRepository;
import studojurata_api.repository.TurmaRepository;
import studojurata_api.security.AlunoAccessGuard;
import studojurata_api.security.EscolaContext;
import studojurata_api.security.EscopoProfessor;
import studojurata_api.security.EscopoUsuario;
import studojurata_api.security.PlanejamentoAccessGuard;
import studojurata_api.security.UsuarioAutenticado;
import studojurata_api.support.AuthorizationTestSupport;

/**
 * Passo 10 — escopo do domínio de planejamento (plano de ensino, plano de aula,
 * conteúdo, aula, vínculo turma-disciplina e caminhos indiretos).
 *
 * <p>A regra é uma só: o vínculo turma-disciplina do recurso precisa estar no
 * escopo do professor (ADMIN passa; ALUNO não escreve). Recurso sem vínculo
 * (plano genérico/conteúdo sem plano, D-C2) continua legível e só o ADMIN
 * escreve.
 */
@ExtendWith(MockitoExtension.class)
class C2PlanejamentoEscopoTest {

    private static final long ESCOLA_ID = 1L;
    private static final long ALUNO_ID = 7L;
    private static final long PROFESSOR_ID = 42L;
    private static final long VINCULO_MEU = 100L;
    private static final long VINCULO_ALHEIO = 200L;
    private static final long TURMA_ID = 10L;
    private static final long PLANO_ENSINO_ID = 300L;
    private static final long PLANO_AULA_ID = 400L;
    private static final long CONTEUDO_ID = 500L;
    private static final long AULA_ID = 600L;

    @Mock private PlanoEnsinoRepository planoEnsinoRepository;
    @Mock private PlanoAulaRepository planoAulaRepository;
    @Mock private ConteudoPlanoRepository conteudoPlanoRepository;
    @Mock private AulaRepository aulaRepository;
    @Mock private AulaConteudoRepository aulaConteudoRepository;
    @Mock private TurmaDisciplinaRepository turmaDisciplinaRepository;
    @Mock private CursoDisciplinaRepository cursoDisciplinaRepository;
    @Mock private CursoRepository cursoRepository;
    @Mock private PlanoAulaService planoAulaServiceDublado;
    @Mock private EscolaContext escolaContext;
    @Mock private EscopoProfessor escopoProfessor;
    @Mock private EscopoUsuario escopoUsuario;
    @Mock private FrequenciaRepository frequenciaRepository;
    @Mock private AlunoRepository alunoRepository;
    @Mock private AlunoTurmaRepository alunoTurmaRepository;
    @Mock private TurmaRepository turmaRepository;
    @Mock private AlunoTurmaService alunoTurmaService;
    @Mock private DisciplinaRepository disciplinaRepository;
    @Mock private studojurata_api.repository.HorarioTurmaRepository horarioTurmaRepository;

    private PlanoEnsinoService planoEnsinoService;
    private PlanoAulaService planoAulaService;
    private ConteudoPlanoService conteudoPlanoService;
    private AulaService aulaService;
    private AulaConteudoService aulaConteudoService;
    private TurmaDisciplinaService turmaDisciplinaService;
    private FrequenciaService frequenciaService;
    private CursoService cursoService;
    private DisciplinaService disciplinaService;

    @BeforeEach
    void setUp() {
        UsuarioAutenticado usuarioAutenticado = new UsuarioAutenticado();
        PlanejamentoAccessGuard guard = new PlanejamentoAccessGuard(usuarioAutenticado, escopoUsuario, escopoProfessor);
        AlunoAccessGuard alunoAccessGuard = new AlunoAccessGuard(escopoProfessor);

        planoEnsinoService = new PlanoEnsinoService(planoEnsinoRepository, cursoRepository, turmaDisciplinaRepository,
                cursoDisciplinaRepository, planoAulaServiceDublado, escolaContext, usuarioAutenticado, escopoUsuario, guard);
        planoAulaService = new PlanoAulaService(planoAulaRepository, aulaRepository, planoEnsinoRepository,
                usuarioAutenticado, escopoUsuario, guard);
        conteudoPlanoService = new ConteudoPlanoService(conteudoPlanoRepository, aulaConteudoRepository,
                usuarioAutenticado, escopoUsuario, guard);
        aulaService = new AulaService(aulaRepository, planoAulaRepository, planoEnsinoRepository,
                horarioTurmaRepository, new AuditLogServiceStub(),
                usuarioAutenticado, escopoUsuario, guard);
        aulaConteudoService = new AulaConteudoService(aulaConteudoRepository, aulaRepository, conteudoPlanoRepository, guard);
        turmaDisciplinaService = new TurmaDisciplinaService(turmaDisciplinaRepository, turmaRepository,
                cursoDisciplinaRepository, disciplinaRepository, planoEnsinoRepository, planoAulaRepository,
                alunoTurmaRepository, usuarioAutenticado, guard);
        frequenciaService = new FrequenciaService(frequenciaRepository, aulaRepository, alunoRepository,
                alunoTurmaRepository, turmaRepository, alunoTurmaService, guard, alunoAccessGuard);
        cursoService = new CursoService(cursoRepository, escolaContext);
        disciplinaService = new DisciplinaService(disciplinaRepository, escolaContext);
    }

    @AfterEach
    void limparContexto() {
        AuthorizationTestSupport.limparContexto();
    }

    // --- leituras individuais ----------------------------------------------

    @Test
    @DisplayName("plano de ensino: professor le o plano do proprio vinculo")
    void professorLePlanoEnsinoProprio() {
        autenticarProfessorComVinculos(VINCULO_MEU);
        given(planoEnsinoRepository.findById(PLANO_ENSINO_ID))
                .willReturn(Optional.of(planoEnsino(PLANO_ENSINO_ID, VINCULO_MEU)));

        assertThat(planoEnsinoService.buscarParaLeitura(PLANO_ENSINO_ID).getId()).isEqualTo(PLANO_ENSINO_ID);
    }

    @Test
    @DisplayName("plano de ensino: professor nao le o plano de outro vinculo")
    void professorNaoLePlanoEnsinoAlheio() {
        autenticarProfessorComVinculos(VINCULO_MEU);
        given(planoEnsinoRepository.findById(PLANO_ENSINO_ID))
                .willReturn(Optional.of(planoEnsino(PLANO_ENSINO_ID, VINCULO_ALHEIO)));

        assertForbidden(() -> planoEnsinoService.buscarParaLeitura(PLANO_ENSINO_ID));
    }

    @Test
    @DisplayName("plano de ensino generico (D-C2) continua legivel")
    void planoEnsinoGenericoContinuaLegivel() {
        // Sem vínculo não há o que consultar: a leitura nem toca o escopo.
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(planoEnsinoRepository.findById(PLANO_ENSINO_ID))
                .willReturn(Optional.of(planoEnsino(PLANO_ENSINO_ID, null)));

        assertThat(planoEnsinoService.buscarParaLeitura(PLANO_ENSINO_ID).getId()).isEqualTo(PLANO_ENSINO_ID);

        verifyNoInteractions(escopoUsuario);
    }

    @Test
    @DisplayName("plano de aula: professor nao le plano de outro vinculo")
    void professorNaoLePlanoAulaAlheio() {
        autenticarProfessorComVinculos(VINCULO_MEU);
        given(planoAulaRepository.findById(PLANO_AULA_ID))
                .willReturn(Optional.of(planoAula(PLANO_AULA_ID, VINCULO_ALHEIO)));

        assertForbidden(() -> planoAulaService.buscarParaLeitura(PLANO_AULA_ID));
        assertForbidden(() -> planoAulaService.estatisticas(PLANO_AULA_ID));
    }

    @Test
    @DisplayName("conteudo do plano: professor nao le conteudo de outro vinculo")
    void professorNaoLeConteudoAlheio() {
        autenticarProfessorComVinculos(VINCULO_MEU);
        given(conteudoPlanoRepository.findById(CONTEUDO_ID))
                .willReturn(Optional.of(conteudo(CONTEUDO_ID, VINCULO_ALHEIO)));

        assertForbidden(() -> conteudoPlanoService.buscarParaLeitura(CONTEUDO_ID));
    }

    @Test
    @DisplayName("conteudo sem plano (D-C2) continua legivel")
    void conteudoSemPlanoContinuaLegivel() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(conteudoPlanoRepository.findById(CONTEUDO_ID))
                .willReturn(Optional.of(conteudo(CONTEUDO_ID, null)));

        assertThat(conteudoPlanoService.buscarParaLeitura(CONTEUDO_ID).getId()).isEqualTo(CONTEUDO_ID);

        verifyNoInteractions(escopoUsuario);
    }

    @Test
    @DisplayName("aula: professor nao le aula de outro vinculo")
    void professorNaoLeAulaAlheia() {
        autenticarProfessorComVinculos(VINCULO_MEU);
        given(aulaRepository.findById(AULA_ID)).willReturn(Optional.of(aula(AULA_ID, VINCULO_ALHEIO)));

        assertForbidden(() -> aulaService.buscarParaLeitura(AULA_ID));
    }

    @Test
    @DisplayName("administrador le recurso de qualquer vinculo sem consultar escopo")
    void administradorLeRecursoDeOutroVinculo() {
        AuthorizationTestSupport.autenticarComoAdministrador();
        given(planoEnsinoRepository.findById(PLANO_ENSINO_ID))
                .willReturn(Optional.of(planoEnsino(PLANO_ENSINO_ID, VINCULO_ALHEIO)));
        given(aulaRepository.findById(AULA_ID)).willReturn(Optional.of(aula(AULA_ID, VINCULO_ALHEIO)));

        assertThat(planoEnsinoService.buscarParaLeitura(PLANO_ENSINO_ID).getId()).isEqualTo(PLANO_ENSINO_ID);
        assertThat(aulaService.buscarParaLeitura(AULA_ID).getId()).isEqualTo(AULA_ID);

        verifyNoInteractions(escopoUsuario);
    }

    // --- escritas ----------------------------------------------------------

    @Test
    @DisplayName("plano de ensino: professor nao altera nem encerra plano de outro")
    void professorNaoAlteraPlanoEnsinoAlheio() {
        autenticarProfessorComVinculos(VINCULO_MEU);
        given(planoEnsinoRepository.findById(PLANO_ENSINO_ID))
                .willReturn(Optional.of(planoEnsino(PLANO_ENSINO_ID, VINCULO_ALHEIO)));

        assertForbidden(() -> planoEnsinoService.atualizar(PLANO_ENSINO_ID, planoEnsino(null, VINCULO_ALHEIO)));
        assertForbidden(() -> planoEnsinoService.deletar(PLANO_ENSINO_ID));

        verify(planoEnsinoRepository, never()).save(any());
    }

    @Test
    @DisplayName("plano de aula: professor nao altera plano de aula de outro")
    void professorNaoAlteraPlanoAulaAlheio() {
        autenticarProfessorComVinculos(VINCULO_MEU);
        given(planoAulaRepository.findById(PLANO_AULA_ID))
                .willReturn(Optional.of(planoAula(PLANO_AULA_ID, VINCULO_ALHEIO)));

        assertForbidden(() -> planoAulaService.atualizar(PLANO_AULA_ID,
                planoAula(null, VINCULO_ALHEIO)));
        assertForbidden(() -> planoAulaService.deletar(PLANO_AULA_ID));

        verify(planoAulaRepository, never()).save(any());
    }

    @Test
    @DisplayName("conteudo: professor nao inativa conteudo de outro planejamento")
    void professorNaoInativaConteudoAlheio() {
        autenticarProfessorComVinculos(VINCULO_MEU);
        given(conteudoPlanoRepository.findById(CONTEUDO_ID))
                .willReturn(Optional.of(conteudo(CONTEUDO_ID, VINCULO_ALHEIO)));

        assertForbidden(() -> conteudoPlanoService.deletar(CONTEUDO_ID));

        verify(conteudoPlanoRepository, never()).save(any());
    }

    @Test
    @DisplayName("aula: professor nao publica nem inativa aula de outro")
    void professorNaoPublicaAulaAlheia() {
        autenticarProfessorComVinculos(VINCULO_MEU);
        given(aulaRepository.findById(AULA_ID)).willReturn(Optional.of(aula(AULA_ID, VINCULO_ALHEIO)));

        assertForbidden(() -> aulaService.publicar(AULA_ID, null));
        assertForbidden(() -> aulaService.deletar(AULA_ID));

        // Efeito colateral nenhum: nem auditoria, nem gravação da aula.
        verify(aulaRepository, never()).save(any());
    }

    @Test
    @DisplayName("aula: professor nao cria aula em plano de aula de outro")
    void professorNaoCriaAulaEmPlanoAlheio() {
        autenticarProfessorComVinculos(VINCULO_MEU);
        given(planoAulaRepository.findById(PLANO_AULA_ID))
                .willReturn(Optional.of(planoAula(PLANO_AULA_ID, VINCULO_ALHEIO)));

        Aula nova = new Aula();
        nova.setPlanoAula(planoAula(PLANO_AULA_ID, null));

        assertForbidden(() -> aulaService.salvar(nova));

        verify(aulaRepository, never()).save(any());
    }

    @Test
    @DisplayName("aula: professor nao gera aulas em lote no plano de outro")
    void professorNaoGeraLoteEmPlanoAlheio() {
        autenticarProfessorComVinculos(VINCULO_MEU);
        given(planoAulaRepository.findById(PLANO_AULA_ID))
                .willReturn(Optional.of(planoAula(PLANO_AULA_ID, VINCULO_ALHEIO)));

        assertForbidden(() -> aulaService.gerarLote(PLANO_AULA_ID, new studojurata_api.dto.GerarAulasLoteRequest()));

        verify(aulaRepository, never()).save(any());
    }

    @Test
    @DisplayName("vinculo turma-disciplina: professor nao vincula em turma alheia nem desvincula vinculo alheio")
    void professorNaoMexeEmVinculoAlheio() {
        autenticarProfessorComVinculos(VINCULO_MEU);
        given(escopoProfessor.turmaIdsDoProfessor(PROFESSOR_ID)).willReturn(Set.of(TURMA_ID));
        given(turmaDisciplinaRepository.findById(VINCULO_ALHEIO))
                .willReturn(Optional.of(vinculo(VINCULO_ALHEIO)));

        TurmaDisciplina novo = new TurmaDisciplina();
        Turma turmaAlheia = new Turma();
        turmaAlheia.setId(999L);
        novo.setTurma(turmaAlheia);

        assertForbidden(() -> turmaDisciplinaService.salvar(novo));
        assertForbidden(() -> turmaDisciplinaService.deletar(VINCULO_ALHEIO));

        verify(turmaDisciplinaRepository, never()).save(any());
    }

    @Test
    @DisplayName("caminho indireto: professor nao vincula conteudo em aula de outro")
    void professorNaoVinculaConteudoEmAulaAlheia() {
        autenticarProfessorComVinculos(VINCULO_MEU);
        given(aulaRepository.findById(AULA_ID)).willReturn(Optional.of(aula(AULA_ID, VINCULO_ALHEIO)));

        assertForbidden(() -> aulaConteudoService.vincular(AULA_ID, CONTEUDO_ID));
        assertForbidden(() -> aulaConteudoService.desvincular(AULA_ID, CONTEUDO_ID));

        verifyNoInteractions(aulaConteudoRepository);
    }

    @Test
    @DisplayName("caminho indireto: professor nao registra chamada em aula de outro")
    void professorNaoRegistraChamadaEmAulaAlheia() {
        autenticarProfessorComVinculos(VINCULO_MEU);
        given(aulaRepository.findById(AULA_ID)).willReturn(Optional.of(aula(AULA_ID, VINCULO_ALHEIO)));

        ChamadaRequest request = new ChamadaRequest();
        ChamadaRequest.Item item = new ChamadaRequest.Item();
        item.setAlunoId(ALUNO_ID);
        item.setPresente(true);
        request.setAlunos(List.of(item));

        assertForbidden(() -> frequenciaService.registrarChamada(AULA_ID, request));

        verifyNoInteractions(frequenciaRepository);
    }

    @Test
    @DisplayName("aluno nao escreve planejamento (403 antes de qualquer efeito)")
    void alunoNaoEscrevePlanejamento() {
        AuthorizationTestSupport.autenticarComoAluno(ALUNO_ID);
        given(planoEnsinoRepository.findById(PLANO_ENSINO_ID))
                .willReturn(Optional.of(planoEnsino(PLANO_ENSINO_ID, VINCULO_MEU)));
        given(aulaRepository.findById(AULA_ID)).willReturn(Optional.of(aula(AULA_ID, VINCULO_MEU)));

        assertForbidden(() -> planoEnsinoService.deletar(PLANO_ENSINO_ID));
        assertForbidden(() -> aulaService.publicar(AULA_ID, null));

        verify(planoEnsinoRepository, never()).save(any());
        verify(aulaRepository, never()).save(any());
    }

    @Test
    @DisplayName("professor nao escreve em recurso sem vinculo (D-C2 e do administrador)")
    void professorNaoEscreveEmRecursoSemVinculo() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(planoEnsinoRepository.findById(PLANO_ENSINO_ID))
                .willReturn(Optional.of(planoEnsino(PLANO_ENSINO_ID, null)));

        assertForbidden(() -> planoEnsinoService.deletar(PLANO_ENSINO_ID));

        verify(planoEnsinoRepository, never()).save(any());
        verifyNoInteractions(escopoUsuario);
    }

    // --- catálogo por escola -------------------------------------------------

    @Test
    @DisplayName("curso e disciplina: leitura individual de outra escola e negada")
    void leituraDeCatalogoDeOutraEscolaEhNegada() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(escolaContext.escolaAtualId()).willReturn(ESCOLA_ID);
        given(cursoRepository.findById(1L)).willReturn(Optional.of(curso(1L, 99L)));
        given(disciplinaRepository.findById(2L)).willReturn(Optional.of(disciplina(2L, 99L)));

        assertForbidden(() -> cursoService.buscarParaLeitura(1L));
        assertForbidden(() -> disciplinaService.buscarParaLeitura(2L));
    }

    @Test
    @DisplayName("curso e disciplina: mesma escola continua acessivel")
    void leituraDeCatalogoDaMesmaEscolaEhPermitida() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(escolaContext.escolaAtualId()).willReturn(ESCOLA_ID);
        given(cursoRepository.findById(1L)).willReturn(Optional.of(curso(1L, ESCOLA_ID)));
        given(disciplinaRepository.findById(2L)).willReturn(Optional.of(disciplina(2L, ESCOLA_ID)));

        assertThat(cursoService.buscarParaLeitura(1L).getId()).isEqualTo(1L);
        assertThat(disciplinaService.buscarParaLeitura(2L).getId()).isEqualTo(2L);
    }

    // --- helpers -----------------------------------------------------------

    private void autenticarProfessorComVinculos(Long... vinculoIds) {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(escopoUsuario.turmaDisciplinaIds()).willReturn(Set.of(vinculoIds));
    }

    private void assertForbidden(Runnable acao) {
        try {
            acao.run();
        } catch (ResponseStatusException ex) {
            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            return;
        }
        fail("esperava ResponseStatusException 403, mas a chamada foi permitida");
    }

    private static PlanoEnsino planoEnsino(Long id, Long vinculoId) {
        PlanoEnsino plano = new PlanoEnsino();
        plano.setId(id);
        plano.setCurso(curso(1L, ESCOLA_ID));
        if (vinculoId != null) {
            plano.setTurmaDisciplina(vinculo(vinculoId));
        }
        return plano;
    }

    private static PlanoAula planoAula(Long id, Long vinculoId) {
        PlanoAula plano = new PlanoAula();
        plano.setId(id);
        plano.setStatus(StatusPlano.ATIVO);
        if (vinculoId != null) {
            plano.setTurmaDisciplina(vinculo(vinculoId));
        }
        return plano;
    }

    private static ConteudoPlano conteudo(Long id, Long vinculoId) {
        ConteudoPlano conteudo = new ConteudoPlano();
        conteudo.setId(id);
        conteudo.setTitulo("Conteúdo " + id);
        if (vinculoId != null) {
            PlanoEnsino plano = new PlanoEnsino();
            plano.setId(PLANO_ENSINO_ID);
            plano.setTurmaDisciplina(vinculo(vinculoId));
            conteudo.setPlanoEnsino(plano);
        }
        return conteudo;
    }

    private static Aula aula(Long id, Long vinculoId) {
        Aula aula = new Aula();
        aula.setId(id);
        aula.setTitulo("Aula " + id);
        aula.setPlanoAula(planoAula(PLANO_AULA_ID, vinculoId));
        return aula;
    }

    private static TurmaDisciplina vinculo(Long id) {
        TurmaDisciplina vinculo = new TurmaDisciplina();
        vinculo.setId(id);
        Turma turma = new Turma();
        turma.setId(TURMA_ID);
        vinculo.setTurma(turma);
        return vinculo;
    }

    private static Curso curso(Long id, Long escolaId) {
        Curso curso = new Curso();
        curso.setId(id);
        curso.setNome("Curso " + id);
        Escola escola = new Escola();
        escola.setId(escolaId);
        curso.setEscola(escola);
        return curso;
    }

    private static Disciplina disciplina(Long id, Long escolaId) {
        Disciplina disciplina = new Disciplina();
        disciplina.setId(id);
        disciplina.setTitulo("Disciplina " + id);
        Escola escola = new Escola();
        escola.setId(escolaId);
        disciplina.setEscola(escola);
        return disciplina;
    }

    /** Auditoria não é o alvo deste teste; o dublê evita depender do repositório. */
    private static class AuditLogServiceStub extends AuditLogService {
        AuditLogServiceStub() {
            super(null);
        }

        @Override
        public void registrar(String entidade, Long entidadeId, studojurata_api.model.enums.AcaoAuditoria acao, String detalhes) {
            // sem efeito
        }
    }
}
