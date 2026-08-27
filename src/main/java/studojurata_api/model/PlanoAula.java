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

    @ManyToOne(optional = false)
    private PlanoEnsino planoEnsino;

    /**
     * Correção "matrícula cíclica": status virou StatusPlano
     * (ATIVO/CONCLUIDO) — mesmo raciocínio de PlanoEnsino.status.
     */
    @Enumerated(EnumType.STRING)
    private StatusPlano status;
}