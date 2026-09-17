package studojurata_api.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.model.enums.Parentesco;

/**
 * Consentimento propositalmente simples (aceite + texto exibido), sem
 * versionamento formal de termos, enquanto não houver definição jurídica
 * sobre a responsabilidade escola vs. plataforma.
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

    /** Aceite do uso dos dados do aluno na plataforma. */
    private Boolean aceitouTermos = false;

    private LocalDateTime dataAceite;

    /** Texto curto exibido junto ao checkbox no momento do aceite (não um sistema de versionamento formal). */
    @Column(length = 500)
    private String textoVersao;
}
