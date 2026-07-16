package studojurata_api.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.model.enums.StatusAtivoInativo;

/**
 * Correção solicitada após a Terceira Análise Crítica: "curso" deixa de ser
 * apenas um atributo String em Turma e passa a ser uma entidade própria.
 * Necessário porque uma mesma escola pode oferecer vários cursos (ex.:
 * "Técnico em Administração", "Preparatório para o ENEM"), cada um com
 * várias turmas vinculadas ao longo do tempo, e o curso em si tem atributos
 * próprios (descrição, carga horária total) que não fazem sentido como
 * texto livre repetido a cada Turma cadastrada.
 *
 * Segue o mesmo padrão de isolamento por escola já aplicado a
 * Turma/Disciplina/Usuario (correção 2.2 da Terceira Análise Crítica): um
 * curso pertence a uma escola.
 */
@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class Curso extends BaseEntity {

    @ManyToOne(optional = false)
    private Escola escola;

    @Column(nullable = false)
    private String nome;

    private String descricao;

    /** Carga horária total do curso (soma informativa das disciplinas/planos de ensino vinculados). */
    private Integer cargaHorariaTotal;

    @Enumerated(EnumType.STRING)
    private StatusAtivoInativo status;
}
