package studojurata_api.machinelearning.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.ia.model.enums.MotivoRecomendacao;
import studojurata_api.machinelearning.model.enums.DecisaoGeracao;
import studojurata_api.machinelearning.model.enums.NecessidadeRevisao;
import studojurata_api.machinelearning.model.enums.OrigemDecisao;
import studojurata_api.model.pessoa.Aluno;
import studojurata_api.model.BaseEntity;
import studojurata_api.model.planejamento.ConteudoPlano;
import studojurata_api.model.curriculo.Disciplina;
import studojurata_api.model.simulado.Questao;
import studojurata_api.model.simulado.Simulado;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class RecomendacaoSimulado extends BaseEntity {
    @ManyToOne(optional = false)
    private Aluno aluno;

    @ManyToOne
    private Disciplina disciplina;

    @ManyToOne(optional = false)
    private ConteudoPlano conteudoPlano;

    @ManyToOne
    private Simulado simulado;

    private Double percentualAcertoConteudo;
    private Double percentualAcertoRecente;
    private Double percentualAcertoDisciplina;
    private Integer quantidadeTentativas;
    private Integer quantidadeRevisoes;
    private Integer diasDesdeUltimaResposta;
    private Integer diasDesdeUltimaRevisao;
    private Double dificuldadeMediaRespondida;
    private Integer quantidadeQuestoesRespondidas;

    @Enumerated(EnumType.STRING)
    private NecessidadeRevisao necessidadeRevisao;

    @Enumerated(EnumType.STRING)
    private OrigemDecisao origemDecisao;

    @Enumerated(EnumType.STRING)
    private DecisaoGeracao decisaoGeracao;

    private Integer quantidadeRecomendada;
    private Integer intervaloRevisaoDias;
    private Boolean necessitaGeracaoIA;
    private Boolean dadosInsuficientes;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    private Set<MotivoRecomendacao> motivos = new LinkedHashSet<>();

    @Column(length = 1000)
    private String motivo;

    @ManyToMany
    @JoinTable(
            name = "recomendacao_simulado_questao",
            joinColumns = @JoinColumn(name = "recomendacao_simulado_id"),
            inverseJoinColumns = @JoinColumn(name = "questao_id")
    )
    private List<Questao> questoesSelecionadas = new ArrayList<>();

    private Double percentualAcertoPosterior;

    private LocalDate dataResultado;
}
