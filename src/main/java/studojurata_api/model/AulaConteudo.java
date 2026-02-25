package studojurata_api.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class AulaConteudo extends BaseEntity {

    @ManyToOne
    private Aula aula;

    @ManyToOne
    private ConteudoPlano conteudoPlano;
}