package studojurata_api.ia.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.ia.model.enums.OrigemResultadoGeracao;
import studojurata_api.model.BaseEntity;
import studojurata_api.model.ConteudoPlano;
import studojurata_api.model.Disciplina;
import studojurata_api.model.Simulado;
import studojurata_api.model.enums.NivelDificuldade;
import studojurata_api.model.enums.TipoQuestao;

/**
 * Um registro por chamada de GeracaoQuestaoIAService.gerar, com qualquer
 * resultado: permite auditar a origem das questões, diagnosticar falhas do
 * Gemini e medir o quanto o cache evita chamadas.
 */
@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class HistoricoGeracaoIA extends BaseEntity {

    @ManyToOne
    private ConteudoPlano conteudoPlano;

    @ManyToOne
    private Disciplina disciplina;

    /** Simulado que motivou/recebeu esta geração, quando aplicável. */
    @ManyToOne
    private Simulado simulado;

    @Enumerated(EnumType.STRING)
    private NivelDificuldade nivelDificuldade;

    @Enumerated(EnumType.STRING)
    private TipoQuestao tipoQuestao;

    private Integer quantidadeSolicitada;
    private Integer quantidadeReaproveitadaCache;
    private Integer quantidadeGeradaGemini;
    private Integer quantidadeFallbackBanco;

    @Enumerated(EnumType.STRING)
    private OrigemResultadoGeracao origemResultado;

    private String modeloUtilizado;

    private Long tempoRespostaMs;

    /** Preenchida quando a chamada ao Gemini falhou (mesmo que o fallback tenha coberto a demanda). */
    @Column(length = 1000)
    private String mensagemErro;

    private LocalDateTime dataGeracao;
}
