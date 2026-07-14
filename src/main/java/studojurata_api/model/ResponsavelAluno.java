package studojurata_api.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.model.enums.Parentesco;

/**
 * Resolve a relação N:N entre Responsavel e Aluno (um responsável pode ter
 * múltiplos alunos; um aluno pode ter múltiplos responsáveis), guardando
 * também o parentesco pedido pela tela de cadastro de aluno.
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"responsavel_id", "aluno_id"}))
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class ResponsavelAluno extends BaseEntity {

    @ManyToOne
    @JoinColumn(nullable = false)
    private Responsavel responsavel;

    @ManyToOne
    @JoinColumn(nullable = false)
    private Aluno aluno;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Parentesco parentesco;
}
