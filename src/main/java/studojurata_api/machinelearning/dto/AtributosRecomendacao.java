package studojurata_api.machinelearning.dto;

public record AtributosRecomendacao(
        Long alunoId,
        Long conteudoPlanoId,
        Long disciplinaId,
        Double percentualAcertoConteudo,
        Double percentualAcertoRecente,
        Double percentualAcertoDisciplina,
        Integer quantidadeTentativas,
        Integer quantidadeRevisoes,
        Integer diasDesdeUltimaResposta,
        Integer diasDesdeUltimaRevisao,
        Double dificuldadeMediaRespondida,
        Integer quantidadeQuestoesRespondidas) {
    public static final double LIMIAR_BAIXO_DESEMPENHO = 0.6;
    public static final double LIMIAR_BOM_DESEMPENHO = 0.8;
    public static final int DIAS_ESQUECIMENTO = 30;
    public static final int DIAS_ATENCAO = 14;

    public boolean temHistorico() {
        return quantidadeQuestoesRespondidas != null && quantidadeQuestoesRespondidas > 0;
    }

    public boolean baixoDesempenho() {
        if (abaixoDoLimiar(percentualAcertoConteudo)) {
            return true;
        }
        return abaixoDoLimiar(percentualAcertoRecente);
    }

    public boolean revisaoPorEsquecimento() {
        return diasDesdeUltimaResposta != null && diasDesdeUltimaResposta >= DIAS_ESQUECIMENTO;
    }

    public boolean atencaoPorTempo() {
        return diasDesdeUltimaResposta != null && diasDesdeUltimaResposta >= DIAS_ATENCAO;
    }

    public boolean desempenhoBom() {
        return percentualAcertoConteudo != null && percentualAcertoConteudo >= LIMIAR_BOM_DESEMPENHO;
    }

    private static boolean abaixoDoLimiar(Double percentual) {
        return percentual != null && percentual < LIMIAR_BAIXO_DESEMPENHO;
    }
}
