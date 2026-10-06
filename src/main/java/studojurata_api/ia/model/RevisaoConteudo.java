package studojurata_api.ia.model;

import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.ia.model.enums.NivelDominio;
import studojurata_api.model.pessoa.Aluno;
import studojurata_api.model.BaseEntity;
import studojurata_api.model.planejamento.ConteudoPlano;

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
