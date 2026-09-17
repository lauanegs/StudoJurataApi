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
public class Curso extends BaseEntity {

    @ManyToOne(optional = false)
    private Escola escola;

    @Column(nullable = false)
    private String nome;

    private String descricao;

    /** Soma das cargas horárias ativas da grade (CursoDisciplinaService); não é editável. */
    private Integer cargaHorariaTotal;

    @Enumerated(EnumType.STRING)
    private StatusAtivoInativo status;
}
