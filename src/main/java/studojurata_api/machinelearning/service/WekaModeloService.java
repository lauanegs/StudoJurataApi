package studojurata_api.machinelearning.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import studojurata_api.machinelearning.dto.AtributosRecomendacao;
import studojurata_api.machinelearning.model.RecomendacaoSimulado;
import studojurata_api.machinelearning.model.enums.NecessidadeRevisao;
import studojurata_api.machinelearning.repository.RecomendacaoSimuladoRepository;
import weka.classifiers.Classifier;
import weka.classifiers.trees.J48;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

/** Estrutura de treino e predição do Weka */
@Service
@RequiredArgsConstructor
public class WekaModeloService {
    private static final Logger log = LoggerFactory.getLogger(WekaModeloService.class);

    /** Mínimo de exemplos rotulados para tentar treinar */
    public static final int MINIMO_EXEMPLOS = 30;

    /** Validade do modelo em memória antes de reconstruir a partir dos registros */
    private static final Duration VALIDADE_MODELO = Duration.ofHours(1);

    private static final String ATRIBUTO_CONTEUDO = "percentualAcertoConteudo";
    private static final String ATRIBUTO_RECENTE = "percentualAcertoRecente";
    private static final String ATRIBUTO_DISCIPLINA = "percentualAcertoDisciplina";
    private static final String ATRIBUTO_TENTATIVAS = "quantidadeTentativas";
    private static final String ATRIBUTO_REVISOES = "quantidadeRevisoes";
    private static final String ATRIBUTO_DIAS_RESPOSTA = "diasDesdeUltimaResposta";
    private static final String ATRIBUTO_DIAS_REVISAO = "diasDesdeUltimaRevisao";
    private static final String ATRIBUTO_DIFICULDADE = "dificuldadeMediaRespondida";
    private static final String ATRIBUTO_QUESTOES = "quantidadeQuestoesRespondidas";
    private static final String ATRIBUTO_CLASSE = "necessidadeRevisao";

    private final RecomendacaoSimuladoRepository repository;

    private final AtomicReference<Modelo> cache = new AtomicReference<>();

    /** Exemplo rotulado para treino — feature snapshot + desfecho observado */
    public record Exemplo(AtributosRecomendacao atributos, NecessidadeRevisao rotulo) {}

    private record Modelo(Classifier classificador, Instances estrutura, Instant treinadoEm, int exemplos) {}

    private record Treino(Classifier classificador, Instances dados) {}

    /** Classifica a necessidade de revisão com o modelo treinado */
    public Optional<NecessidadeRevisao> classificar(AtributosRecomendacao atributos) {
        try {
            Modelo modelo = modeloAtual();
            if (modelo == null) {
                return Optional.empty();
            }

            Instance instancia = paraInstancia(atributos, modelo.estrutura());
            double predicao = modelo.classificador().classifyInstance(instancia);
            String rotulo = modelo.estrutura().classAttribute().value((int) predicao);
            return Optional.of(NecessidadeRevisao.valueOf(rotulo));
        } catch (Exception erro) {
            // Qualquer problema do modelo (treino, dados, predição) só desliga o Weka nesta chamada
            log.warn("Falha ao classificar com o modelo Weka; seguindo com a regra determinística.", erro);
            return Optional.empty();
        }
    }

    /** Novo desfecho registrado invalida o modelo: o próximo uso reconstrói com os dados novos */
    public void invalidarModelo() {
        cache.set(null);
    }

    /** Treina uma árvore J48 */
    public Optional<Classifier> treinar(List<Exemplo> exemplos) {
        return treinarComDataset(exemplos).map(Treino::classificador);
    }

    private Optional<Treino> treinarComDataset(List<Exemplo> exemplos) {
        if (exemplos == null || exemplos.size() < MINIMO_EXEMPLOS) {
            return Optional.empty();
        }

        try {
            Instances dados = montarDataset(exemplos);
            Classifier classificador = new J48();
            classificador.buildClassifier(dados);
            return Optional.of(new Treino(classificador, dados));
        } catch (Exception erro) {
            log.warn("Não foi possível treinar o modelo Weka; seguindo com a regra determinística.", erro);
            return Optional.empty();
        }
    }

    private Modelo modeloAtual() {
        Modelo atual = cache.get();
        if (atual != null && !expirado(atual)) {
            return atual.classificador() != null ? atual : null;
        }

        return reconstruir();
    }

    private boolean expirado(Modelo modelo) {
        return modelo.treinadoEm().plus(VALIDADE_MODELO).isBefore(Instant.now());
    }

    private Modelo reconstruir() {
        List<Exemplo> exemplos = new ArrayList<>();
        try {
            for (RecomendacaoSimulado registro : repository.findByPercentualAcertoPosteriorIsNotNull()) {
                NecessidadeRevisao rotulo = rotuloDoDesfecho(registro.getPercentualAcertoPosterior());
                if (rotulo == null) continue;
                exemplos.add(new Exemplo(atributosDe(registro), rotulo));
            }
        } catch (Exception erro) {
            // Sem leitura dos registros não há treino
            log.warn("Não foi possível ler os registros de treino do modelo Weka.", erro);
            cache.set(new Modelo(null, estruturaVazia(), Instant.now(), 0));
            return null;
        }

        Optional<Treino> treinado = treinarComDataset(exemplos);
        Modelo modelo = treinado
                .map(treino -> new Modelo(treino.classificador(), treino.dados(), Instant.now(), exemplos.size()))
                .orElseGet(() -> new Modelo(null, estruturaVazia(), Instant.now(), exemplos.size()));

        cache.set(modelo);
        return modelo.classificador() != null ? modelo : null;
    }

    /** Rótulo do treino = necessidade observada depois da recomendação (desfecho real) */
    private static NecessidadeRevisao rotuloDoDesfecho(Double percentualPosterior) {
        if (percentualPosterior == null) return null;
        if (percentualPosterior < AtributosRecomendacao.LIMIAR_BAIXO_DESEMPENHO) return NecessidadeRevisao.REVISAR_AGORA;
        if (percentualPosterior < AtributosRecomendacao.LIMIAR_BOM_DESEMPENHO) return NecessidadeRevisao.REVISAR_EM_BREVE;
        return NecessidadeRevisao.DOMINIO_ESTAVEL;
    }

    private static AtributosRecomendacao atributosDe(RecomendacaoSimulado registro) {
        return new AtributosRecomendacao(
                registro.getAluno() != null ? registro.getAluno().getId() : null,
                registro.getConteudoPlano() != null ? registro.getConteudoPlano().getId() : null,
                registro.getDisciplina() != null ? registro.getDisciplina().getId() : null,
                registro.getPercentualAcertoConteudo(),
                registro.getPercentualAcertoRecente(),
                registro.getPercentualAcertoDisciplina(),
                registro.getQuantidadeTentativas(),
                registro.getQuantidadeRevisoes(),
                registro.getDiasDesdeUltimaResposta(),
                registro.getDiasDesdeUltimaRevisao(),
                registro.getDificuldadeMediaRespondida(),
                registro.getQuantidadeQuestoesRespondidas());
    }

    private static Instances montarDataset(List<Exemplo> exemplos) {
        Instances dados = estruturaVazia();
        dados.setClassIndex(dados.numAttributes() - 1);

        for (Exemplo exemplo : exemplos) {
            dados.add(paraInstancia(exemplo.atributos(), dados, exemplo.rotulo()));
        }
        return dados;
    }

    /** Cabeçalho do dataset (sem instâncias), reaproveitado para montar cada linha */
    private static Instances estruturaVazia() {
        ArrayList<Attribute> atributos = new ArrayList<>();
        atributos.add(new Attribute(ATRIBUTO_CONTEUDO));
        atributos.add(new Attribute(ATRIBUTO_RECENTE));
        atributos.add(new Attribute(ATRIBUTO_DISCIPLINA));
        atributos.add(new Attribute(ATRIBUTO_TENTATIVAS));
        atributos.add(new Attribute(ATRIBUTO_REVISOES));
        atributos.add(new Attribute(ATRIBUTO_DIAS_RESPOSTA));
        atributos.add(new Attribute(ATRIBUTO_DIAS_REVISAO));
        atributos.add(new Attribute(ATRIBUTO_DIFICULDADE));
        atributos.add(new Attribute(ATRIBUTO_QUESTOES));
        atributos.add(new Attribute(ATRIBUTO_CLASSE, rotulos()));

        Instances estrutura = new Instances("recomendacao_simulado_individual", atributos, 0);
        estrutura.setClassIndex(estrutura.numAttributes() - 1);
        return estrutura;
    }

    private static List<String> rotulos() {
        return List.of(
                NecessidadeRevisao.REVISAR_AGORA.name(),
                NecessidadeRevisao.REVISAR_EM_BREVE.name(),
                NecessidadeRevisao.DOMINIO_ESTAVEL.name());
    }

    private static Instance paraInstancia(AtributosRecomendacao atributos, Instances estrutura) {
        return paraInstancia(atributos, estrutura, null);
    }

    private static Instance paraInstancia(AtributosRecomendacao atributos, Instances estrutura, NecessidadeRevisao rotulo) {
        DenseInstance instancia = new DenseInstance(estrutura.numAttributes());
        instancia.setDataset(estrutura);

        // Valor ausente entra como missing ("?"), não como zero
        preencher(instancia, 0, atributos.percentualAcertoConteudo());
        preencher(instancia, 1, atributos.percentualAcertoRecente());
        preencher(instancia, 2, atributos.percentualAcertoDisciplina());
        preencher(instancia, 3, atributos.quantidadeTentativas());
        preencher(instancia, 4, atributos.quantidadeRevisoes());
        preencher(instancia, 5, atributos.diasDesdeUltimaResposta());
        preencher(instancia, 6, atributos.diasDesdeUltimaRevisao());
        preencher(instancia, 7, atributos.dificuldadeMediaRespondida());
        preencher(instancia, 8, atributos.quantidadeQuestoesRespondidas());

        if (rotulo != null) {
            instancia.setValue(estrutura.classIndex(), rotulo.name());
        }
        return instancia;
    }

    private static void preencher(DenseInstance instancia, int indice, Number valor) {
        if (valor == null) {
            instancia.setMissing(indice);
            return;
        }
        instancia.setValue(indice, valor.doubleValue());
    }
}
