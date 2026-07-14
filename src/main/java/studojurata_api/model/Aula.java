package studojurata_api.model;

import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.model.enums.StatusAtivoInativo;

/**
 * Aula: registro de execução de uma sessão de ensino, sempre vinculada a um
 * PlanoAula (que por sua vez amarra TurmaDisciplina + PlanoEnsino).
 *
 * Correção 2.5 da Análise Crítica: a interface leva do PlanoAula para a tela
 * "Aulas" (estatísticas de aulas realizadas daquele plano específico), mas
 * antes não existia FK de Aula para PlanoAula. Adicionada aqui.
 *
 * Correção 3.2: a cadeia curricular passa a ter dono único
 * (PlanoEnsino → ConteudoPlano → Aula/Questao); por isso os campos
 * turmaDisciplina e planoEnsino, que duplicavam o caminho já obtido via
 * planoAula.getTurmaDisciplina() / planoAula.getPlanoEnsino(), foram
 * removidos daqui.
 *
 * Correção 4.3: exclusões de registros com histórico pedagógico devem ser
 * soft-delete. O campo status permite marcar a aula como INATIVA em vez de
 * apagá-la fisicamente (ver AulaService.deletar).
 */
@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class Aula extends BaseEntity {

    @ManyToOne(optional = false)
    private PlanoAula planoAula;

    /** Quantidade de horários (carga horária) daquela aula específica. */
    private Integer cargaHoraria;

    private LocalDate dataPrevista;

    /** Número/ordem sequencial da aula dentro do plano de aula. */
    private Integer ordem;

    private String titulo;

    /** Data em que a aula realmente foi ministrada (preenchida = aula realizada). */
    private LocalDate dataPublicacao;

    /** Anotações do professor sobre a aula. */
    @Column(length = 2000)
    private String observacoes;

    @Enumerated(EnumType.STRING)
    private StatusAtivoInativo status;
}