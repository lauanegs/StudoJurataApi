package studojurata_api.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class Alternativa extends BaseEntity {

    @ManyToOne
    private Questao questao;

    private String texto;
    private Boolean correta;
    private Integer ordem;
}