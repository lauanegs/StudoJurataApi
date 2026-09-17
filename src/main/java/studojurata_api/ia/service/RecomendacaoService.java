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
 * Recomenda reforço por dois sinais: repetição espaçada vencida e
 * aproveitamento abaixo de 60% no conteúdo. Não aciona geração sozinho.
 *
 * A taxa por conteúdo diz SE reforçar; a taxa por nível de dificuldade
 * (sugerirNivelReforco) diz EM QUE NÍVEL, já que errar o fácil e acertar o
 * difícil pede um reforço bem diferente do caso inverso.
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
                Map<NivelDificuldade, double[]> porNivel = taxasPorConteudoENivel.get(entry.getKey());
                dto.setNivelPrioritario(porNivel != null ? determinarNivelPrioritario(porNivel) : null);
            }
        }

        return new ArrayList<>(porConteudo.values());
    }

    /**
     * O nível mais baixo em que a taxa de acerto, com amostra suficiente, está
     * abaixo do limiar daquele nível. Null quando nenhum nível tem dados
     * suficientes.
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
     * conteudoPlanoId -> [quantidadeAcertos, quantidadeRespondidas], todos os
     * níveis juntos. Separada da agregação por nível porque nivelDificuldade é
     * opcional e a taxa geral não pode depender dele.
     */
    private Map<Long, double[]> calcularTaxaAcertoPorConteudo(Long alunoId) {
        RespostasComConteudo dados = carregarRespostasComConteudo(alunoId);
        if (dados.respostas().isEmpty()) {
            return Map.of();
        }

        Map<Long, double[]> agregado = new HashMap<>();
        for (QuestaoAluno resposta : dados.respostas()) {
            List<Long> conteudoIds = dados.conteudosPorQuestao().get(resposta.getQuestao().getId());
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
        RespostasComConteudo dados = carregarRespostasComConteudo(alunoId);
        if (dados.respostas().isEmpty()) {
            return Map.of();
        }

        Map<Long, Map<NivelDificuldade, double[]>> agregado = new HashMap<>();
        for (QuestaoAluno resposta : dados.respostas()) {
            List<Long> conteudoIds = dados.conteudosPorQuestao().get(resposta.getQuestao().getId());
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

    /** Respostas do aluno e os conteúdos vinculados a cada questão — busca compartilhada pelas duas agregações acima. */
    private record RespostasComConteudo(List<QuestaoAluno> respostas, Map<Long, List<Long>> conteudosPorQuestao) {}

    private RespostasComConteudo carregarRespostasComConteudo(Long alunoId) {
        List<QuestaoAluno> respostas = questaoAlunoRepository.findBySimuladoAluno_AlunoId(alunoId);
        Map<Long, List<Long>> conteudosPorQuestao = respostas.isEmpty() ? Map.of() : mapearConteudosPorQuestao(respostas);
        return new RespostasComConteudo(respostas, conteudosPorQuestao);
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
