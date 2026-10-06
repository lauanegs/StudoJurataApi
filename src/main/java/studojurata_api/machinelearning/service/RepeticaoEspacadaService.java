package studojurata_api.machinelearning.service;

import org.springframework.stereotype.Service;

import studojurata_api.machinelearning.dto.AtributosRecomendacao;
import studojurata_api.machinelearning.model.enums.NecessidadeRevisao;

/** Estratégia determinística de repetição espaçada */
@Service
public class RepeticaoEspacadaService {
    /** Maior intervalo que faz sentido agendar nesta primeira versão */
    static final int INTERVALO_MAXIMO_DIAS = 90;
    /** Janela em que uma revisão recente conta como "acabou de ser reforçado" */
    static final int DIAS_REVISAO_RECENTE = 7;

    /** Classificação determinística da necessidade de revisão */
    public NecessidadeRevisao classificar(AtributosRecomendacao atributos) {
        if (!atributos.temHistorico()) {
            return NecessidadeRevisao.REVISAR_EM_BREVE;
        }
        if (atributos.baixoDesempenho() || atributos.revisaoPorEsquecimento()) {
            return NecessidadeRevisao.REVISAR_AGORA;
        }
        if (revisouRecentemente(atributos)) {
            return NecessidadeRevisao.DOMINIO_ESTAVEL;
        }
        if (atributos.atencaoPorTempo() || !atributos.desempenhoBom()) {
            return NecessidadeRevisao.REVISAR_EM_BREVE;
        }
        return NecessidadeRevisao.DOMINIO_ESTAVEL;
    }

    /** Reforço recente com desempenho bom: nada a fazer agora, mesmo perto do limiar de atenção */
    private static boolean revisouRecentemente(AtributosRecomendacao atributos) {
        return atributos.diasDesdeUltimaRevisao() != null
                && atributos.diasDesdeUltimaRevisao() <= DIAS_REVISAO_RECENTE
                && atributos.desempenhoBom();
    }

    /** Intervalo até a próxima revisão, em dias */
    public int intervaloDias(AtributosRecomendacao atributos, NecessidadeRevisao necessidade) {
        int base = switch (necessidade) {
            case REVISAR_AGORA -> 1;
            case REVISAR_EM_BREVE -> 7;
            case DOMINIO_ESTAVEL -> 21;
        };

        int revisoes = atributos.quantidadeRevisoes() != null ? atributos.quantidadeRevisoes() : 0;
        double fatorRevisoes = 1 + Math.min(revisoes, 3) * 0.5;

        double percentual = atributos.percentualAcertoConteudo() != null
                ? atributos.percentualAcertoConteudo()
                : AtributosRecomendacao.LIMIAR_BAIXO_DESEMPENHO;
        double fatorDesempenho = 0.5 + percentual;

        long dias = Math.round(base * fatorRevisoes * fatorDesempenho);
        return (int) Math.max(1, Math.min(INTERVALO_MAXIMO_DIAS, dias));
    }

    /** Quantidade de questões do simulado individual, sempre dentro do limite pedagógico de 10 */
    public int quantidadeQuestoes(NecessidadeRevisao necessidade) {
        return switch (necessidade) {
            case REVISAR_AGORA -> 10;
            case REVISAR_EM_BREVE -> 6;
            case DOMINIO_ESTAVEL -> 3;
        };
    }
}
