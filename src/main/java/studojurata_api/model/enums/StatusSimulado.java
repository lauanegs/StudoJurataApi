package studojurata_api.model.enums;

/**
 * Ciclo de vida de um {@link studojurata_api.model.Simulado}.
 *
 * RASCUNHO  -> em montagem pelo professor (ou aguardando revisão humana, no
 *              caso de simulados propostos pela IA — fora do escopo deste
 *              módulo). Ainda não gera nenhum SimuladoAluno.
 * PUBLICADO -> lançado (ver item 1.3 da Análise Crítica): neste momento são
 *              criados os registros SimuladoAluno (status PENDENTE) para
 *              todos os alunos elegíveis.
 * ENCERRADO -> fora da janela de realização (dataFim atingida ou encerrado
 *              manualmente pelo professor); não aceita novas tentativas.
 */
public enum StatusSimulado {
    RASCUNHO,
    PUBLICADO,
    ENCERRADO
}
