package studojurata_api.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.model.enums.StatusAtivoInativo;

/**
 * Plano de aula: nível de execução do currículo para uma TurmaDisciplina,
 * amarrado ao PlanoEnsino (currículo/nível macro) do qual herda curso,
 * carga horária total e período letivo.
 *
 * Correção 2.6 da Análise Crítica: os campos cargaHoraria, curso e
 * periodoLetivo eram duplicados aqui e em PlanoEnsino, com risco de
 * divergência. Ficou definido (Respostas à Análise Crítica) que esses
 * atributos pertencem ao Plano de Ensino (nível macro); o Plano de Aula
 * passa a obtê-los sempre via planoEnsino.getCargaHoraria() / getCurso() /
 * getPeriodoLetivo(), nunca os armazenando duplicados.
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
     * Correção 2.11: status deixa de ser String livre e passa a ser enum,
     * evitando inconsistências como "ativo" vs "Ativo" vs "ATIVO".
     */
    @Enumerated(EnumType.STRING)
    private StatusAtivoInativo status;
}