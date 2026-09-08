package studojurata_api.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Representa a resposta do aluno a uma questão dentro de uma tentativa de
 * simulado (conceitualmente "RespostaAluno" — ver item 2.4 da Análise
 * Crítica). Antes desta correção a entidade só guardava questao/alternativa/
 * acertou, sem nenhuma FK para o aluno ou para a tentativa (SimuladoAluno), o
 * que tornava estruturalmente impossível reconstruir "quais respostas o
 * aluno X deu no simulado Y" — exatamente o que a tela de detalhamento do
 * simulado por aluno precisa exibir.
 *
 * Agora cada resposta pertence a um SimuladoAluno (a tentativa), o que já
 * identifica aluno e simulado transitivamente, e registra o tempo gasto pelo
 * aluno naquela questão específica (tempoResposta, em segundos) — usado tanto
 * para a tela de detalhamento quanto como sinal de desempenho (item 7.4).
 *
 * alternativa pode ser nula quando a questão é deixada em branco (ver item
 * 4.2): a finalização do simulado não é bloqueada por questões sem resposta;
 * elas são registradas aqui com alternativa=null e acertou=false.
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
