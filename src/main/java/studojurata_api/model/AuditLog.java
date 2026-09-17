package studojurata_api.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.model.enums.AcaoAuditoria;

/**
 * Quem fez o quê em entidades sensíveis (Nota, SimuladoAluno, Aula). Uma
 * linha legível em detalhes, não um diff campo a campo; o "quando" é o
 * createdAt de BaseEntity.
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
