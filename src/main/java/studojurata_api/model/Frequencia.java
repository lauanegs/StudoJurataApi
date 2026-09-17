package studojurata_api.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"aluno_id", "aula_id"}))
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class Frequencia extends BaseEntity {

    @ManyToOne(optional = false)
    private Aluno aluno;

    @ManyToOne(optional = false)
    private Aula aula;

    @Column(nullable = false)
    private Boolean presente;

    /** Preenchida opcionalmente quando presente = false. */
    @Column(length = 500)
    private String justificativa;
}
