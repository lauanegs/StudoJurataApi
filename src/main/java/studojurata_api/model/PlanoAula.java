package studojurata_api.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.model.enums.StatusPlano;

/**
 * Plano de aula: nível de execução do currículo para uma TurmaDisciplina,
 * amarrado ao PlanoEnsino (currículo/nível macro) do qual herda curso e
 * carga horária total.
 *
 * Correção 2.6 da Análise Crítica: os campos cargaHoraria e curso eram
 * duplicados aqui e em PlanoEnsino, com risco de divergência. Ficou
 * definido (Respostas à Análise Crítica) que esses atributos pertencem ao
 * Plano de Ensino (nível macro); o Plano de Aula passa a obtê-los sempre
 * via planoEnsino.getCargaHoraria() / getCurso(), nunca os armazenando
 * duplicados.
 */
@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class PlanoAula extends BaseEntity {

    @ManyToOne(optional = false)
    private TurmaDisciplina turmaDisciplina;

    /**
     * Relação 1-para-1 com PlanoEnsino (pedido explícito): um plano de
     * ensino tem no máximo um plano de aula, e um plano de aula pertence a
     * exatamente um plano de ensino (já garantido pelo próprio campo). A
     * constraint unique no FK trava isso também no banco, além da
     * verificação em PlanoAulaService.validar().
     */
    @ManyToOne(optional = false)
    @JoinColumn(unique = true)
    private PlanoEnsino planoEnsino;

    /**
     * Correção "matrícula cíclica": status virou StatusPlano
     * (ATIVO/CONCLUIDO) — mesmo raciocínio de PlanoEnsino.status.
     */
    @Enumerated(EnumType.STRING)
    private StatusPlano status;
}