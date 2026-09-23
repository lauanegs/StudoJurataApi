package studojurata_api.machinelearning.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import studojurata_api.ia.model.enums.MotivoRecomendacao;
import studojurata_api.machinelearning.model.enums.DecisaoGeracao;

class DecisaoGeracaoIAServiceTest {

    private final DecisaoGeracaoIAService service = new DecisaoGeracaoIAService();

    @Test
    @DisplayName("simulado de turma nunca aciona IA")
    void simuladoDeTurmaNaoAcionaIA() {
        assertThat(service.decidir(false, Set.of(MotivoRecomendacao.BAIXO_APROVEITAMENTO), false))
                .isEqualTo(DecisaoGeracao.REUTILIZAR_BANCO);
    }

    @Test
    @DisplayName("sem gatilho pedagógico, reutiliza o banco")
    void semGatilhoPedagogico() {
        assertThat(service.decidir(true, Set.of(), false)).isEqualTo(DecisaoGeracao.REUTILIZAR_BANCO);
        assertThat(service.decidir(true, null, false)).isEqualTo(DecisaoGeracao.REUTILIZAR_BANCO);
    }

    @Test
    @DisplayName("com banco suficiente, reutiliza as questões existentes")
    void bancoSuficienteReutiliza() {
        assertThat(service.decidir(true, Set.of(MotivoRecomendacao.REPETICAO_ESPACADA), true))
                .isEqualTo(DecisaoGeracao.REUTILIZAR_BANCO);
    }

    @Test
    @DisplayName("repetição espaçada ou baixo aproveitamento com banco insuficiente gera por IA")
    void bancoInsuficienteGeraPorIA() {
        assertThat(service.decidir(true, Set.of(MotivoRecomendacao.REPETICAO_ESPACADA), false))
                .isEqualTo(DecisaoGeracao.GERAR_POR_IA);
        assertThat(service.decidir(true, Set.of(MotivoRecomendacao.BAIXO_APROVEITAMENTO), false))
                .isEqualTo(DecisaoGeracao.GERAR_POR_IA);
        assertThat(service.decidir(true,
                Set.of(MotivoRecomendacao.BAIXO_APROVEITAMENTO, MotivoRecomendacao.REPETICAO_ESPACADA), false))
                .isEqualTo(DecisaoGeracao.GERAR_POR_IA);
    }
}
