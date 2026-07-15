package studojurata_api.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

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

    private Boolean acertou;

    /** Tempo gasto pelo aluno nesta questão específica, em segundos. */
    private Integer tempoResposta;
}
