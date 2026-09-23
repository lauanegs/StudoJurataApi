package studojurata_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import studojurata_api.ia.model.SimuladoGeradoIA;
import studojurata_api.ia.repository.RevisaoConteudoRepository;
import studojurata_api.ia.repository.SimuladoGeradoIARepository;
import studojurata_api.ia.service.GeracaoQuestaoIAService;
import studojurata_api.ia.service.GeracaoSimuladoIAService;
import studojurata_api.machinelearning.service.MachineLearningRecomendacaoService;
import studojurata_api.model.Simulado;
import studojurata_api.repository.AlunoRepository;
import studojurata_api.repository.ConteudoPlanoRepository;
import studojurata_api.repository.SimuladoQuestaoRepository;
import studojurata_api.repository.SimuladoRepository;
import studojurata_api.security.UsuarioAutenticado;
import studojurata_api.support.AuthorizationTestSupport;

/**
 * Escopo da listagem de simulados gerados pela IA ({@code GET /ia/geracao/simulado}).
 *
 * <p>A rota era global: qualquer professor recebia o vínculo (aluno, conteúdo e
 * prazo) de toda a escola, e a tela filtrava depois. Agora a lista reusa o
 * escopo de {@link SimuladoService#simuladoIdsVisiveis()} — o mesmo da listagem
 * de simulados —, então vínculo de simulado fora das turmas do professor (ou de
 * simulado órfão) não sai do servidor.
 */
@ExtendWith(MockitoExtension.class)
class C2GeracaoIAListagemEscopoTest {

    private static final long PROFESSOR_ID = 42L;
    private static final long SIMULADO_ID = 300L;
    private static final long OUTRO_SIMULADO_ID = 301L;
    private static final long ORFAO_ID = 900L;

    @Mock private SimuladoRepository simuladoRepository;
    @Mock private SimuladoQuestaoRepository simuladoQuestaoRepository;
    @Mock private ConteudoPlanoRepository conteudoPlanoRepository;
    @Mock private AlunoRepository alunoRepository;
    @Mock private GeracaoQuestaoIAService geracaoQuestaoIAService;
    @Mock private SimuladoGeradoIARepository simuladoGeradoIARepository;
    @Mock private RevisaoConteudoRepository revisaoConteudoRepository;
    @Mock private MachineLearningRecomendacaoService machineLearningRecomendacaoService;
    @Mock private SimuladoService simuladoService;
    @Mock private studojurata_api.security.SimuladoAccessGuard simuladoAccessGuard;
    @Mock private studojurata_api.repository.AulaConteudoRepository aulaConteudoRepository;

    private GeracaoSimuladoIAService service;

    @BeforeEach
    void setUp() {
        service = new GeracaoSimuladoIAService(simuladoRepository, simuladoQuestaoRepository, conteudoPlanoRepository,
                alunoRepository, geracaoQuestaoIAService, simuladoGeradoIARepository, revisaoConteudoRepository,
                machineLearningRecomendacaoService, simuladoService, new UsuarioAutenticado(), simuladoAccessGuard,
                aulaConteudoRepository);
    }

    @AfterEach
    void limparContexto() {
        AuthorizationTestSupport.limparContexto();
    }

    @Test
    @DisplayName("administrador recebe todos os vínculos de IA, sem consultar escopo")
    void administradorRecebeTodosOsVinculos() {
        AuthorizationTestSupport.autenticarComoAdministrador();
        given(simuladoGeradoIARepository.findAll())
                .willReturn(List.of(vinculo(1L, SIMULADO_ID), vinculo(2L, ORFAO_ID)));

        assertThat(service.listarGerados()).hasSize(2);

        verifyNoInteractions(simuladoService);
    }

    @Test
    @DisplayName("professor recebe apenas vínculos de simulados das suas turmas")
    void professorRecebeApenasDoEscopo() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(simuladoService.simuladoIdsVisiveis()).willReturn(Set.of(SIMULADO_ID));
        given(simuladoGeradoIARepository.findBySimulado_IdIn(Set.of(SIMULADO_ID)))
                .willReturn(List.of(vinculo(1L, SIMULADO_ID)));

        assertThat(service.listarGerados()).extracting(SimuladoGeradoIA::getId).containsExactly(1L);

        // A consulta é feita só com o id do escopo: vínculo de outro simulado
        // (ou de órfão) nunca é buscado, e não há varredura global.
        verify(simuladoGeradoIARepository).findBySimulado_IdIn(Set.of(SIMULADO_ID));
        verify(simuladoGeradoIARepository, never()).findAll();
    }

    @Test
    @DisplayName("professor sem turmas não recebe vínculo nenhum e não consulta repositório")
    void professorSemTurmasNaoRecebeVinculos() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        given(simuladoService.simuladoIdsVisiveis()).willReturn(Set.of());

        assertThat(service.listarGerados()).isEmpty();

        verifyNoInteractions(simuladoGeradoIARepository);
    }

    @Test
    @DisplayName("vínculo de simulado órfão fica fora do escopo do professor")
    void vinculoDeOrfaoNaoEntraNoEscopo() {
        AuthorizationTestSupport.autenticarComoProfessor(PROFESSOR_ID);
        // Escopo do professor (SimuladoService) já exclui órfãos; a listagem da IA
        // só pode pedir os ids devolvidos por ele.
        given(simuladoService.simuladoIdsVisiveis()).willReturn(Set.of(OUTRO_SIMULADO_ID));
        given(simuladoGeradoIARepository.findBySimulado_IdIn(Set.of(OUTRO_SIMULADO_ID)))
                .willReturn(List.of(vinculo(7L, OUTRO_SIMULADO_ID)));

        assertThat(service.listarGerados()).extracting(SimuladoGeradoIA::getId).containsExactly(7L);

        // A busca usa exatamente o escopo devolvido por SimuladoService — que já
        // exclui o órfão — e não varre a tabela inteira.
        verify(simuladoGeradoIARepository).findBySimulado_IdIn(Set.of(OUTRO_SIMULADO_ID));
        verify(simuladoGeradoIARepository, never()).findAll();
    }

    private static SimuladoGeradoIA vinculo(long id, long simuladoId) {
        Simulado simulado = new Simulado();
        simulado.setId(simuladoId);

        SimuladoGeradoIA vinculo = new SimuladoGeradoIA();
        vinculo.setId(id);
        vinculo.setSimulado(simulado);
        return vinculo;
    }
}
