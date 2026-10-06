package studojurata_api.model.planejamento;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.model.enums.StatusAtivoInativo;
import studojurata_api.model.BaseEntity;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class ConteudoPlano extends BaseEntity {
    @ManyToOne
    private PlanoEnsino planoEnsino;

    private String titulo;
    @Column(columnDefinition = "TEXT")
    private String descricao;
    private Integer ordem;

    @Enumerated(EnumType.STRING)
    private StatusAtivoInativo status;
}
