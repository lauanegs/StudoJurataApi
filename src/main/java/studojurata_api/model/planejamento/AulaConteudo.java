package studojurata_api.model.planejamento;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.model.BaseEntity;

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