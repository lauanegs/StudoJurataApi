package studojurata_api.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.model.enums.StatusAtivoInativo;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class ConteudoPlano extends BaseEntity {

    @ManyToOne
    private PlanoEnsino planoEnsino;

    private String titulo;
    private String descricao;
    private Integer ordem;
    private Integer cargaHoraria;

    /**
     * Correção 2.11 da Segunda Análise Crítica: era String livre, agora enum.
     * INATIVO é usado como soft-delete quando o conteúdo é removido do plano
     * (ver item "Conteúdo removido" nos Casos Extremos) — nunca DELETE físico
     * quando já existir QuestaoConteudo/AulaConteudo vinculado.
     */
    @Enumerated(EnumType.STRING)
    private StatusAtivoInativo status;
}
