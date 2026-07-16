package studojurata_api.model.gamificacao;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.model.BaseEntity;

/**
 * Catálogo de skins do personagem/mascote, compráveis com moedas (item 8.2
 * — decisão confirmada). No lançamento, apenas 3 skins disponíveis
 * (ver GamificacaoSeeder).
 */
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

    /** URL/caminho do asset visual da skin. */
    private String urlAsset;

    @Column(nullable = false)
    private Boolean disponivel = true;
}
