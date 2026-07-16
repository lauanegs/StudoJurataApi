package studojurata_api.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Correção 2.9 da Segunda Análise Crítica ("Auditoria — decisão confirmada"):
 * createdAt/updatedAt agora são preenchidos automaticamente (JPA Auditing,
 * habilitado via @EnableJpaAuditing em StudojurataApiApplication) para todas
 * as entidades que estendem BaseEntity, sem precisar de código repetido em
 * cada uma. Complementado por AuditLog (ver model/AuditLog.java) para as
 * entidades sensíveis (Nota, SimuladoAluno, Aula), que precisam não só de
 * "quando foi a última alteração" mas de "o que mudou e quem mudou".
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
