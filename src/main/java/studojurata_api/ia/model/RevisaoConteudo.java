package studojurata_api.ia.model;

import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.ia.model.enums.NivelDominio;
import studojurata_api.model.Aluno;
import studojurata_api.model.BaseEntity;
import studojurata_api.model.ConteudoPlano;

/**
 * Repetição espaçada de um aluno sobre um conteúdo, baseada na curva de
 * esquecimento. A cada reforço, dataProximoReforco avança 7 dias (1º), 14 (2º)
 * e 90 (3º); a partir do 4º o conteúdo é considerado dominado
 * (dataProximoReforco nula, nivelDominio ALTO).
 *
 * Um único registro por aluno+conteúdo: quantidadeReforcos resume o
 * histórico em vez de uma linha por evento.
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"aluno_id", "conteudo_plano_id"}))
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class RevisaoConteudo extends BaseEntity {

    @ManyToOne(optional = false)
    private Aluno aluno;

    @ManyToOne(optional = false)
    private ConteudoPlano conteudoPlano;

    private Integer quantidadeReforcos;

    private LocalDate dataUltimoReforco;

    private LocalDate dataProximoReforco;

    @Enumerated(EnumType.STRING)
    private NivelDominio nivelDominio;
}
