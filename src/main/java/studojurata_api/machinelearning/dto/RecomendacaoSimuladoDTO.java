package studojurata_api.machinelearning.dto;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;
import studojurata_api.ia.model.enums.MotivoRecomendacao;
import studojurata_api.machinelearning.model.enums.DecisaoGeracao;
import studojurata_api.machinelearning.model.enums.NecessidadeRevisao;
import studojurata_api.machinelearning.model.enums.OrigemDecisao;
import studojurata_api.model.Questao;
import studojurata_api.model.enums.NivelDificuldade;

/**
 * Resultado da recomendação de um simulado individual. É interno (não é
 * contrato de tela): quem consome é a geração de simulado por IA e os jobs.
 */
@Getter
@Setter
public class RecomendacaoSimuladoDTO {

    private Long id;
    private Long alunoId;
    private Long disciplinaId;
    private Long conteudoPlanoId;
    private String conteudoTitulo;

    /** Nunca passa de 10 — o limite do simulado é aplicado na seleção. */
    private int quantidadeRecomendada;
    private List<Long> conteudosPrioritarios = List.of();
    private Map<NivelDificuldade, Integer> distribuicaoDeDificuldade = new LinkedHashMap<>();
    private List<Questao> questoesCandidatas = List.of();

    private DecisaoGeracao decisaoGeracao = DecisaoGeracao.REUTILIZAR_BANCO;
    private boolean necessitaGeracaoIA;
    private Set<MotivoRecomendacao> motivos = Set.of();
    private String motivo;
    private int intervaloRevisaoDias;
    /** Nível de dificuldade que deve predominar no simulado; null quando não há sinal que aponte um. */
    private NivelDificuldade nivelPrioritario;

    private NecessidadeRevisao necessidadeRevisao;
    private OrigemDecisao origemDecisao;
    /** true quando não há histórico real do aluno no conteúdo — a decisão é de partida. */
    private boolean dadosInsuficientes;
    /** true quando o modelo Weka não pôde ser usado (poucos dados ou falha no treino). */
    private boolean modeloIndisponivel;

    private AtributosRecomendacao atributos;

    /** Orientação para o professor quando a turma inteira está abaixo do limiar; null no caso normal. */
    private String alertaProfessor;

    public boolean isAlertaColetivo() {
        return alertaProfessor != null && !alertaProfessor.isBlank();
    }
}
