package studojurata_api.model.gamificacao;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.model.BaseEntity;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class Skin extends BaseEntity {
    @Column(nullable = false, unique = true)
    private String nome;

    private String descricao;

    @Column(nullable = false)
    private Integer custoMoedas;

    private String urlAsset;

    @Column(nullable = false)
    private Boolean disponivel = true;
}
