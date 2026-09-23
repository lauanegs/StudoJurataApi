package studojurata_api.machinelearning.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import studojurata_api.machinelearning.dto.AtributosRecomendacao;
import studojurata_api.machinelearning.model.enums.NecessidadeRevisao;

/** Estratégia determinística de repetição espaçada (fallback do modelo). */
class RepeticaoEspacadaServiceTest {

    private final RepeticaoEspacadaService service = new RepeticaoEspacadaService();

    @Test
    @DisplayName("aluno sem histórico começa em 'revisar em breve'")
    void semHistorico() {
        AtributosRecomendacao atributos = atributos(null, null, 0, null, null);

        assertThat(service.classificar(atributos)).isEqualTo(NecessidadeRevisao.REVISAR_EM_BREVE);
        assertThat(atributos.temHistorico()).isFalse();
    }

    @Test
    @DisplayName("baixo desempenho no conteúdo pede revisão agora")
    void baixoDesempenho() {
        assertThat(service.classificar(atributos(0.4, 0.4, 5, 10, 2))).isEqualTo(NecessidadeRevisao.REVISAR_AGORA);
        assertThat(service.classificar(atributos(0.4, 0.4, 5, 10, 2)).toString()).isEqualTo("REVISAR_AGORA");
    }

    @Test
    @DisplayName("desempenho recente caindo também pede revisão agora")
    void desempenhoRecenteCaindo() {
        assertThat(service.classificar(atributos(0.9, 0.3, 5, 10, 2))).isEqualTo(NecessidadeRevisao.REVISAR_AGORA);
    }

    @Test
    @DisplayName("muito tempo sem contato com o conteúdo pede revisão agora")
    void esquecimento() {
        assertThat(service.classificar(atributos(0.95, 0.95, 40, 10, 2))).isEqualTo(NecessidadeRevisao.REVISAR_AGORA);
    }

    @Test
    @DisplayName("duas semanas sem contato pede revisão em breve, mesmo com bom desempenho")
    void atencaoPorTempo() {
        assertThat(service.classificar(atributos(0.95, 0.95, 15, 10, 2))).isEqualTo(NecessidadeRevisao.REVISAR_EM_BREVE);
    }

    @Test
    @DisplayName("desempenho confortável e recente é domínio estável")
    void dominioEstavel() {
        assertThat(service.classificar(atributos(0.9, 0.85, 3, 12, 2))).isEqualTo(NecessidadeRevisao.DOMINIO_ESTAVEL);
    }

    @Test
    @DisplayName("intervalo cresce com revisões e desempenho, dentro dos limites")
    void intervalo() {
        int agora = service.intervaloDias(atributos(0.3, 0.3, 1, 10, 0), NecessidadeRevisao.REVISAR_AGORA);
        int estavel = service.intervaloDias(atributos(0.9, 0.9, 1, 10, 0), NecessidadeRevisao.DOMINIO_ESTAVEL);
        int comRevisoes = service.intervaloDias(atributos(0.9, 0.9, 1, 10, 3), NecessidadeRevisao.DOMINIO_ESTAVEL);

        assertThat(agora).isEqualTo(1);
        assertThat(estavel).isGreaterThan(agora);
        assertThat(comRevisoes).isGreaterThan(estavel);
        assertThat(comRevisoes).isLessThanOrEqualTo(RepeticaoEspacadaService.INTERVALO_MAXIMO_DIAS);
    }

    @Test
    @DisplayName("revisão recente com bom desempenho não é tratada como atraso")
    void revisaoRecenteNaoEhAtraso() {
        // 20 dias desde a última resposta já passaria do limiar de atenção, mas o
        // ciclo foi reforçado há 2 dias com desempenho bom.
        AtributosRecomendacao revisadoRecentemente = new AtributosRecomendacao(
                1L, 2L, 3L, 0.9, 0.9, 0.9, 2, 2, 20, 2, 2.0, 12);
        assertThat(service.classificar(revisadoRecentemente)).isEqualTo(NecessidadeRevisao.DOMINIO_ESTAVEL);

        // Sem o reforço recente (última revisão há 40 dias), volta a pedir revisão em breve.
        AtributosRecomendacao semReforcoRecente = new AtributosRecomendacao(
                1L, 2L, 3L, 0.9, 0.9, 0.9, 2, 2, 20, 40, 2.0, 12);
        assertThat(service.classificar(semReforcoRecente)).isEqualTo(NecessidadeRevisao.REVISAR_EM_BREVE);
    }

    @Test
    @DisplayName("intervalo nunca é negativo nem zero, em nenhuma necessidade")
    void intervaloValidoEmTodosOsCasos() {
        for (NecessidadeRevisao necessidade : NecessidadeRevisao.values()) {
            int dias = service.intervaloDias(atributos(null, null, null, 0, 0), necessidade);

            assertThat(dias).isGreaterThanOrEqualTo(1);
            assertThat(dias).isLessThanOrEqualTo(RepeticaoEspacadaService.INTERVALO_MAXIMO_DIAS);
        }
    }

    @Test
    @DisplayName("quantidade nunca passa de 10 e é maior quando a revisão é urgente")
    void quantidade() {
        assertThat(service.quantidadeQuestoes(NecessidadeRevisao.REVISAR_AGORA)).isEqualTo(10);
        assertThat(service.quantidadeQuestoes(NecessidadeRevisao.REVISAR_EM_BREVE)).isEqualTo(6);
        assertThat(service.quantidadeQuestoes(NecessidadeRevisao.DOMINIO_ESTAVEL)).isEqualTo(3);
        assertThat(service.quantidadeQuestoes(NecessidadeRevisao.REVISAR_AGORA))
                .isLessThanOrEqualTo(SelecaoQuestoesService.MAXIMO_QUESTOES);
    }

    private static AtributosRecomendacao atributos(
            Double conteudo, Double recente, Integer diasUltimaResposta, Integer respostas, Integer revisoes) {
        return new AtributosRecomendacao(1L, 2L, 3L, conteudo, recente, conteudo, 1, revisoes, diasUltimaResposta,
                null, 2.0, respostas);
    }
}
