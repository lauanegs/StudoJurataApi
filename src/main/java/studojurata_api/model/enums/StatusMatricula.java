package studojurata_api.model.enums;

/**
 * Status do vínculo Aluno-Turma (matrícula).
 *
 * ATIVA     -> aluno cursando normalmente a turma.
 * CONCLUIDA -> aluno concluiu o ciclo/curso naquela turma (encerramento natural,
 *              inclusive automático ao atingir a carga horária do curso — ver
 *              FrequenciaService). Pode ser reativada (voltar para ATIVA) via
 *              edição da matrícula, a critério da organização.
 * CANCELADA -> matrícula cancelada/desistência. O registro é mantido (soft delete),
 *              nunca removido fisicamente, preservando o histórico pedagógico.
 *
 * Não existe transferência de turma nesta primeira versão da plataforma
 * (decisão confirmada pelo usuário) — para trocar um aluno de turma, cancele a
 * matrícula atual e crie uma nova na turma de destino.
 */
public enum StatusMatricula {
    ATIVA,
    CONCLUIDA,
    CANCELADA
}
