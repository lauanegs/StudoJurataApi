package studojurata_api.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.model.enums.Parentesco;

/**
 * Resolve a relação N:N entre Responsavel e Aluno (um responsável pode ter
 * múltiplos alunos; um aluno pode ter múltiplos responsáveis), guardando
 * também o parentesco pedido pela tela de cadastro de aluno.
 *
 * Correção 10.3 da Segunda Análise Crítica ("Consentimento — decisão
 * confirmada"): enquanto não há retorno jurídico sobre a responsabilidade
 * escola vs. plataforma, o consentimento é registrado aqui de forma
 * deliberadamente simples — um checkbox de aceite com um texto curto —
 * em vez de um sistema formal de versionamento de termos. Trocar por um
 * modelo mais robusto assim que houver decisão jurídica.
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"responsavel_id", "aluno_id"}))
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class ResponsavelAluno extends BaseEntity {

    @ManyToOne
    @JoinColumn(nullable = false)
    private Responsavel responsavel;

    @ManyToOne
    @JoinColumn(nullable = false)
    private Aluno aluno;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Parentesco parentesco;

    /** Checkbox de aceite (item 10.3) — true quando o responsável aceitou o uso dos dados do aluno na plataforma. */
    private Boolean aceitouTermos = false;

    private LocalDateTime dataAceite;

    /** Texto curto exibido junto ao checkbox no momento do aceite (não um sistema de versionamento formal). */
    @Column(length = 500)
    private String textoVersao;
}
