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
import studojurata_api.model.enums.NivelDificuldade;
import studojurata_api.repository.ConteudoPlanoRepository;
import studojurata_api.repository.QuestaoAlunoRepository;
import studojurata_api.repository.QuestaoConteudoRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
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
 *
 * Desempenho por conteúdo e por dificuldade: taxa de acerto agregada por
 * conteúdo já não bastava pra decidir EM QUE NÍVEL reforçar (um aluno que
 * acerta o fácil e erra o difícil precisa de um reforço bem diferente de um
 * que erra o fácil e "acerta" o difícil, provavelmente no chute) — ver
 * sugerirNivelReforco.
 */
@Service
@RequiredArgsConstructor
public class RecomendacaoService {

    private static final double LIMIAR_BAIXO_APROVEITAMENTO = 0.6;

    /**
     * Amostra mínima de respostas, NUM NÍVEL de um conteúdo, antes de
     * confiar na taxa de acerto daquele nível — sem isso, uma única questão
     * errada por azar já bastaria pra apontar um nível como fraco.
     */
    private static final int AMOSTRA_MINIMA_NIVEL = 3;

    /**
     * Limiar por nível: errar o básico é mais grave que errar o avançado,
     * então o fácil exige uma taxa de acerto bem mais alta que o difícil
     * antes de ser considerado "ok".
     */
    private static final Map<NivelDificuldade, Double> LIMIAR_POR_NIVEL = new EnumMap<>(NivelDificuldade.class);
    static {
        LIMIAR_POR_NIVEL.put(NivelDificuldade.FACIL, 0.8);
        LIMIAR_POR_NIVEL.put(NivelDificuldade.MEDIA, 0.6);
        LIMIAR_POR_NIVEL.put(NivelDificuldade.DIFICIL, 0.5);
    }

    /**
     * Da base pro avançado: o reforço sempre aponta pro nível mais baixo que
     * ainda está fraco, mesmo que o aluno tenha ido bem em níveis mais
     * difíceis — acerto no difícil com o fácil furado é sinal de chute ou
     * inconsistência, não de domínio, e não faz sentido avançar sem a base.
     */
    private static final List<NivelDificuldade> ORDEM_NIVEIS =
            List.of(NivelDificuldade.FACIL, NivelDificuldade.MEDIA, NivelDificuldade.DIFICIL);

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

        Map<Long, Map<NivelDificuldade, double[]>> taxasPorConteudoENivel = calcularTaxaAcertoPorConteudoENivel(alunoId);
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
                // Só entra quando há questão com nível registrado pra esse
                // conteúdo — sem isso, fica null e GeracaoSimuladoIAService
                // cai no nível padrão (ver lá).
                Map<NivelDificuldade, double[]> porNivel = taxasPorConteudoENivel.get(entry.getKey());
                dto.setNivelPrioritario(porNivel != null ? determinarNivelPrioritario(porNivel) : null);
            }
        }

        return new ArrayList<>(porConteudo.values());
    }

    /**
     * Nível de dificuldade a usar num simulado de reforço pra este
     * aluno+conteúdo — o mais baixo dos três em que a taxa de acerto (com
     * amostra suficiente) está abaixo do limiar daquele nível. Retorna null
     * quando não há dado suficiente em nenhum nível pra decidir (caller cai
     * pro nível padrão, ver GeracaoSimuladoIAService).
     */
    public NivelDificuldade sugerirNivelReforco(Long alunoId, Long conteudoPlanoId) {
        Map<NivelDificuldade, double[]> porNivel = calcularTaxaAcertoPorConteudoENivel(alunoId).get(conteudoPlanoId);
        if (porNivel == null) return null;
        return determinarNivelPrioritario(porNivel);
    }

    private NivelDificuldade determinarNivelPrioritario(Map<NivelDificuldade, double[]> porNivel) {
        for (NivelDificuldade nivel : ORDEM_NIVEIS) {
            double[] contadores = porNivel.get(nivel);
            if (contadores == null) continue;

            double acertos = contadores[0];
            double total = contadores[1];
            if (total < AMOSTRA_MINIMA_NIVEL) continue;

            double taxa = acertos / total;
            if (taxa < LIMIAR_POR_NIVEL.get(nivel)) return nivel;
        }
        return null;
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

    /**
     * conteudoPlanoId -> [quantidadeAcertos, quantidadeRespondidas] — taxa
     * geral, agregando todos os níveis de dificuldade juntos (comportamento
     * original, usado pro gatilho de BAIXO_APROVEITAMENTO). Mantida separada
     * de calcularTaxaAcertoPorConteudoENivel de propósito: nem toda questão
     * tem nível preenchido (nivelDificuldade é opcional), então a taxa geral
     * não pode depender disso — só o nível prioritário do reforço depende.
     */
    private Map<Long, double[]> calcularTaxaAcertoPorConteudo(Long alunoId) {
        List<QuestaoAluno> respostas = questaoAlunoRepository.findBySimuladoAluno_AlunoId(alunoId);
        if (respostas.isEmpty()) {
            return Map.of();
        }

        Map<Long, List<Long>> conteudosPorQuestao = mapearConteudosPorQuestao(respostas);

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

    /** conteudoPlanoId -> nível -> [quantidadeAcertos, quantidadeRespondidas] — só questões com nível preenchido. */
    private Map<Long, Map<NivelDificuldade, double[]>> calcularTaxaAcertoPorConteudoENivel(Long alunoId) {
        List<QuestaoAluno> respostas = questaoAlunoRepository.findBySimuladoAluno_AlunoId(alunoId);
        if (respostas.isEmpty()) {
            return Map.of();
        }

        Map<Long, List<Long>> conteudosPorQuestao = mapearConteudosPorQuestao(respostas);

        Map<Long, Map<NivelDificuldade, double[]>> agregado = new HashMap<>();
        for (QuestaoAluno resposta : respostas) {
            List<Long> conteudoIds = conteudosPorQuestao.get(resposta.getQuestao().getId());
            if (conteudoIds == null) continue;

            NivelDificuldade nivel = resposta.getQuestao().getNivelDificuldade();
            if (nivel == null) continue;

            for (Long conteudoId : conteudoIds) {
                double[] contadores = agregado
                        .computeIfAbsent(conteudoId, k -> new EnumMap<>(NivelDificuldade.class))
                        .computeIfAbsent(nivel, k -> new double[2]);
                contadores[1] += 1;
                if (Boolean.TRUE.equals(resposta.getAcertou())) {
                    contadores[0] += 1;
                }
            }
        }
        return agregado;
    }

    /** questaoId -> conteudoPlanoIds vinculados, resolvido só pras questões respondidas passadas. */
    private Map<Long, List<Long>> mapearConteudosPorQuestao(List<QuestaoAluno> respostas) {
        List<Long> questaoIds = respostas.stream().map(qa -> qa.getQuestao().getId()).distinct().toList();
        List<QuestaoConteudo> vinculos = questaoConteudoRepository.findByQuestao_IdIn(questaoIds);
        Map<Long, List<Long>> conteudosPorQuestao = new HashMap<>();
        for (QuestaoConteudo vinculo : vinculos) {
            conteudosPorQuestao
                    .computeIfAbsent(vinculo.getQuestao().getId(), k -> new ArrayList<>())
                    .add(vinculo.getConteudoPlano().getId());
        }
        return conteudosPorQuestao;
    }
}
