package studojurata_api.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class ConteudoPlano extends BaseEntity {

    @ManyToOne
    private PlanoEnsino planoEnsino;

    private String titulo;
    private String descricao;
    private Integer ordem;
    private Integer cargaHoraria;
    private String status;
}