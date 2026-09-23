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
import studojurata_api.model.Aluno;
import studojurata_api.model.BaseEntity;
import studojurata_api.model.ConteudoPlano;
import studojurata_api.model.Disciplina;
import studojurata_api.model.Questao;
import studojurata_api.model.Simulado;

/**
 * Registro do aprendizado: cada recomendação emitida guarda os atributos que a
 * originaram, a decisão tomada e, depois, o resultado real do aluno naquilo que
 * foi aplicado. É esta tabela que alimenta o treino do Weka — o rótulo do
 * treino é o desempenho observado depois da recomendação, não a própria decisão
 * do sistema (senão o modelo só aprenderia a imitar as regras atuais).
 *
 * <p>Os atributos ficam em colunas (e não num texto serializado) porque são
 * numéricos e serão lidos como dataset; ficam só os realmente usados na
 * decisão, para não duplicar dado que já vive em QuestaoAluno/SimuladoAluno.
 */
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

    /** Simulado individual gerado a partir desta recomendação, quando houve. */
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

    /** Questões indicadas pela recomendação — liga o desfecho posterior ao que foi aplicado. */
    @ManyToMany
    @JoinTable(
            name = "recomendacao_simulado_questao",
            joinColumns = @JoinColumn(name = "recomendacao_simulado_id"),
            inverseJoinColumns = @JoinColumn(name = "questao_id")
    )
    private List<Questao> questoesSelecionadas = new ArrayList<>();

    /** Percentual de acerto do aluno (0 a 1) naquilo que foi aplicado depois da recomendação. */
    private Double percentualAcertoPosterior;

    private LocalDate dataResultado;
}
