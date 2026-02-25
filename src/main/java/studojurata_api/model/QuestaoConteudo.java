package studojurata_api.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class QuestaoConteudo extends BaseEntity {

    @ManyToOne
    private Questao questao;

    @ManyToOne
    private ConteudoPlano conteudoPlano;
}