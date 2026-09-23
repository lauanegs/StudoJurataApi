package studojurata_api.machinelearning.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import studojurata_api.ia.model.RevisaoConteudo;
import studojurata_api.ia.repository.RevisaoConteudoRepository;
import studojurata_api.machinelearning.dto.AtributosRecomendacao;
import studojurata_api.model.Questao;
import studojurata_api.model.QuestaoAluno;
import studojurata_api.model.QuestaoConteudo;
import studojurata_api.model.Simulado;
import studojurata_api.model.enums.NivelDificuldade;
import studojurata_api.repository.QuestaoAlunoRepository;
import studojurata_api.repository.QuestaoConteudoRepository;

/**
 * Monta os atributos da recomendação a partir do que o sistema realmente
 * guarda. Não estima nem preenche lacuna com valor inventado: quando o dado não
 * existe, o atributo vai nulo (e o Weka recebe valor ausente, não zero).
 *
 * <p>Onde cada atributo nasce (levantamento do passo):
 * <ul>
 *   <li>respostas e acerto: {@code QuestaoAluno} (uma linha por questão da
 *       tentativa, com {@code acertou});</li>
 *   <li>data da resposta: {@code Simulado.dataInicio} (a tentativa é respondida
 *       dentro da janela do simulado) e, na falta dele, {@code QuestaoAluno.createdAt};</li>
 *   <li>dificuldade: {@code Questao.nivelDificuldade} (opcional no banco — questões
 *       sem nível não entram na média);</li>
 *   <li>conteúdo da questão: {@code QuestaoConteudo} (o vínculo é que liga a
 *       questão ao conteúdo);</li>
 *   <li>revisões: {@code RevisaoConteudo.quantidadeReforcos} e as datas de
 *       último/próximo reforço;</li>
 *   <li>tentativas: quantidade de {@code SimuladoAluno} distintos que tocaram o
 *       conteúdo.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class AtributosAlunoService {

    /** Quantas respostas mais recentes formam o "desempenho recente". */
    static final int JANELA_RECENTE = 5;

    private final QuestaoAlunoRepository questaoAlunoRepository;
    private final QuestaoConteudoRepository questaoConteudoRepository;
    private final RevisaoConteudoRepository revisaoConteudoRepository;

    public AtributosRecomendacao montar(Long alunoId, Long conteudoPlanoId, Long disciplinaId) {
        RevisaoConteudo revisao = conteudoPlanoId != null
                ? revisaoConteudoRepository.findByAlunoIdAndConteudoPlanoId(alunoId, conteudoPlanoId).orElse(null)
                : null;

        Integer reforcos = revisao != null && revisao.getQuantidadeReforcos() != null
                ? revisao.getQuantidadeReforcos()
                : 0;
        Integer diasDesdeRevisao = revisao != null ? diasDesde(revisao.getDataUltimoReforco()) : null;

        List<QuestaoAluno> respostas = questaoAlunoRepository.findBySimuladoAluno_AlunoId(alunoId);
        if (respostas.isEmpty()) {
            return new AtributosRecomendacao(alunoId, conteudoPlanoId, disciplinaId,
                    null, null, null, 0, reforcos, null, diasDesdeRevisao, null, 0);
        }

        Map<Long, Set<Long>> conteudosPorQuestao = conteudosPorQuestao(respostas);
        List<QuestaoAluno> doConteudo = respostas.stream()
                .filter(resposta -> conteudosDe(resposta, conteudosPorQuestao).contains(conteudoPlanoId))
                .toList();
        List<QuestaoAluno> daDisciplina = disciplinaId == null ? List.of() : respostas.stream()
                .filter(resposta -> pertenceADisciplina(resposta, disciplinaId))
                .toList();

        List<QuestaoAluno> doConteudoPorData = new ArrayList<>(doConteudo);
        doConteudoPorData.sort(Comparator.comparing(AtributosAlunoService::dataDaResposta,
                Comparator.nullsLast(Comparator.naturalOrder())).reversed());

        return new AtributosRecomendacao(
                alunoId,
                conteudoPlanoId,
                disciplinaId,
                percentual(doConteudo),
                percentual(doConteudoPorData.stream().limit(JANELA_RECENTE).toList()),
                percentual(daDisciplina),
                quantidadeTentativas(doConteudo),
                reforcos,
                diasDesde(dataMaisRecente(doConteudoPorData)),
                diasDesdeRevisao,
                dificuldadeMedia(doConteudo),
                doConteudo.size());
    }

    private Map<Long, Set<Long>> conteudosPorQuestao(List<QuestaoAluno> respostas) {
        List<Long> questaoIds = respostas.stream().map(resposta -> resposta.getQuestao().getId()).distinct().toList();
        Map<Long, Set<Long>> porQuestao = new HashMap<>();
        for (QuestaoConteudo vinculo : questaoConteudoRepository.findByQuestao_IdIn(questaoIds)) {
            if (vinculo.getConteudoPlano() == null) continue;
            porQuestao.computeIfAbsent(vinculo.getQuestao().getId(), chave -> new HashSet<>())
                    .add(vinculo.getConteudoPlano().getId());
        }
        return porQuestao;
    }

    private static Set<Long> conteudosDe(QuestaoAluno resposta, Map<Long, Set<Long>> conteudosPorQuestao) {
        return conteudosPorQuestao.getOrDefault(resposta.getQuestao().getId(), Set.of());
    }

    private static boolean pertenceADisciplina(QuestaoAluno resposta, Long disciplinaId) {
        Simulado simulado = simuladoDe(resposta);
        Questao questao = resposta.getQuestao();
        if (questao != null && questao.getDisciplina() != null) {
            return disciplinaId.equals(questao.getDisciplina().getId());
        }
        return simulado != null && simulado.getDisciplina() != null
                && disciplinaId.equals(simulado.getDisciplina().getId());
    }

    /** Percentual de acerto (0 a 1) do conjunto informado; nulo quando não há o que medir. */
    private static Double percentual(List<QuestaoAluno> respostas) {
        if (respostas.isEmpty()) return null;
        long acertos = respostas.stream().filter(resposta -> Boolean.TRUE.equals(resposta.getAcertou())).count();
        return (double) acertos / respostas.size();
    }

    private static Integer quantidadeTentativas(List<QuestaoAluno> respostas) {
        Set<Long> tentativas = new HashSet<>();
        for (QuestaoAluno resposta : respostas) {
            if (resposta.getSimuladoAluno() != null && resposta.getSimuladoAluno().getId() != null) {
                tentativas.add(resposta.getSimuladoAluno().getId());
            }
        }
        return tentativas.size();
    }

    /** 1 = FÁCIL, 2 = MÉDIA, 3 = DIFÍCIL; questões sem nível não entram na média. */
    private static Double dificuldadeMedia(List<QuestaoAluno> respostas) {
        List<Integer> pesos = respostas.stream()
                .map(resposta -> peso(resposta.getQuestao() != null ? resposta.getQuestao().getNivelDificuldade() : null))
                .filter(Objects::nonNull)
                .toList();
        if (pesos.isEmpty()) return null;
        return pesos.stream().mapToInt(Integer::intValue).average().orElse(0);
    }

    private static Integer peso(NivelDificuldade nivel) {
        if (nivel == null) return null;
        return switch (nivel) {
            case FACIL -> 1;
            case MEDIA -> 2;
            case DIFICIL -> 3;
        };
    }

    private static LocalDate dataMaisRecente(List<QuestaoAluno> respostasPorDataDesc) {
        return respostasPorDataDesc.isEmpty() ? null : dataDaResposta(respostasPorDataDesc.get(0));
    }

    private static LocalDate dataDaResposta(QuestaoAluno resposta) {
        Simulado simulado = simuladoDe(resposta);
        LocalDateTime quando = simulado != null && simulado.getDataInicio() != null
                ? simulado.getDataInicio()
                : resposta.getCreatedAt();
        return quando != null ? quando.toLocalDate() : null;
    }

    private static Simulado simuladoDe(QuestaoAluno resposta) {
        return resposta.getSimuladoAluno() != null ? resposta.getSimuladoAluno().getSimulado() : null;
    }

    private static Integer diasDesde(LocalDate data) {
        if (data == null) return null;
        long dias = ChronoUnit.DAYS.between(data, LocalDate.now());
        return (int) Math.max(0, dias);
    }
}
