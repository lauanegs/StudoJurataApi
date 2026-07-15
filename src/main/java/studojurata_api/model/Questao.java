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
 * Ver itens 7.1, 7.2 e 7.3 da Análise Crítica:
 * - toda questão é vinculada ao conteúdo através de QuestaoConteudo
 *   (relação já existente, mantida à parte para não duplicar FK aqui);
 * - nivelDificuldade e origem foram adicionados para permitir auditoria de
 *   qualidade e, futuramente, alimentar sinais de dificuldade progressiva;
 * - status passou a representar o fluxo de moderação: questões de origem IA
 *   nascem PENDENTE e só entram no banco de reaproveitamento (podem ser
 *   vinculadas a novos simulados) após aprovação do professor; questões de
 *   origem PROFESSOR nascem já APROVADA.
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
