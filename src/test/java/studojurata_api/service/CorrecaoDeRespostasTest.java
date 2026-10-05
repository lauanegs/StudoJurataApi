package studojurata_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import studojurata_api.dto.FinalizarSimuladoRequest;
import studojurata_api.model.Alternativa;
import studojurata_api.model.Questao;
import studojurata_api.model.QuestaoAluno;
import studojurata_api.model.SimuladoAluno;
import studojurata_api.model.SimuladoQuestao;
import studojurata_api.model.enums.TipoQuestao;
import studojurata_api.repository.AlternativaRepository;
import studojurata_api.repository.QuestaoAlunoRepository;

@ExtendWith(MockitoExtension.class)
class CorrecaoDeRespostasTest {

    private static final long QUESTAO_ID = 10L;
    private static final long CERTA_ID = 11L;
    private static final long ERRADA_ID = 12L;

    @Mock private QuestaoAlunoRepository questaoAlunoRepository;
    @Mock private AlternativaRepository alternativaRepository;
    @InjectMocks private CorrecaoDeRespostas correcao;

    @Test
    @DisplayName("alternativa correta acerta e conta como respondida")
    void alternativaCorreta() {
        prepararQuestaoComAlternativas();

        CorrecaoDeRespostas.Resultado resultado = correcao.corrigir(tentativa(), List.of(vinculo(2.0)), pedido(CERTA_ID));

        assertThat(resultado).isEqualTo(new CorrecaoDeRespostas.Resultado(1, 1, 2.0, 2.0));
    }

    @Test
    @DisplayName("alternativa errada não pontua, mas conta como respondida")
    void alternativaErrada() {
        prepararQuestaoComAlternativas();

        CorrecaoDeRespostas.Resultado resultado = correcao.corrigir(tentativa(), List.of(vinculo(2.0)), pedido(ERRADA_ID));

        assertThat(resultado).isEqualTo(new CorrecaoDeRespostas.Resultado(0, 1, 0.0, 2.0));
    }

    @Test
    @DisplayName("questão em branco conta como erro e não como respondida")
    void emBranco() {
        given(questaoAlunoRepository.findFirstBySimuladoAlunoIdAndQuestaoId(any(), any())).willReturn(Optional.empty());

        CorrecaoDeRespostas.Resultado resultado = correcao.corrigir(tentativa(), List.of(vinculo(2.0)), pedido(null));

        assertThat(resultado).isEqualTo(new CorrecaoDeRespostas.Resultado(0, 0, 0.0, 2.0));
    }

    @Test
    @DisplayName("resposta já conferida durante a prova vale mais que a do pedido de finalização")
    void respostaJaConferidaPrevalece() {
        QuestaoAluno conferida = new QuestaoAluno();
        conferida.setRespondida(true);
        conferida.setAcertou(true);
        given(questaoAlunoRepository.findFirstBySimuladoAlunoIdAndQuestaoId(any(), any())).willReturn(Optional.of(conferida));

        // O pedido manda a alternativa errada, mas a conferência gravada foi acerto.
        CorrecaoDeRespostas.Resultado resultado = correcao.corrigir(tentativa(), List.of(vinculo(2.0)), pedido(ERRADA_ID));

        assertThat(resultado).isEqualTo(new CorrecaoDeRespostas.Resultado(1, 1, 2.0, 2.0));
        verify(questaoAlunoRepository).save(conferida);
    }

    private void prepararQuestaoComAlternativas() {
        given(questaoAlunoRepository.findFirstBySimuladoAlunoIdAndQuestaoId(any(), any())).willReturn(Optional.empty());
        lenient().when(alternativaRepository.findById(CERTA_ID)).thenReturn(Optional.of(alternativa(CERTA_ID, true)));
        lenient().when(alternativaRepository.findById(ERRADA_ID)).thenReturn(Optional.of(alternativa(ERRADA_ID, false)));
    }

    private static Alternativa alternativa(long id, boolean correta) {
        Alternativa alternativa = new Alternativa();
        alternativa.setId(id);
        alternativa.setCorreta(correta);
        return alternativa;
    }

    private static SimuladoQuestao vinculo(double pontuacao) {
        Questao questao = new Questao();
        questao.setId(QUESTAO_ID);
        questao.setTipo(TipoQuestao.ALTERNATIVAS);
        SimuladoQuestao vinculo = new SimuladoQuestao();
        vinculo.setQuestao(questao);
        vinculo.setPontuacao(pontuacao);
        return vinculo;
    }

    private static SimuladoAluno tentativa() {
        SimuladoAluno tentativa = new SimuladoAluno();
        tentativa.setId(1L);
        return tentativa;
    }

    private static FinalizarSimuladoRequest pedido(Long alternativaId) {
        FinalizarSimuladoRequest.Item item = new FinalizarSimuladoRequest.Item();
        item.setQuestaoId(QUESTAO_ID);
        item.setAlternativaId(alternativaId);
        FinalizarSimuladoRequest pedido = new FinalizarSimuladoRequest();
        pedido.setRespostas(List.of(item));
        return pedido;
    }
}
