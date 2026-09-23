package studojurata_api.machinelearning.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import studojurata_api.machinelearning.dto.AtributosRecomendacao;
import studojurata_api.machinelearning.model.RecomendacaoSimulado;
import studojurata_api.machinelearning.model.enums.NecessidadeRevisao;
import studojurata_api.machinelearning.repository.RecomendacaoSimuladoRepository;
import studojurata_api.model.ConteudoPlano;

/**
 * Treino e predição reais do Weka: o modelo só existe com o mínimo de registros
 * com desfecho observado, e o rótulo vem do desempenho posterior do aluno.
 */
@ExtendWith(MockitoExtension.class)
class WekaModeloServiceTest {

    @Mock private RecomendacaoSimuladoRepository repository;

    private WekaModeloService service;

    @BeforeEach
    void setUp() {
        service = new WekaModeloService(repository);
    }

    @Test
    @DisplayName("com exemplos rotulados suficientes, treina e classifica os dois extremos")
    void treinaEClassifica() {
        given(repository.findByPercentualAcertoPosteriorIsNotNull()).willReturn(registrosRotulados());

        assertThat(service.modeloDisponivel()).isTrue();
        assertThat(service.classificar(atributosBaixos())).contains(NecessidadeRevisao.REVISAR_AGORA);
        assertThat(service.classificar(atributosAltos())).contains(NecessidadeRevisao.DOMINIO_ESTAVEL);
    }

    @Test
    @DisplayName("sem o mínimo de exemplos, não treina e devolve vazio (fallback)")
    void semExemplosSuficientes() {
        given(repository.findByPercentualAcertoPosteriorIsNotNull())
                .willReturn(registrosRotulados().subList(0, WekaModeloService.MINIMO_EXEMPLOS - 1));

        assertThat(service.treinar(exemplos().subList(0, WekaModeloService.MINIMO_EXEMPLOS - 1))).isEmpty();
        assertThat(service.modeloDisponivel()).isFalse();
        assertThat(service.classificar(atributosBaixos())).isEmpty();
    }

    @Test
    @DisplayName("invalidar o modelo força reconstrução a partir dos registros reais")
    void invalidarReconstroi() {
        given(repository.findByPercentualAcertoPosteriorIsNotNull()).willReturn(registrosRotulados());

        assertThat(service.modeloDisponivel()).isTrue();
        service.invalidarModelo();

        assertThat(service.classificar(atributosAltos())).contains(NecessidadeRevisao.DOMINIO_ESTAVEL);
        assertThat(service.exemplosDisponiveis()).isEqualTo(registrosRotulados().size());
    }

    @Test
    @DisplayName("falha ao ler os dados de treino não derruba a recomendação: segue sem modelo")
    void falhaNoTreinoUsaFallback() {
        given(repository.findByPercentualAcertoPosteriorIsNotNull())
                .willThrow(new IllegalStateException("banco indisponível no momento"));

        // Nada é lançado: a decisão continua pela regra determinística.
        assertThat(service.classificar(atributosBaixos())).isEmpty();
        assertThat(service.modeloDisponivel()).isFalse();
    }

    // --- dados de teste (features no formato real; rótulo vem do desfecho) ----

    private static List<WekaModeloService.Exemplo> exemplos() {
        List<WekaModeloService.Exemplo> exemplos = new ArrayList<>();
        registrosRotulados().forEach(registro -> exemplos.add(new WekaModeloService.Exemplo(
                atributosDe(registro),
                registro.getPercentualAcertoPosterior() < 0.6
                        ? NecessidadeRevisao.REVISAR_AGORA
                        : NecessidadeRevisao.DOMINIO_ESTAVEL)));
        return exemplos;
    }

    /** 15 recomendações com desfecho ruim e 15 com desfecho bom. */
    private static List<RecomendacaoSimulado> registrosRotulados() {
        List<RecomendacaoSimulado> registros = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            registros.add(registro(0.2 + i * 0.01, 0.3, 0.3, 5, 1, 2.0, 8, 0.35));
            registros.add(registro(0.9 + i * 0.005, 0.9, 0.9, 2, 3, 2.5, 12, 0.95));
        }
        return registros;
    }

    private static RecomendacaoSimulado registro(double conteudo, double recente, double disciplina,
            int diasResposta, int revisoes, double dificuldade, int respostas, double desfecho) {
        ConteudoPlano plano = new ConteudoPlano();
        plano.setId(2L);

        RecomendacaoSimulado registro = new RecomendacaoSimulado();
        registro.setConteudoPlano(plano);
        registro.setPercentualAcertoConteudo(conteudo);
        registro.setPercentualAcertoRecente(recente);
        registro.setPercentualAcertoDisciplina(disciplina);
        registro.setQuantidadeTentativas(2);
        registro.setQuantidadeRevisoes(revisoes);
        registro.setDiasDesdeUltimaResposta(diasResposta);
        registro.setDiasDesdeUltimaRevisao(revisoes > 0 ? diasResposta : null);
        registro.setDificuldadeMediaRespondida(dificuldade);
        registro.setQuantidadeQuestoesRespondidas(respostas);
        registro.setPercentualAcertoPosterior(desfecho);
        return registro;
    }

    private static AtributosRecomendacao atributosDe(RecomendacaoSimulado registro) {
        return new AtributosRecomendacao(1L, 2L, 3L, registro.getPercentualAcertoConteudo(),
                registro.getPercentualAcertoRecente(), registro.getPercentualAcertoDisciplina(),
                registro.getQuantidadeTentativas(), registro.getQuantidadeRevisoes(),
                registro.getDiasDesdeUltimaResposta(), registro.getDiasDesdeUltimaRevisao(),
                registro.getDificuldadeMediaRespondida(), registro.getQuantidadeQuestoesRespondidas());
    }

    private static AtributosRecomendacao atributosBaixos() {
        return new AtributosRecomendacao(1L, 2L, 3L, 0.25, 0.3, 0.35, 2, 1, 5, 5, 2.0, 8);
    }

    private static AtributosRecomendacao atributosAltos() {
        return new AtributosRecomendacao(1L, 2L, 3L, 0.95, 0.9, 0.9, 2, 3, 2, 2, 2.5, 12);
    }
}
