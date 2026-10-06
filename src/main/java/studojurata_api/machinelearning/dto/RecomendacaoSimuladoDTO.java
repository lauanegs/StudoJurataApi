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
import studojurata_api.model.simulado.Questao;
import studojurata_api.model.enums.NivelDificuldade;

@Getter
@Setter
public class RecomendacaoSimuladoDTO {
    private Long id;
    private Long alunoId;
    private Long disciplinaId;
    private Long conteudoPlanoId;
    private String conteudoTitulo;

    private int quantidadeRecomendada;
    private List<Long> conteudosPrioritarios = List.of();
    private Map<NivelDificuldade, Integer> distribuicaoDeDificuldade = new LinkedHashMap<>();
    private List<Questao> questoesCandidatas = List.of();

    private DecisaoGeracao decisaoGeracao = DecisaoGeracao.REUTILIZAR_BANCO;
    private boolean necessitaGeracaoIA;
    private Set<MotivoRecomendacao> motivos = Set.of();
    private String motivo;
    private int intervaloRevisaoDias;
    private NivelDificuldade nivelPrioritario;

    private NecessidadeRevisao necessidadeRevisao;
    private OrigemDecisao origemDecisao;
    private boolean dadosInsuficientes;
    private boolean modeloIndisponivel;

    private AtributosRecomendacao atributos;

    private String alertaProfessor;

    public boolean isAlertaColetivo() {
        return alertaProfessor != null && !alertaProfessor.isBlank();
    }
}
