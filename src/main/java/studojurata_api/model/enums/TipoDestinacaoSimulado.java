package studojurata_api.model.enums;

/**
 * Define, no lançamento do simulado (ver item 1.3 e problema 1.3 da Análise
 * Crítica), se ele vale para todos os alunos com matrícula ativa na turma ou
 * apenas para uma lista específica de alunos informada pelo professor.
 *
 * A lista de alunos elegíveis em si não é armazenada como atributo do
 * Simulado (evitaríamos duplicar estado): quando ESPECIFICO, os ids são
 * recebidos apenas no momento do lançamento (SimuladoService.lancar) e o
 * registro que efetivamente materializa "quem foi convocado" é o conjunto de
 * SimuladoAluno criado — este sim persistido e consultável.
 */
public enum TipoDestinacaoSimulado {
    TODOS,
    ESPECIFICO
}
