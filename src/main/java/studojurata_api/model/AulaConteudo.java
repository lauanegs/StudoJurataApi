package studojurata_api.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * Vínculo N:N entre Aula e ConteudoPlano ("conteúdos vistos naquela aula",
 * conforme a tela "Registrar conteúdo" / "Vincular conteúdo do plano de
 * ensino"). Uma constraint de unicidade evita que o mesmo conteúdo seja
 * vinculado duas vezes à mesma aula.
 */
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