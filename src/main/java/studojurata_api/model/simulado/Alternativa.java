package studojurata_api.model.simulado;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.model.BaseEntity;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class Alternativa extends BaseEntity {

    @ManyToOne(optional = false)
    private Questao questao;

    @Column(columnDefinition = "TEXT")

    private String texto;
    private Boolean correta;
    private Integer ordem;
}