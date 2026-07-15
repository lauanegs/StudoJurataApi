package studojurata_api.model.enums;

/**
 * Status da tentativa de um aluno em um simulado.
 *
 * Ver item 1.3 da Análise Crítica: o registro SimuladoAluno é criado no
 * momento do lançamento do simulado, para todos os elegíveis, com status
 * PENDENTE. Quando o aluno finaliza (por conclusão normal ou por esgotamento
 * do tempo — ver item 4.2), o status muda para CONCLUIDO e passam a valer
 * nota, quantidadeAcertos e tempoGasto.
 */
public enum StatusSimuladoAluno {
    PENDENTE,
    CONCLUIDO
}
