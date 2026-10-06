package studojurata_api.model.gamificacao;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.model.pessoa.Aluno;
import studojurata_api.model.BaseEntity;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"aluno_id"}))
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class PontuacaoAluno extends BaseEntity {
    @OneToOne(optional = false)
    @JoinColumn(unique = true)
    private Aluno aluno;

    @Column(nullable = false)
    private Integer moedas = 0;
}
