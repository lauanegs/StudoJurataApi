package studojurata_api.model.gamificacao;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.model.Aluno;
import studojurata_api.model.BaseEntity;

/**
 * Moedas vêm de simulado concluído e de reforço registrado, não só de acerto,
 * para que quem revisa seja tão recompensado quanto quem acerta de primeira.
 * Sempre consultado individualmente: não há ranking entre colegas.
 */
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
