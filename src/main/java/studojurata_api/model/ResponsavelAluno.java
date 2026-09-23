package studojurata_api.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.model.enums.Parentesco;

/**
 * Vínculo entre um aluno e um responsável — é ele que diz quem responde pelo
 * aluno (e o parentesco). O aceite de termos não faz parte deste vínculo: o
 * responsável não tem login no sistema, então não haveria quem o assinasse.
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
