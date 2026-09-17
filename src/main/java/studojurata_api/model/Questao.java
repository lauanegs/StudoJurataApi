package studojurata_api.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.model.enums.NivelDificuldade;
import studojurata_api.model.enums.OrigemQuestao;
import studojurata_api.model.enums.StatusQuestao;
import studojurata_api.model.enums.TipoQuestao;

/**
 * O vínculo com conteúdos fica em QuestaoConteudo. status é o fluxo de
 * moderação: questões de origem IA nascem PENDENTE e só podem ir para novos
 * simulados após aprovação; as do professor já nascem APROVADA.
 */
@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class Questao extends BaseEntity {

    private String enunciado;

    @Enumerated(EnumType.STRING)
    private TipoQuestao tipo;

    @ManyToOne
    private Disciplina disciplina;

    @Enumerated(EnumType.STRING)
    private NivelDificuldade nivelDificuldade;

    @Enumerated(EnumType.STRING)
    private OrigemQuestao origem;

    @Enumerated(EnumType.STRING)
    private StatusQuestao status;
}
