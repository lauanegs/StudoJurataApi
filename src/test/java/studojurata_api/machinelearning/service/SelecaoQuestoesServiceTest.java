package studojurata_api.machinelearning.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import studojurata_api.model.Alternativa;
import studojurata_api.model.ConteudoPlano;
import studojurata_api.model.Disciplina;
import studojurata_api.model.Questao;
import studojurata_api.model.QuestaoConteudo;
import studojurata_api.model.enums.NivelDificuldade;
import studojurata_api.model.enums.StatusAtivoInativo;
import studojurata_api.model.enums.StatusQuestao;
import studojurata_api.model.enums.TipoQuestao;
import studojurata_api.repository.AlternativaRepository;
import studojurata_api.repository.QuestaoConteudoRepository;
import studojurata_api.repository.QuestaoRepository;

/** Seleção de questões do banco: disciplina, conteúdo, dificuldade, repetição e limites. */
@ExtendWith(MockitoExtension.class)
class SelecaoQuestoesServiceTest {

    private static final long DISC = 10L;
    private static final long OUTRA_DISC = 20L;
    private static final long CONTEUDO = 100L;
    private static final long OUTRO_CONTEUDO = 200L;

    @Mock private QuestaoRepository questaoRepository;
    @Mock private QuestaoConteudoRepository questaoConteudoRepository;
    @Mock private AlternativaRepository alternativaRepository;

    private SelecaoQuestoesService service;

    @BeforeEach
    void setUp() {
        service = new SelecaoQuestoesService(questaoRepository, questaoConteudoRepository, alternativaRepository);
    }

    @Test
    @DisplayName("banco com questões suficientes: cobertura e dificuldade atendidas")
    void bancoSuficiente() {
        dadoBanco(
                List.of(questao(1L, DISC, NivelDificuldade.FACIL), questao(2L, DISC, NivelDificuldade.MEDIA),
                        questao(3L, DISC, NivelDificuldade.DIFICIL)),
                List.of(vinculo(1L, CONTEUDO), vinculo(2L, CONTEUDO), vinculo(3L, CONTEUDO)),
                Map.of(1L, 3, 2L, 3, 3L, 3));

        SelecaoQuestoesService.Resultado resultado = service.selecionar(
                entrada(3, distribuicao(1, 1, 1), Set.of()));

        assertThat(resultado.selecionadas()).hasSize(3);
        assertThat(resultado.faltantes()).isEmpty();
        assertThat(resultado.repetidas()).isZero();
        assertThat(resultado.bancoSuficiente()).isTrue();
    }

    @Test
    @DisplayName("nunca passa de 10 questões, mesmo pedindo mais")
    void limiteDeDezQuestoes() {
        List<Questao> questoes = new ArrayList<>();
        List<QuestaoConteudo> vinculos = new ArrayList<>();
        Map<Long, Integer> alternativas = new LinkedHashMap<>();
        for (long id = 1; id <= 15; id++) {
            questoes.add(questao(id, DISC, NivelDificuldade.MEDIA));
            vinculos.add(vinculo(id, CONTEUDO));
            alternativas.put(id, 3);
        }
        dadoBanco(questoes, vinculos, alternativas);

        SelecaoQuestoesService.Resultado resultado = service.selecionar(
                entrada(15, distribuicao(0, 15, 0), Set.of()));

        assertThat(resultado.selecionadas()).hasSize(SelecaoQuestoesService.MAXIMO_QUESTOES);
    }

    @Test
    @DisplayName("questão com mais de 3 alternativas fica fora")
    void maximoDeTresAlternativas() {
        dadoBanco(
                List.of(questao(1L, DISC, NivelDificuldade.MEDIA), questao(2L, DISC, NivelDificuldade.MEDIA)),
                List.of(vinculo(1L, CONTEUDO), vinculo(2L, CONTEUDO)),
                Map.of(1L, 4, 2L, 3));

        SelecaoQuestoesService.Resultado resultado = service.selecionar(
                entrada(2, distribuicao(0, 2, 0), Set.of()));

        assertThat(resultado.selecionadas()).extracting(Questao::getId).containsExactly(2L);
        assertThat(resultado.descartadasPorAlternativas()).isEqualTo(1);
    }

    @Test
    @DisplayName("questão de outra disciplina nunca é selecionada")
    void disciplinaIncorreta() {
        // A consulta já é por disciplina; o filtro defensivo é o que este teste cobre.
        given(questaoRepository.findByDisciplina_IdIn(any()))
                .willReturn(List.of(questao(1L, OUTRA_DISC, NivelDificuldade.MEDIA)));

        SelecaoQuestoesService.Resultado resultado = service.selecionar(
                entrada(1, distribuicao(0, 1, 0), Set.of()));

        assertThat(resultado.selecionadas()).isEmpty();
        assertThat(resultado.bancoSuficiente()).isFalse();
    }

    @Test
    @DisplayName("só questão aprovada entra no simulado")
    void soQuestaoAprovada() {
        Questao pendente = questao(1L, DISC, NivelDificuldade.MEDIA);
        pendente.setStatus(StatusQuestao.PENDENTE);
        Questao rejeitada = questao(2L, DISC, NivelDificuldade.MEDIA);
        rejeitada.setStatus(StatusQuestao.REJEITADA);
        given(questaoRepository.findByDisciplina_IdIn(any())).willReturn(List.of(pendente, rejeitada));

        SelecaoQuestoesService.Resultado resultado = service.selecionar(
                entrada(2, distribuicao(0, 2, 0), Set.of()));

        assertThat(resultado.selecionadas()).isEmpty();
        assertThat(resultado.bancoSuficiente()).isFalse();
    }

    @Test
    @DisplayName("questão vinculada só a conteúdo inativado fica fora; conteúdo ativo mantém a questão")
    void conteudoInativado() {
        dadoBanco(
                List.of(questao(1L, DISC, NivelDificuldade.MEDIA), questao(2L, DISC, NivelDificuldade.MEDIA),
                        questao(3L, DISC, NivelDificuldade.MEDIA)),
                List.of(vinculo(1L, CONTEUDO, StatusAtivoInativo.INATIVO),
                        vinculo(2L, CONTEUDO, StatusAtivoInativo.ATIVO),
                        vinculo(3L, OUTRO_CONTEUDO, StatusAtivoInativo.INATIVO),
                        vinculo(3L, CONTEUDO, StatusAtivoInativo.ATIVO)),
                Map.of(1L, 3, 2L, 3, 3L, 3));

        SelecaoQuestoesService.Resultado resultado = service.selecionar(
                entrada(3, distribuicao(0, 3, 0), Set.of()));

        // 1 está só em conteúdo inativado; 3 está em conteúdo inativado e ativo (entra).
        assertThat(resultado.selecionadas()).extracting(Questao::getId).containsExactly(2L, 3L);
    }

    @Test
    @DisplayName("questão já respondida só entra se não houver inédita — e o fato é reportado")
    void evitaRepeticao() {
        dadoBanco(
                List.of(questao(1L, DISC, NivelDificuldade.MEDIA), questao(2L, DISC, NivelDificuldade.MEDIA)),
                List.of(vinculo(1L, CONTEUDO), vinculo(2L, CONTEUDO)),
                Map.of(1L, 3, 2L, 3));

        SelecaoQuestoesService.Resultado comInedita = service.selecionar(
                entrada(1, distribuicao(0, 1, 0), Set.of(1L)));
        assertThat(comInedita.selecionadas()).extracting(Questao::getId).containsExactly(2L);
        assertThat(comInedita.repetidas()).isZero();
        assertThat(comInedita.bancoSuficiente()).isTrue();

        SelecaoQuestoesService.Resultado soRepetida = service.selecionar(
                entrada(2, distribuicao(0, 2, 0), Set.of(1L, 2L)));
        assertThat(soRepetida.selecionadas()).hasSize(2);
        assertThat(soRepetida.repetidas()).isEqualTo(2);
        assertThat(soRepetida.bancoSuficiente()).isFalse();
    }

    @Test
    @DisplayName("dificuldade pedida sem banco fica registrada como faltante")
    void dificuldadeIndisponivel() {
        dadoBanco(
                List.of(questao(1L, DISC, NivelDificuldade.FACIL)),
                List.of(vinculo(1L, CONTEUDO)),
                Map.of(1L, 3));

        SelecaoQuestoesService.Resultado resultado = service.selecionar(
                entrada(1, distribuicao(0, 1, 0), Set.of()));

        // A questão de outro nível entra como candidata (serve de base), mas a
        // dificuldade pedida fica marcada como faltante — o que reprova o banco.
        assertThat(resultado.selecionadas()).hasSize(1);
        assertThat(resultado.faltantes()).containsEntry(NivelDificuldade.MEDIA, 1);
        assertThat(resultado.bancoSuficiente()).isFalse();
    }

    @Test
    @DisplayName("prioriza a questão do conteúdo recomendado")
    void priorizaConteudoRecomendado() {
        dadoBanco(
                List.of(questao(1L, DISC, NivelDificuldade.MEDIA), questao(2L, DISC, NivelDificuldade.MEDIA)),
                List.of(vinculo(1L, OUTRO_CONTEUDO), vinculo(2L, CONTEUDO)),
                Map.of(1L, 3, 2L, 3));

        SelecaoQuestoesService.Resultado resultado = service.selecionar(
                entrada(1, distribuicao(0, 1, 0), Set.of()));

        assertThat(resultado.selecionadas()).extracting(Questao::getId).containsExactly(2L);
    }

    @Test
    @DisplayName("disciplina sem questões devolve seleção vazia e banco insuficiente")
    void semQuestoes() {
        given(questaoRepository.findByDisciplina_IdIn(any())).willReturn(List.of());

        SelecaoQuestoesService.Resultado resultado = service.selecionar(entrada(3, distribuicao(1, 1, 1), Set.of()));

        assertThat(resultado.selecionadas()).isEmpty();
        assertThat(resultado.bancoSuficiente()).isFalse();
    }

    // --- helpers -----------------------------------------------------------

    private void dadoBanco(List<Questao> questoes, List<QuestaoConteudo> vinculos, Map<Long, Integer> alternativasPorQuestao) {
        List<Long> ids = questoes.stream().map(Questao::getId).toList();
        given(questaoRepository.findByDisciplina_IdIn(any())).willReturn(questoes);
        given(questaoConteudoRepository.findByQuestao_IdIn(ids)).willReturn(vinculos);
        given(alternativaRepository.findByQuestao_IdIn(ids)).willReturn(alternativas(alternativasPorQuestao));
    }

    private static SelecaoQuestoesService.Entrada entrada(
            int quantidade, Map<NivelDificuldade, Integer> distribuicao, Set<Long> respondidas) {
        return new SelecaoQuestoesService.Entrada(DISC, List.of(CONTEUDO), distribuicao, quantidade, respondidas);
    }

    private static Map<NivelDificuldade, Integer> distribuicao(int facil, int media, int dificil) {
        Map<NivelDificuldade, Integer> distribuicao = new LinkedHashMap<>();
        if (facil > 0) distribuicao.put(NivelDificuldade.FACIL, facil);
        if (media > 0) distribuicao.put(NivelDificuldade.MEDIA, media);
        if (dificil > 0) distribuicao.put(NivelDificuldade.DIFICIL, dificil);
        return distribuicao;
    }

    private static List<Alternativa> alternativas(Map<Long, Integer> quantidades) {
        List<Alternativa> alternativas = new ArrayList<>();
        quantidades.forEach((questaoId, quantidade) -> {
            for (int i = 0; i < quantidade; i++) {
                Alternativa alternativa = new Alternativa();
                alternativa.setQuestao(questao(questaoId, DISC, NivelDificuldade.MEDIA));
                alternativas.add(alternativa);
            }
        });
        return alternativas;
    }

    private static Questao questao(long id, long disciplinaId, NivelDificuldade nivel) {
        Questao questao = new Questao();
        questao.setId(id);
        questao.setTipo(TipoQuestao.ALTERNATIVAS);
        questao.setStatus(StatusQuestao.APROVADA);
        questao.setNivelDificuldade(nivel);

        Disciplina disciplina = new Disciplina();
        disciplina.setId(disciplinaId);
        questao.setDisciplina(disciplina);
        return questao;
    }

    private static QuestaoConteudo vinculo(long questaoId, long conteudoId) {
        return vinculo(questaoId, conteudoId, StatusAtivoInativo.ATIVO);
    }

    private static QuestaoConteudo vinculo(long questaoId, long conteudoId, StatusAtivoInativo status) {
        QuestaoConteudo vinculo = new QuestaoConteudo();
        vinculo.setQuestao(questao(questaoId, DISC, NivelDificuldade.MEDIA));

        ConteudoPlano conteudo = new ConteudoPlano();
        conteudo.setId(conteudoId);
        conteudo.setStatus(status);
        vinculo.setConteudoPlano(conteudo);
        return vinculo;
    }
}
