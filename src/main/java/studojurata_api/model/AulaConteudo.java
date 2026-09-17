package studojurata_api.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"aula_id", "conteudo_plano_id"}))
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class AulaConteudo extends BaseEntity {

    @ManyToOne(optional = false)
    private Aula aula;

    @ManyToOne(optional = false)
    private ConteudoPlano conteudoPlano;
}