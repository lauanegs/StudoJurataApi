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
 * Ver item 1.5 da Análise Crítica (sugestão aprovada): rastreia o reforço
 * adaptativo por repetição espaçada de um aluno sobre um conteúdo específico,
 * seguindo a teoria da curva de esquecimento citada no TCC.
 *
 * A cada reforço realizado (RevisaoConteudoService.registrarReforco):
 * - quantidadeReforcos é incrementada;
 * - dataUltimoReforco passa a ser hoje;
 * - dataProximoReforco é recalculada como hoje + 2^quantidadeReforcos dias
 *   (intervalo dobra a cada repetição, conforme pedido explicitamente no
 *   item 1.5: "dataProximoReforço (calculada por 2ⁿ)");
 * - nivelDominio é reavaliado (ver NivelDominio).
 *
 * Existe no máximo um registro por par (aluno, conteudoPlano) — histórico de
 * reforços é resumido neste único registro (quantidadeReforcos acumula a
 * contagem), não uma linha por evento, mantendo o modelo simples para o MVP.
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
