package studojurata_api.model.gamificacao;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.model.Aluno;
import studojurata_api.model.BaseEntity;

/**
 * Correção 8.1/8.2 da Segunda Análise Crítica ("Gamificação — decisão
 * confirmada"): moedas ganhas por simulado concluído E por reforço
 * registrado em RevisaoConteudo — não só por acerto — garantindo a equidade
 * pedida (quem revisa é tão beneficiado quanto quem acerta de primeira).
 * Sem ranking competitivo entre colegas (decisão explícita do usuário):
 * PontuacaoAluno é sempre consultado individualmente, nunca em uma listagem
 * comparativa entre alunos.
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"aluno_id"}))
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class PontuacaoAluno extends BaseEntity {

    @OneToOne(optional = false)
    @JoinColumn(unique = true)
    private Aluno aluno;

    @Column(nullable = false)
    private Integer moedas = 0;

    @Column(nullable = false)
    private Integer xpTotal = 0;
}
