package studojurata_api.model.enums;

/**
 * Status próprio de PlanoEnsino/PlanoAula — distinto de StatusAtivoInativo
 * (compartilhado por ~15 outras entidades) porque aqui "inativo" nunca fez
 * sentido pedagogicamente: um plano não é desligado, ele é CONCLUIDO quando
 * o ciclo termina, e a turma pode receber um novo plano em seguida
 * (matrícula cíclica — ver Nota.java).
 */
public enum StatusPlano {
    ATIVO,
    CONCLUIDO
}
