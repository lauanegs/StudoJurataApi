package studojurata_api.machinelearning.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import studojurata_api.ia.model.RevisaoConteudo;
import studojurata_api.ia.repository.RevisaoConteudoRepository;
import studojurata_api.machinelearning.dto.AtributosRecomendacao;
import studojurata_api.model.Aluno;
import studojurata_api.model.ConteudoPlano;
import studojurata_api.model.Disciplina;
import studojurata_api.model.Questao;
import studojurata_api.model.QuestaoAluno;
import studojurata_api.model.QuestaoConteudo;
import studojurata_api.model.Simulado;
import studojurata_api.model.SimuladoAluno;
import studojurata_api.model.enums.NivelDificuldade;
import studojurata_api.repository.QuestaoAlunoRepository;
import studojurata_api.repository.QuestaoConteudoRepository;

/** Atributos montados só com dados reais; ausência de dado vira nulo, nunca estimativa. */
@ExtendWith(MockitoExtension.class)
class AtributosAlunoServiceTest {

    private static final long ALUNO_ID = 7L;
    private static final long DISC = 10L;
    private static final long CONTEUDO = 100L;
    private static final long OUTRO_CONTEUDO = 200L;

    @Mock private QuestaoAlunoRepository questaoAlunoRepository;
    @Mock private QuestaoConteudoRepository questaoConteudoRepository;
    @Mock private RevisaoConteudoRepository revisaoConteudoRepository;

    private AtributosAlunoService service;

    @BeforeEach
    void setUp() {
        service = new AtributosAlunoService(questaoAlunoRepository, questaoConteudoRepository, revisaoConteudoRepository);
    }

    @Test
    @DisplayName("sem respostas: atributos ficam nulos e só as revisões são conhecidas")
    void semRespostas() {
        given(questaoAlunoRepository.findBySimuladoAluno_AlunoId(ALUNO_ID)).willReturn(List.of());
        given(revisaoConteudoRepository.findByAlunoIdAndConteudoPlanoId(ALUNO_ID, CONTEUDO))
                .willReturn(Optional.of(revisao(2, LocalDate.now().minusDays(10))));

        AtributosRecomendacao atributos = service.montar(ALUNO_ID, CONTEUDO, DISC);

        assertThat(atributos.temHistorico()).isFalse();
        assertThat(atributos.percentualAcertoConteudo()).isNull();
        assertThat(atributos.percentualAcertoRecente()).isNull();
        assertThat(atributos.diasDesdeUltimaResposta()).isNull();
        assertThat(atributos.quantidadeQuestoesRespondidas()).isZero();
        assertThat(atributos.quantidadeRevisoes()).isEqualTo(2);
        assertThat(atributos.diasDesdeUltimaRevisao()).isEqualTo(10);
        assertThat(atributos.baixoDesempenho()).isFalse();
    }

    @Test
    @DisplayName("com respostas: percentuais, dificuldade média, tentativas e dias desde a última resposta")
    void comRespostas() {
        LocalDate ultimaResposta = LocalDate.now().minusDays(12);
        List<QuestaoAluno> respostas = List.of(
                resposta(1L, CONTEUDO, NivelDificuldade.FACIL, true, 1L, ultimaResposta.minusDays(5)),
                resposta(2L, CONTEUDO, NivelDificuldade.FACIL, false, 1L, ultimaResposta.minusDays(5)),
                resposta(3L, CONTEUDO, NivelDificuldade.MEDIA, true, 2L, ultimaResposta),
                resposta(4L, OUTRO_CONTEUDO, NivelDificuldade.DIFICIL, false, 2L, ultimaResposta));

        given(questaoAlunoRepository.findBySimuladoAluno_AlunoId(ALUNO_ID)).willReturn(respostas);
        given(questaoConteudoRepository.findByQuestao_IdIn(List.of(1L, 2L, 3L, 4L)))
                .willReturn(List.of(vinculo(1L, CONTEUDO), vinculo(2L, CONTEUDO), vinculo(3L, CONTEUDO),
                        vinculo(4L, OUTRO_CONTEUDO)));
        given(revisaoConteudoRepository.findByAlunoIdAndConteudoPlanoId(ALUNO_ID, CONTEUDO))
                .willReturn(Optional.of(revisao(1, LocalDate.now().minusDays(30))));

        AtributosRecomendacao atributos = service.montar(ALUNO_ID, CONTEUDO, DISC);

        assertThat(atributos.temHistorico()).isTrue();
        // 2 acertos em 3 questões do conteúdo.
        assertThat(atributos.percentualAcertoConteudo()).isEqualTo(2.0 / 3.0);
        // Na disciplina entram as 4 respostas (3 do conteúdo + 1 de outro conteúdo).
        assertThat(atributos.percentualAcertoDisciplina()).isEqualTo(0.5);
        assertThat(atributos.quantidadeQuestoesRespondidas()).isEqualTo(3);
        assertThat(atributos.quantidadeTentativas()).isEqualTo(2);
        assertThat(atributos.dificuldadeMediaRespondida()).isEqualTo((1 + 1 + 2) / 3.0);
        assertThat(atributos.diasDesdeUltimaResposta()).isEqualTo(12);
        assertThat(atributos.quantidadeRevisoes()).isEqualTo(1);
        assertThat(atributos.diasDesdeUltimaRevisao()).isEqualTo(30);
    }

    @Test
    @DisplayName("questões sem nível de dificuldade não entram na média")
    void semNivel() {
        given(questaoAlunoRepository.findBySimuladoAluno_AlunoId(ALUNO_ID))
                .willReturn(List.of(resposta(1L, CONTEUDO, null, true, 1L, LocalDate.now())));
        given(questaoConteudoRepository.findByQuestao_IdIn(List.of(1L)))
                .willReturn(List.of(vinculo(1L, CONTEUDO)));
        given(revisaoConteudoRepository.findByAlunoIdAndConteudoPlanoId(ALUNO_ID, CONTEUDO))
                .willReturn(Optional.empty());

        AtributosRecomendacao atributos = service.montar(ALUNO_ID, CONTEUDO, DISC);

        assertThat(atributos.dificuldadeMediaRespondida()).isNull();
        assertThat(atributos.percentualAcertoConteudo()).isEqualTo(1.0);
        assertThat(atributos.quantidadeRevisoes()).isZero();
    }

    // --- helpers -----------------------------------------------------------

    private static QuestaoAluno resposta(long questaoId, long conteudoId, NivelDificuldade nivel, boolean acertou,
            long tentativaId, LocalDate data) {
        Questao questao = new Questao();
        questao.setId(questaoId);
        questao.setNivelDificuldade(nivel);

        Disciplina disciplina = new Disciplina();
        disciplina.setId(DISC);
        questao.setDisciplina(disciplina);

        Simulado simulado = new Simulado();
        simulado.setId(tentativaId);
        simulado.setDisciplina(disciplina);
        simulado.setDataInicio(LocalDateTime.of(data, java.time.LocalTime.of(8, 0)));

        Aluno aluno = new Aluno();
        aluno.setId(ALUNO_ID);

        SimuladoAluno tentativa = new SimuladoAluno();
        tentativa.setId(tentativaId);
        tentativa.setAluno(aluno);
        tentativa.setSimulado(simulado);

        QuestaoAluno resposta = new QuestaoAluno();
        resposta.setQuestao(questao);
        resposta.setSimuladoAluno(tentativa);
        resposta.setAcertou(acertou);
        return resposta;
    }

    private static QuestaoConteudo vinculo(long questaoId, long conteudoId) {
        Questao questao = new Questao();
        questao.setId(questaoId);

        ConteudoPlano conteudo = new ConteudoPlano();
        conteudo.setId(conteudoId);

        QuestaoConteudo vinculo = new QuestaoConteudo();
        vinculo.setQuestao(questao);
        vinculo.setConteudoPlano(conteudo);
        return vinculo;
    }

    private static RevisaoConteudo revisao(int reforcos, LocalDate ultimoReforco) {
        RevisaoConteudo revisao = new RevisaoConteudo();
        revisao.setQuantidadeReforcos(reforcos);
        revisao.setDataUltimoReforco(ultimoReforco);
        return revisao;
    }
}
