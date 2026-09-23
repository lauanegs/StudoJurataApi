package studojurata_api.machinelearning.dto;

/**
 * Retrato dos atributos usados na recomendação, montado só com dados que o
 * sistema realmente guarda (respostas em QuestaoAluno/SimuladoAluno, agenda em
 * RevisaoConteudo, dificuldade em Questao.nivelDificuldade).
 *
 * <p>Campo nulo significa "sem dado" — nunca é preenchido com estimativa
 * inventada. Os indicadores derivados (baixo desempenho / revisão necessária)
 * são calculados a partir dos mesmos atributos, para não existir um segundo
 * critério escondido.
 *
 * <p>Percentuais são fração de 0 a 1; dificuldade média usa 1 = FÁCIL,
 * 2 = MÉDIA, 3 = DIFÍCIL.
 */
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

    /** Mesmo limiar do sinal de baixo aproveitamento já usado em RecomendacaoService. */
    public static final double LIMIAR_BAIXO_DESEMPENHO = 0.6;
    /** Acima disso o conteúdo está confortável — mas ainda pode haver revisão devida. */
    public static final double LIMIAR_BOM_DESEMPENHO = 0.8;
    /** Dias sem contato em que o esquecimento já é motivo suficiente para revisar. */
    public static final int DIAS_ESQUECIMENTO = 30;
    /** Dias sem contato em que a revisão passa a ser recomendada. */
    public static final int DIAS_ATENCAO = 14;

    public boolean temHistorico() {
        return quantidadeQuestoesRespondidas != null && quantidadeQuestoesRespondidas > 0;
    }

    /** Indicador de baixo desempenho: conteúdo abaixo do limiar ou acerto recente caindo. */
    public boolean baixoDesempenho() {
        if (abaixoDoLimiar(percentualAcertoConteudo)) {
            return true;
        }
        return abaixoDoLimiar(percentualAcertoRecente);
    }

    /** Indicador de revisão necessária: muito tempo sem contato com o conteúdo. */
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
