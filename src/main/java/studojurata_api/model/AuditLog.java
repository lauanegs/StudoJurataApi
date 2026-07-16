package studojurata_api.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.model.enums.AcaoAuditoria;

/**
 * Log de auditoria (item 2.9/10.4 da Segunda Análise Crítica, "Auditoria —
 * decisão confirmada"): registra quem fez o quê em entidades sensíveis
 * (inicialmente Nota, SimuladoAluno e Aula — ver AuditLogService e os pontos
 * de chamada em NotaService, SimuladoAlunoService e AulaService).
 *
 * Propositalmente simples para o estágio atual do projeto (MVP): não é um
 * diff campo-a-campo, mas uma linha legível (detalhes) com o suficiente para
 * responder "quem alterou a nota do aluno X, quando, e o que mudou".
 * createdAt (herdado de BaseEntity) já registra o "quando".
 */
@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class AuditLog extends BaseEntity {

    /** Nome simples da entidade afetada (ex.: "Nota", "SimuladoAluno", "Aula"). */
    @Column(nullable = false)
    private String entidade;

    @Column(nullable = false)
    private Long entidadeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AcaoAuditoria acao;

    /** Username de quem executou a ação; null quando a ação não teve usuário autenticado (ex.: job automático). */
    private String usuario;

    /** Descrição legível do que mudou (ex.: "total: 7.0 -> 8.5"). */
    @Column(length = 1000)
    private String detalhes;
}
