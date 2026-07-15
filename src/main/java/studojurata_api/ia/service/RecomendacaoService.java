package studojurata_api.ia.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import studojurata_api.ia.dto.RecomendacaoDTO;
import studojurata_api.ia.model.RevisaoConteudo;
import studojurata_api.ia.model.enums.MotivoRecomendacao;
import studojurata_api.ia.repository.RevisaoConteudoRepository;
import studojurata_api.model.ConteudoPlano;
import studojurata_api.model.QuestaoAluno;
import studojurata_api.model.QuestaoConteudo;
import studojurata_api.repository.ConteudoPlanoRepository;
import studojurata_api.repository.QuestaoAlunoRepository;
import studojurata_api.repository.QuestaoConteudoRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Gera recomendações de reforço para um aluno (item "recomendações" desta
 * etapa), combinando dois sinais já aprovados na Análise Crítica:
 *
 * - item 1.5: conteúdos cuja repetição espaçada está devida
 *   (RevisaoConteudo.dataProximoReforco já atingida);
 * - item 1.4: conteúdos em que o aproveitamento do aluno está abaixo de 60%
 *   ("haverá situações onde a nota inferior a 60% de aproveitamento do aluno
 *   ia requerir a produção de mais um simulado").
 *
 * Ver também item 7.4 (métricas que deveriam alimentar a IA): esta é a
 * primeira consumidora concreta do histórico granular por
 * conteúdo/questão/aluno estruturado no módulo de simulados (QuestaoAluno).
 *
 * O resultado é apenas uma lista priorizável: não aciona sozinho nenhuma
 * geração de simulado — isso é uma decisão explícita do professor (ou de um
 * job futuro), via GeracaoSimuladoIAService, respeitando o item 1.4 quanto à
 * exigência de revisão humana antes da liberação ao aluno.
 */
@Service
@RequiredArgsConstructor
public class RecomendacaoService {

    private static final double LIMIAR_BAIXO_APROVEITAMENTO = 0.6;

    private final RevisaoConteudoRepository revisaoConteudoRepository;
    private final QuestaoAlunoRepository questaoAlunoRepository;
    private final QuestaoConteudoRepository questaoConteudoRepository;
    private final ConteudoPlanoRepository conteudoPlanoRepository;

    public List<RecomendacaoDTO> recomendarParaAluno(Long alunoId) {
        Map<Long, RecomendacaoDTO> porConteudo = new LinkedHashMap<>();

        for (RevisaoConteudo revisao : revisaoConteudoRepository.findByAlunoIdAndDataProximoReforcoLessThanEqual(alunoId, LocalDate.now())) {
            RecomendacaoDTO dto = obterOuCriar(porConteudo, alunoId, revisao.getConteudoPlano());
            dto.getMotivos().add(MotivoRecomendacao.REPETICAO_ESPACADA);
            dto.setDataProximoReforco(revisao.getDataProximoReforco());
        }

        for (Map.Entry<Long, double[]> entry : calcularTaxaAcertoPorConteudo(alunoId).entrySet()) {
            double acertos = entry.getValue()[0];
            double total = entry.getValue()[1];
            if (total <= 0) continue;
            double taxa = acertos / total;
            if (taxa < LIMIAR_BAIXO_APROVEITAMENTO) {
                ConteudoPlano conteudo = conteudoPlanoRepository.findById(entry.getKey()).orElse(null);
                if (conteudo == null) continue;
                RecomendacaoDTO dto = obterOuCriar(porConteudo, alunoId, conteudo);
                dto.getMotivos().add(MotivoRecomendacao.BAIXO_APROVEITAMENTO);
                dto.setTaxaAcerto(taxa);
            }
        }

        return new ArrayList<>(porConteudo.values());
    }

    private RecomendacaoDTO obterOuCriar(Map<Long, RecomendacaoDTO> porConteudo, Long alunoId, ConteudoPlano conteudo) {
        return porConteudo.computeIfAbsent(conteudo.getId(), id -> {
            RecomendacaoDTO dto = new RecomendacaoDTO();
            dto.setAlunoId(alunoId);
            dto.setConteudoPlanoId(conteudo.getId());
            dto.setConteudoTitulo(conteudo.getTitulo());
            dto.setMotivos(new java.util.LinkedHashSet<>());
            return dto;
        });
    }

    /** conteudoPlanoId -> [quantidadeAcertos, quantidadeRespondidas] */
    private Map<Long, double[]> calcularTaxaAcertoPorConteudo(Long alunoId) {
        List<QuestaoAluno> respostas = questaoAlunoRepository.findBySimuladoAluno_AlunoId(alunoId);
        if (respostas.isEmpty()) {
            return Map.of();
        }

        List<Long> questaoIds = respostas.stream().map(qa -> qa.getQuestao().getId()).distinct().toList();
        List<QuestaoConteudo> vinculos = questaoConteudoRepository.findByQuestao_IdIn(questaoIds);
        Map<Long, List<Long>> conteudosPorQuestao = new HashMap<>();
        for (QuestaoConteudo vinculo : vinculos) {
            conteudosPorQuestao
                    .computeIfAbsent(vinculo.getQuestao().getId(), k -> new ArrayList<>())
                    .add(vinculo.getConteudoPlano().getId());
        }

        Map<Long, double[]> agregado = new HashMap<>();
        for (QuestaoAluno resposta : respostas) {
            List<Long> conteudoIds = conteudosPorQuestao.get(resposta.getQuestao().getId());
            if (conteudoIds == null) continue;
            for (Long conteudoId : conteudoIds) {
                double[] contadores = agregado.computeIfAbsent(conteudoId, k -> new double[2]);
                contadores[1] += 1;
                if (Boolean.TRUE.equals(resposta.getAcertou())) {
                    contadores[0] += 1;
                }
            }
        }
        return agregado;
    }
}
