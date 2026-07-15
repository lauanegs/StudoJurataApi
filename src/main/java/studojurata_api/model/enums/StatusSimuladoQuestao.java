package studojurata_api.model.enums;

/**
 * Status do vínculo entre uma Questao e um Simulado.
 *
 * ATIVA    -> a questão compõe o simulado normalmente.
 * REMOVIDA -> a questão foi retirada do simulado (ex.: conteúdo removido do
 *             plano de ensino, ou correção de falha encontrada após alunos já
 *             terem respondido — ver Casos Extremos: "Conteudo removido" e
 *             "Plano de ensino alterado após simulados já realizados").
 *             Soft-delete: nunca removemos fisicamente, para preservar o
 *             histórico de tentativas (QuestaoAluno) já registradas contra
 *             esta questão.
 */
public enum StatusSimuladoQuestao {
    ATIVA,
    REMOVIDA
}
