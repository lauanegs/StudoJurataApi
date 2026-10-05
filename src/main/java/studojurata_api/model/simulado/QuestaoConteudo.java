package studojurata_api.model.simulado;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.model.BaseEntity;
import studojurata_api.model.planejamento.ConteudoPlano;

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