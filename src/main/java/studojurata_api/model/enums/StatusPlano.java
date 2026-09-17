package studojurata_api.model.enums;

/**
 * Distinto de StatusAtivoInativo porque um plano não é desligado: é CONCLUIDO
 * ao fim do ciclo, e a turma pode receber um novo plano em seguida.
 */
public enum StatusPlano {
    ATIVO,
    CONCLUIDO
}
