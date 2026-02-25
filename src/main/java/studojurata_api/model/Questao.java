package studojurata_api.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class Questao extends BaseEntity {

    private String enunciado;
    private String tipo;

    @ManyToOne
    private Disciplina disciplina;

    private String status;
}