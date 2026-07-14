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
public class Professor extends BaseEntity {

    /**
     * 1 Pessoa = no máximo 1 Professor.
     */
    @OneToOne
    @JoinColumn(unique = true, nullable = false)
    private Pessoa pessoa;

    @Enumerated(EnumType.STRING)
    private StatusAtivoInativo status;
}
