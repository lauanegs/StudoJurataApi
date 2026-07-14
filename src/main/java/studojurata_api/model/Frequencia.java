package studojurata_api.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * Registro de presença/frequência de um Aluno em uma Aula.
 *
 * Correção 1.1 da Análise Crítica: a tela "Registro de aula" tem uma aba
 * "Realizar chamada" com checkbox de presença por aluno, mas não existia
 * nenhuma entidade correspondente no back-end — a funcionalidade era uma
 * "tela fantasma" sem persistência. Esta entidade resolve a relação N:N
 * entre Aluno e Aula, guardando se o aluno esteve presente e, quando
 * ausente, uma justificativa opcional.
 *
 * A constraint de unicidade (aluno, aula) impede o registro de duas
 * frequências para o mesmo aluno na mesma aula.
 */
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
