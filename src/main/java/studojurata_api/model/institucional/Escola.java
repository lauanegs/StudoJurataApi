package studojurata_api.model.institucional;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.model.enums.StatusAtivoInativo;
import studojurata_api.model.BaseEntity;

/**
 * Tenant. Existe mesmo com uma única escola hoje porque introduzi-lo depois
 * exigiria uma migração muito mais cara.
 */
@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class Escola extends BaseEntity {

    @Column(nullable = false)
    private String nome;

    @Column(unique = true, length = 18)
    private String cnpj;

    @Enumerated(EnumType.STRING)
    private StatusAtivoInativo status;
}
