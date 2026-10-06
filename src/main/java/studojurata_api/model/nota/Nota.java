package studojurata_api.model.nota;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.model.pessoa.Aluno;
import studojurata_api.model.BaseEntity;
import studojurata_api.model.curriculo.Disciplina;
import studojurata_api.model.turma.Turma;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"aluno_id", "disciplina_id", "turma_id"}))
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class Nota extends BaseEntity {
    @ManyToOne(optional = false)
    private Aluno aluno;

    @ManyToOne(optional = false)
    private Disciplina disciplina;

    @ManyToOne(optional = true)
    private Turma turma;

    private Double total;

    private Integer quantidadeSimuladosConsiderados;
}
