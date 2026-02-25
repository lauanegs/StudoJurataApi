package studojurata_api.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class SimuladoAluno extends BaseEntity {

    @ManyToOne
    private Simulado simulado;

    @ManyToOne
    private Aluno aluno;

    private Integer quantidadeAcertos;
    private Double nota;
    private String status;
}