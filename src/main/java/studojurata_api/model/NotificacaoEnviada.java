package studojurata_api.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.model.enums.TipoNotificacao;

/**
 * Item 9.8 do documento de regras: notificação a responsáveis. Por enquanto
 * só estrutura + registro em banco — sem provedor de e-mail/SMS configurado,
 * "enviada" fica sempre false, deixando explícito que é apenas o registro do
 * que seria enviado.
 */
@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class NotificacaoEnviada extends BaseEntity {

    @ManyToOne(optional = false)
    private ResponsavelAluno responsavelAluno;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoNotificacao tipo;

    @Column(length = 500)
    private String mensagem;

    /** Sempre false por enquanto (nenhum provedor real plugado). */
    @Column(nullable = false)
    private Boolean enviada = false;
}
