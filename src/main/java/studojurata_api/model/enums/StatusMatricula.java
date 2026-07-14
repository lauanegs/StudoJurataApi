package studojurata_api.model.enums;

/**
 * Status do vínculo Aluno-Turma (matrícula).
 *
 * ATIVA       -> aluno cursando normalmente a turma.
 * CONCLUIDA   -> aluno concluiu o ciclo/curso naquela turma (encerramento natural).
 * CANCELADA   -> matrícula cancelada/desistência. O registro é mantido (soft delete),
 *                nunca removido fisicamente, preservando o histórico pedagógico.
 * TRANSFERIDA -> encerrada nesta turma porque o aluno foi transferido para outra
 *                turma. Distinta de CANCELADA para não confundir "saiu da escola"
 *                com "mudou de turma".
 */
public enum StatusMatricula {
    ATIVA,
    CONCLUIDA,
    CANCELADA,
    TRANSFERIDA
}
