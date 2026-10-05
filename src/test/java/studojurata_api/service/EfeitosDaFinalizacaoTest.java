package studojurata_api.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import studojurata_api.ia.service.RevisaoConteudoService;
import studojurata_api.machinelearning.service.MachineLearningRecomendacaoService;
import studojurata_api.model.Aluno;
import studojurata_api.model.Questao;
import studojurata_api.model.Simulado;
import studojurata_api.model.SimuladoAluno;
import studojurata_api.model.SimuladoQuestao;
import studojurata_api.repository.QuestaoConteudoRepository;
import studojurata_api.service.gamificacao.PontuacaoAlunoService;

@ExtendWith(MockitoExtension.class)
class EfeitosDaFinalizacaoTest {

    private static final long ALUNO_ID = 7L;

    @Mock private NotaService notaService;
    @Mock private AuditLogService auditLogService;
    @Mock private PontuacaoAlunoService pontuacaoAlunoService;
    @Mock private RevisaoConteudoService revisaoConteudoService;
    @Mock private QuestaoConteudoRepository questaoConteudoRepository;
    @Mock private MachineLearningRecomendacaoService machineLearningRecomendacaoService;
    @InjectMocks private EfeitosDaFinalizacao efeitos;

    @Test
    @DisplayName("prova toda respondida concede as moedas e registra o desfecho")
    void provaCompleta() {
        efeitos.aplicar(tentativa(), new CorrecaoDeRespostas.Resultado(1, 2, 1.0, 2.0), List.of(vinculo(1L), vinculo(2L)));

        verify(pontuacaoAlunoService).concederMoedas(ALUNO_ID, PontuacaoAlunoService.MOEDAS_POR_SIMULADO_CONCLUIDO);
        verify(machineLearningRecomendacaoService).registrarResultado(any(), any(), anyDouble(), any());
    }

    @Test
    @DisplayName("prova com questão em branco não rende moedas")
    void provaIncompleta() {
        efeitos.aplicar(tentativa(), new CorrecaoDeRespostas.Resultado(1, 1, 1.0, 2.0), List.of(vinculo(1L), vinculo(2L)));

        verify(pontuacaoAlunoService, never()).concederMoedas(any(), anyInt());
    }

    @Test
    @DisplayName("prova toda em branco não gera desfecho para o modelo")
    void provaEmBranco() {
        efeitos.aplicar(tentativa(), new CorrecaoDeRespostas.Resultado(0, 0, 0.0, 2.0), List.of(vinculo(1L), vinculo(2L)));

        verify(machineLearningRecomendacaoService, never()).registrarResultado(any(), any(), anyDouble(), any());
    }

    private static SimuladoAluno tentativa() {
        Aluno aluno = new Aluno();
        aluno.setId(ALUNO_ID);
        Simulado simulado = new Simulado();
        simulado.setId(300L);
        SimuladoAluno tentativa = new SimuladoAluno();
        tentativa.setAluno(aluno);
        tentativa.setSimulado(simulado);
        return tentativa;
    }

    private static SimuladoQuestao vinculo(long questaoId) {
        Questao questao = new Questao();
        questao.setId(questaoId);
        SimuladoQuestao vinculo = new SimuladoQuestao();
        vinculo.setQuestao(questao);
        return vinculo;
    }
}
