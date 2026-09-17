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
public class ConteudoPlano extends BaseEntity {

    @ManyToOne
    private PlanoEnsino planoEnsino;

    private String titulo;
    private String descricao;
    private Integer ordem;

    /** INATIVO é o soft-delete: conteúdo com questões ou aulas vinculadas nunca é apagado. */
    @Enumerated(EnumType.STRING)
    private StatusAtivoInativo status;
}
