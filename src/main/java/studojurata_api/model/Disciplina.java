package studojurata_api.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.model.enums.StatusAtivoInativo;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class Disciplina extends BaseEntity {

    @ManyToOne(optional = false)
    private Escola escola;

    private String titulo;

    @Enumerated(EnumType.STRING)
    private StatusAtivoInativo status;
}
