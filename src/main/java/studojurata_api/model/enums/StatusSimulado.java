package studojurata_api.model.enums;

/**
 * RASCUNHO  -> em montagem pelo professor ou aguardando revisão das questões
 *              geradas pela IA. Ainda não gera nenhum SimuladoAluno.
 * PUBLICADO -> lançado: são criadas as tentativas PENDENTE dos alunos elegíveis.
 * ENCERRADO -> fora da janela de realização (dataFim atingida ou encerrado
 *              manualmente pelo professor); não aceita novas tentativas.
 */
public enum StatusSimulado {
    RASCUNHO,
    PUBLICADO,
    ENCERRADO
}
