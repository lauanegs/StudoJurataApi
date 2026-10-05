package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import studojurata_api.ia.model.enums.NivelDominio;
import studojurata_api.ia.service.RevisaoConteudoService;
import studojurata_api.machinelearning.service.MachineLearningRecomendacaoService;
import studojurata_api.model.SimuladoAluno;
import studojurata_api.model.SimuladoQuestao;
import studojurata_api.model.enums.AcaoAuditoria;
import studojurata_api.repository.QuestaoConteudoRepository;
import studojurata_api.service.gamificacao.PontuacaoAlunoService;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Consequências de uma tentativa finalizada: nota da disciplina, desfecho da
 * recomendação (rótulo do modelo), moedas e agenda da revisão espaçada. Roda na
 * mesma transação da finalização, depois de a tentativa estar salva.
 */
@Component
@RequiredArgsConstructor
public class EfeitosDaFinalizacao {

    private final NotaService notaService;
    private final AuditLogService auditLogService;
    private final PontuacaoAlunoService pontuacaoAlunoService;
    private final RevisaoConteudoService revisaoConteudoService;
    private final QuestaoConteudoRepository questaoConteudoRepository;
    private final MachineLearningRecomendacaoService machineLearningRecomendacaoService;

    /** @return dias até a próxima revisão espaçada, ou null quando nenhum conteúdo tem revisão agendada. */
    public Integer aplicar(SimuladoAluno salvo, CorrecaoDeRespostas.Resultado correcao, List<SimuladoQuestao> questoes) {
        recalcularNotaDaDisciplina(salvo);

        // Aprendizado progressivo: o desempenho real desta tentativa vira o
        // desfecho das recomendações ainda em aberto que tocaram estas questões —
        // é esse número que serve de rótulo para o próximo treino do modelo.
        registrarDesfechoDaRecomendacao(salvo, correcao, questoes);

        // Moedas por concluir, independente da nota — mas só quando o aluno
        // respondeu todas as questões: prova entregue em branco/incompleta não rende moedas.
        if (!questoes.isEmpty() && correcao.respondidas() == questoes.size()) {
            pontuacaoAlunoService.concederMoedas(salvo.getAluno().getId(),
                    PontuacaoAlunoService.MOEDAS_POR_SIMULADO_CONCLUIDO);
        }

        return registrarRevisaoEspacada(salvo.getAluno().getId(), questoes, correcao);
    }

    /**
     * Percentual real de acerto da tentativa, associado à recomendação que a
     * explica. Tentativa sem nenhuma resposta (tudo em branco) não gera rótulo:
     * não houve desempenho observável, e um zero artificial ensinaria o modelo a
     * tratar abandono como falta de domínio.
     */
    private void registrarDesfechoDaRecomendacao(
            SimuladoAluno simuladoAluno, CorrecaoDeRespostas.Resultado correcao, List<SimuladoQuestao> questoes) {

        if (correcao.respondidas() == 0) {
            return;
        }

        double percentual = correcao.pontuacaoTotal() > 0
                ? correcao.pontuacaoObtida() / correcao.pontuacaoTotal()
                : 0;
        List<Long> questaoIds = questoes.stream().map(vinculo -> vinculo.getQuestao().getId()).toList();
        Long simuladoId = simuladoAluno.getSimulado() != null ? simuladoAluno.getSimulado().getId() : null;

        machineLearningRecomendacaoService.registrarResultado(
                simuladoAluno.getAluno().getId(), simuladoId, percentual, questaoIds);
    }

    /** Simulado.turma pode ser nula em ESPECIFICO; nesse caso não há como escopar a nota. */
    private void recalcularNotaDaDisciplina(SimuladoAluno salvo) {
        Long disciplinaId = salvo.getSimulado().getDisciplina() != null ? salvo.getSimulado().getDisciplina().getId() : null;
        Long turmaId = salvo.getSimulado().getTurma() != null ? salvo.getSimulado().getTurma().getId() : null;
        if (disciplinaId != null && turmaId != null) {
            notaService.recalcular(salvo.getAluno().getId(), disciplinaId, turmaId);
        } else {
            // Registrado em AuditLog para o Administrador identificar por que o
            // aluno não tem nota nessa disciplina.
            auditLogService.registrar("SimuladoAluno", salvo.getId(), AcaoAuditoria.ATUALIZACAO,
                    "Nota da disciplina NÃO recalculada: disciplina ou turma do simulado ausente "
                            + "(simulado sem turma vinculada). Corrija o cadastro do simulado.");
        }
    }

    /** Qualquer simulado finalizado alimenta a repetição espaçada. */
    private Integer registrarRevisaoEspacada(Long alunoId, List<SimuladoQuestao> questoes, CorrecaoDeRespostas.Resultado correcao) {
        double percentualAcerto = correcao.pontuacaoTotal() > 0
                ? (correcao.pontuacaoObtida() / correcao.pontuacaoTotal()) * 100
                : 0;
        LocalDate proximaRevisao = registrarReforcoNosConteudos(alunoId, questoes, percentualAcerto);
        return proximaRevisao != null
                ? (int) ChronoUnit.DAYS.between(LocalDate.now(), proximaRevisao)
                : null;
    }

    /** Mesmos limiares de nivelDesempenho no front (utils/desempenho.ts). */
    private static NivelDominio nivelDominioPeloPercentual(double percentual) {
        if (percentual < 40) return NivelDominio.BAIXO;
        if (percentual < 70) return NivelDominio.MEDIO;
        return NivelDominio.ALTO;
    }

    /**
     * Cada conteúdo tocado avança uma vez por finalização, não uma vez por
     * questão. Retorna a próxima data mais próxima, ou null se nenhum conteúdo
     * tem revisão agendada.
     */
    private LocalDate registrarReforcoNosConteudos(Long alunoId, List<SimuladoQuestao> questoes, double percentualAcerto) {
        List<Long> questaoIds = questoes.stream().map(sq -> sq.getQuestao().getId()).toList();
        if (questaoIds.isEmpty()) return null;

        Set<Long> conteudoPlanoIds = new HashSet<>();
        for (var vinculo : questaoConteudoRepository.findByQuestao_IdIn(questaoIds)) {
            if (vinculo.getConteudoPlano() != null) {
                conteudoPlanoIds.add(vinculo.getConteudoPlano().getId());
            }
        }

        NivelDominio nivel = nivelDominioPeloPercentual(percentualAcerto);
        LocalDate maisProxima = null;
        for (Long conteudoPlanoId : conteudoPlanoIds) {
            var revisao = revisaoConteudoService.registrarReforco(alunoId, conteudoPlanoId, nivel);
            LocalDate data = revisao.getDataProximoReforco();
            if (data != null && (maisProxima == null || data.isBefore(maisProxima))) {
                maisProxima = data;
            }
        }
        return maisProxima;
    }
}
