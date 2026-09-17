package studojurata_api.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Resposta do aluno a uma questão dentro de uma tentativa (SimuladoAluno).
 * Questão deixada em branco é registrada com alternativa nula e
 * acertou=false.
 *
 * Questão VERDADEIRO_FALSO não usa `alternativa` (é uma lista de afirmações
 * julgadas independentemente, não uma escolha única): usa
 * alternativasVerdadeiras, com as afirmações que o aluno marcou como
 * Verdadeiras — as demais alternativas da questão são, por exclusão, as que
 * o aluno marcou como Falsas. `acertou` é da questão inteira: só é true
 * quando TODAS as afirmações foram julgadas corretamente (marcar uma errada
 * derruba a questão toda, igual a uma prova tradicional).
 */
@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class QuestaoAluno extends BaseEntity {

    @ManyToOne(optional = false)
    private SimuladoAluno simuladoAluno;

    @ManyToOne(optional = false)
    private Questao questao;

    @ManyToOne
    private Alternativa alternativa;

    @ManyToMany
    @JoinTable(
            name = "questao_aluno_alternativa_verdadeira",
            joinColumns = @JoinColumn(name = "questao_aluno_id"),
            inverseJoinColumns = @JoinColumn(name = "alternativa_id")
    )
    private List<Alternativa> alternativasVerdadeiras = new ArrayList<>();

    /**
     * Explícito porque, em VERDADEIRO_FALSO, alternativasVerdadeiras vazia é
     * ambígua: tanto "aluno não respondeu nada" quanto "aluno julgou todas as
     * afirmações como Falsas" (uma resposta legítima) chegam como lista
     * vazia. Sem esse flag a tela de revisão não teria como distinguir
     * "Em branco" de "respondeu e errou tudo".
     */
    private Boolean respondida;

    private Boolean acertou;

    /** Tempo gasto pelo aluno nesta questão específica, em segundos. */
    private Integer tempoResposta;
}
