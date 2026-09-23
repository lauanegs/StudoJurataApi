package studojurata_api.machinelearning.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import studojurata_api.machinelearning.dto.AtributosRecomendacao;
import studojurata_api.model.AlunoTurma;
import studojurata_api.model.ConteudoPlano;
import studojurata_api.model.QuestaoAluno;
import studojurata_api.model.QuestaoConteudo;
import studojurata_api.model.TurmaDisciplina;
import studojurata_api.model.enums.StatusMatricula;
import studojurata_api.repository.AlunoTurmaRepository;
import studojurata_api.repository.QuestaoAlunoRepository;
import studojurata_api.repository.QuestaoConteudoRepository;

/**
 * Alerta de baixo aproveitamento <b>coletivo</b>: quando a maioria dos alunos de
 * uma turma está abaixo do limiar no conteúdo, a resposta pedagógica é revisão
 * em sala (e um simulado tradicional), não gerar um simulado individual por IA
 * para cada aluno.
 *
 * <p>Existe separado da recomendação individual porque a leitura é inversa: aqui
 * a pergunta é sobre a turma, e a decisão resultante bloqueia — não dispara — a
 * geração individual.
 *
 * <p>Usa consultas em lote (questões do conteúdo e respostas dessas questões):
 * a checagem percorre a turma inteira sem uma consulta por aluno.
 */
@Service
@RequiredArgsConstructor
public class BaixoAproveitamentoColetivoService {

    /** Mesmo limiar do sinal individual, para não existirem dois critérios de "baixo". */
    static final double LIMIAR_BAIXO = AtributosRecomendacao.LIMIAR_BAIXO_DESEMPENHO;
    /** Amostra mínima por aluno para entrar na conta da turma. */
    static final int MINIMO_RESPOSTAS_POR_ALUNO = 3;
    /** Abaixo disso não existe "maioria da turma" — é turma pequena demais. */
    static final int MINIMO_ALUNOS_AVALIADOS = 4;
    /** Mais da metade dos alunos avaliados abaixo do limiar. */
    static final double PROPORCAO_MAIORIA = 0.5;

    private final AlunoTurmaRepository alunoTurmaRepository;
    private final QuestaoConteudoRepository questaoConteudoRepository;
    private final QuestaoAlunoRepository questaoAlunoRepository;

    /**
     * Mensagem de orientação ao professor quando a maioria da turma está abaixo
     * do limiar neste conteúdo; {@code null} quando o caso não se aplica (turma
     * pequena, sem dados suficientes, ou desempenho coletivo adequado).
     */
    public String avaliar(ConteudoPlano conteudo) {
        TurmaDisciplina vinculo = turmaDisciplinaDo(conteudo);
        if (vinculo == null || vinculo.getTurma() == null || vinculo.getTurma().getId() == null) {
            // Sem turma não existe "a maioria da turma" — o simulado é individual.
            return null;
        }

        List<AlunoTurma> matriculas = alunoTurmaRepository
                .findByTurmaIdAndStatus(vinculo.getTurma().getId(), StatusMatricula.ATIVA);
        if (matriculas.size() < MINIMO_ALUNOS_AVALIADOS) {
            return null;
        }

        Set<Long> questoesDoConteudo = questoesDoConteudo(conteudo.getId());
        if (questoesDoConteudo.isEmpty()) {
            return null;
        }

        Map<Long, double[]> respostasPorAluno = new HashMap<>();
        for (QuestaoAluno resposta : questaoAlunoRepository.findByQuestao_IdIn(questoesDoConteudo)) {
            Long alunoId = resposta.getSimuladoAluno() != null && resposta.getSimuladoAluno().getAluno() != null
                    ? resposta.getSimuladoAluno().getAluno().getId()
                    : null;
            if (alunoId == null) continue;

            double[] contadores = respostasPorAluno.computeIfAbsent(alunoId, chave -> new double[2]);
            contadores[1]++;
            if (Boolean.TRUE.equals(resposta.getAcertou())) {
                contadores[0]++;
            }
        }

        long avaliados = 0;
        long abaixo = 0;
        for (double[] contadores : respostasPorAluno.values()) {
            if (contadores[1] < MINIMO_RESPOSTAS_POR_ALUNO) continue;
            avaliados++;
            if (contadores[0] / contadores[1] < LIMIAR_BAIXO) {
                abaixo++;
            }
        }

        if (avaliados < MINIMO_ALUNOS_AVALIADOS || (double) abaixo / avaliados <= PROPORCAO_MAIORIA) {
            return null;
        }

        return "Baixo aproveitamento coletivo: " + abaixo + " de " + avaliados + " alunos avaliados estão abaixo de "
                + (int) Math.round(LIMIAR_BAIXO * 100) + "% em \"" + conteudo.getTitulo() + "\" ("
                + vinculo.getTurma().getTitulo() + "). Recomendamos revisão em sala e um simulado tradicional para a turma; "
                + "simulados individuais por IA não serão gerados automaticamente.";
    }

    private Set<Long> questoesDoConteudo(Long conteudoPlanoId) {
        Set<Long> questoes = new HashSet<>();
        for (QuestaoConteudo vinculo : questaoConteudoRepository.findByConteudoPlano_Id(conteudoPlanoId)) {
            if (vinculo.getQuestao() != null && vinculo.getQuestao().getId() != null) {
                questoes.add(vinculo.getQuestao().getId());
            }
        }
        return questoes;
    }

    private static TurmaDisciplina turmaDisciplinaDo(ConteudoPlano conteudo) {
        return conteudo.getPlanoEnsino() != null ? conteudo.getPlanoEnsino().getTurmaDisciplina() : null;
    }
}
