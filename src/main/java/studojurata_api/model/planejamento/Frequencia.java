package studojurata_api.model.planejamento;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.model.pessoa.Aluno;
import studojurata_api.model.BaseEntity;

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

    @Column(length = 500)
    private String justificativa;
}
