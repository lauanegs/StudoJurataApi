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

    /** Correção 9.1 (Escola/tenant). */
    @ManyToOne(optional = false)
    private Escola escola;

    private String titulo;
    private Integer cargaHoraria;

    /** Correção 2.11 da Segunda Análise Crítica: era String livre, agora enum. */
    @Enumerated(EnumType.STRING)
    private StatusAtivoInativo status;
}
