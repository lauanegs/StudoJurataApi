package studojurata_api.machinelearning.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import studojurata_api.model.Alternativa;
import studojurata_api.model.ConteudoPlano;
import studojurata_api.model.Questao;
import studojurata_api.model.QuestaoConteudo;
import studojurata_api.model.enums.NivelDificuldade;
import studojurata_api.model.enums.StatusQuestao;
import studojurata_api.model.enums.StatusAtivoInativo;
import studojurata_api.repository.AlternativaRepository;
import studojurata_api.repository.QuestaoConteudoRepository;
import studojurata_api.repository.QuestaoRepository;

/**
 * Seleciona as questões candidatas de um simulado individual a partir do banco,
 * sem chamar a IA: quem decide se precisa de IA é o
 * {@link DecisaoGeracaoIAService}, a partir do resultado daqui.
 *
 * <p>Regras respeitadas: só questões da disciplina da recomendação (nunca de
 * outra), só questões aprovadas (mesma exigência do lançamento de simulado),
 * prioridade para os conteúdos recomendados, distribuição de dificuldade pedida,
 * máximo de 10 questões e no máximo 3 alternativas por questão. Questão já
 * respondida pelo aluno entra só se o banco não tiver como preencher de outra
 * forma — e o fato é reportado para a decisão de gerar por IA.
 *
 * <p>Conteúdo inativado (INATIVO é o soft-delete de {@code ConteudoPlano}) não
 * entra: questão que só está vinculada a conteúdo que saiu do planejamento não
 * serve de base para um simulado novo.
 */
@Service
@RequiredArgsConstructor
public class SelecaoQuestoesService {

    /** Limite pedagógico do simulado. */
    public static final int MAXIMO_QUESTOES = 10;
    /** Limite de alternativas por questão, igual ao adotado no banco de questões. */
    public static final int MAXIMO_ALTERNATIVAS = 3;

    private final QuestaoRepository questaoRepository;
    private final QuestaoConteudoRepository questaoConteudoRepository;
    private final AlternativaRepository alternativaRepository;

    public record Entrada(
            Long disciplinaId,
            List<Long> conteudosPrioritarios,
            Map<NivelDificuldade, Integer> distribuicaoDesejada,
            int quantidade,
            Set<Long> questoesJaRespondidas) {

        public Entrada {
            conteudosPrioritarios = conteudosPrioritarios != null ? conteudosPrioritarios : List.of();
            distribuicaoDesejada = distribuicaoDesejada != null ? distribuicaoDesejada : Map.of();
            questoesJaRespondidas = questoesJaRespondidas != null ? questoesJaRespondidas : Set.of();
        }
    }

    public record Resultado(
            List<Questao> selecionadas,
            /** Quanto faltou por nível de dificuldade desejado. */
            Map<NivelDificuldade, Integer> faltantes,
            /** Questões repetidas (já respondidas) que entraram por falta de inéditas. */
            int repetidas,
            int descartadasPorAlternativas,
            /** Cobertura completa + dificuldade pedida atendida + sem repetição forçada. */
            boolean bancoSuficiente) {}

    public Resultado selecionar(Entrada entrada) {
        int quantidade = Math.min(Math.max(entrada.quantidade(), 0), MAXIMO_QUESTOES);

        if (entrada.disciplinaId() == null) {
            return new Resultado(List.of(), copia(entrada.distribuicaoDesejada()), 0, 0, false);
        }

        List<Questao> daDisciplina = questaoRepository.findByDisciplina_IdIn(Set.of(entrada.disciplinaId())).stream()
                // Defensivo: a consulta já filtra por disciplina, mas a seleção nunca
                // pode devolver questão de disciplina diferente da recomendada.
                .filter(questao -> pertenceADisciplina(questao, entrada.disciplinaId()))
                .filter(questao -> questao.getStatus() == StatusQuestao.APROVADA)
                .toList();

        if (daDisciplina.isEmpty()) {
            return new Resultado(List.of(), copia(entrada.distribuicaoDesejada()), 0, 0, false);
        }

        List<Long> questaoIds = daDisciplina.stream().map(Questao::getId).toList();
        Map<Long, List<ConteudoPlano>> conteudosPorQuestao = conteudosPorQuestao(questaoIds);
        Map<Long, Long> alternativasPorQuestao = alternativasPorQuestao(questaoIds);

        List<Questao> comAlternativasValidas = daDisciplina.stream()
                .filter(questao -> temConteudoAtivo(conteudosPorQuestao.get(questao.getId())))
                .filter(questao -> alternativasPorQuestao.getOrDefault(questao.getId(), 0L) <= MAXIMO_ALTERNATIVAS)
                .toList();
        int descartadasPorAlternativas = daDisciplina.size() - comAlternativasValidas.size();

        List<Questao> ordenadas = new ArrayList<>(comAlternativasValidas);
        ordenadas.sort(Comparator
                .comparing((Questao questao) -> jaRespondida(questao, entrada) ? 1 : 0)
                .thenComparing(questao -> priorizaConteudo(questao, conteudosPorQuestao, entrada) ? 0 : 1)
                .thenComparing(Questao::getNivelDificuldade, Comparator.nullsLast(Comparator.naturalOrder())));

        List<Questao> selecionadas = new ArrayList<>();
        Set<Long> usadas = new HashSet<>();
        Map<NivelDificuldade, Integer> restante = copia(entrada.distribuicaoDesejada());

        // 1) distribuição pedida, com questões inéditas
        for (Map.Entry<NivelDificuldade, Integer> desejado : restante.entrySet()) {
            for (Questao questao : ordenadas) {
                if (desejado.getValue() <= 0 || selecionadas.size() >= quantidade) break;
                if (jaRespondida(questao, entrada) || usadas.contains(questao.getId())) continue;
                if (!Objects.equals(questao.getNivelDificuldade(), desejado.getKey())) continue;

                usadas.add(questao.getId());
                selecionadas.add(questao);
                desejado.setValue(desejado.getValue() - 1);
            }
        }

        // 2) completa o total com as demais inéditas (qualquer dificuldade)
        for (Questao questao : ordenadas) {
            if (selecionadas.size() >= quantidade) break;
            if (jaRespondida(questao, entrada) || !usadas.add(questao.getId())) continue;
            selecionadas.add(questao);
        }

        // 3) só então repete questão já respondida — e registra que repetiu
        int repetidas = 0;
        for (Questao questao : ordenadas) {
            if (selecionadas.size() >= quantidade) break;
            if (!jaRespondida(questao, entrada) || !usadas.add(questao.getId())) continue;
            selecionadas.add(questao);
            repetidas++;
        }

        Map<NivelDificuldade, Integer> faltantes = new LinkedHashMap<>();
        restante.forEach((nivel, quanto) -> {
            if (quanto > 0) faltantes.put(nivel, quanto);
        });

        boolean coberturaCompleta = selecionadas.size() >= quantidade;
        boolean dificuldadeAtendida = faltantes.isEmpty();
        boolean bancoSuficiente = coberturaCompleta && dificuldadeAtendida && repetidas == 0;

        return new Resultado(List.copyOf(selecionadas), faltantes, repetidas, descartadasPorAlternativas, bancoSuficiente);
    }

    /** questaoId -> conteúdos vinculados (uma consulta, sem N+1). */
    private Map<Long, List<ConteudoPlano>> conteudosPorQuestao(List<Long> questaoIds) {
        Map<Long, List<ConteudoPlano>> porQuestao = new HashMap<>();
        for (QuestaoConteudo vinculo : questaoConteudoRepository.findByQuestao_IdIn(questaoIds)) {
            if (vinculo.getConteudoPlano() == null) continue;
            porQuestao.computeIfAbsent(vinculo.getQuestao().getId(), chave -> new ArrayList<>())
                    .add(vinculo.getConteudoPlano());
        }
        return porQuestao;
    }

    private Map<Long, Long> alternativasPorQuestao(List<Long> questaoIds) {
        Map<Long, Long> contagem = new HashMap<>();
        for (Alternativa alternativa : alternativaRepository.findByQuestao_IdIn(questaoIds)) {
            if (alternativa.getQuestao() == null) continue;
            contagem.merge(alternativa.getQuestao().getId(), 1L, Long::sum);
        }
        return contagem;
    }

    private static boolean pertenceADisciplina(Questao questao, Long disciplinaId) {
        return questao.getDisciplina() != null && disciplinaId.equals(questao.getDisciplina().getId());
    }

    /**
     * Questão entra se algum conteúdo dela continua ativo. Questão sem vínculo
     * continua elegível: não há conteúdo inativado a excluir, e ela pertence à
     * disciplina recomendada.
     */
    private static boolean temConteudoAtivo(List<ConteudoPlano> conteudos) {
        if (conteudos == null || conteudos.isEmpty()) {
            return true;
        }
        return conteudos.stream().anyMatch(conteudo -> conteudo.getStatus() != StatusAtivoInativo.INATIVO);
    }

    private static boolean jaRespondida(Questao questao, Entrada entrada) {
        return entrada.questoesJaRespondidas().contains(questao.getId());
    }

    private static boolean priorizaConteudo(
            Questao questao, Map<Long, List<ConteudoPlano>> conteudosPorQuestao, Entrada entrada) {

        List<ConteudoPlano> conteudos = conteudosPorQuestao.get(questao.getId());
        return conteudos != null && conteudos.stream()
                .map(ConteudoPlano::getId)
                .anyMatch(entrada.conteudosPrioritarios()::contains);
    }

    private static Map<NivelDificuldade, Integer> copia(Map<NivelDificuldade, Integer> original) {
        Map<NivelDificuldade, Integer> copia = new LinkedHashMap<>();
        original.forEach(copia::put);
        return copia;
    }
}
