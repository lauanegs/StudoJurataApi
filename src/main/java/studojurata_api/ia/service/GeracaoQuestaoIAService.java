package studojurata_api.ia.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import studojurata_api.ia.client.GeminiIndisponivelException;
import studojurata_api.ia.client.GeminiQuestaoClient;
import studojurata_api.ia.dto.AlternativaGeradaDTO;
import studojurata_api.ia.dto.GeminiQuestaoGeradaDTO;
import studojurata_api.ia.model.HistoricoGeracaoIA;
import studojurata_api.ia.model.enums.OrigemResultadoGeracao;
import studojurata_api.ia.repository.HistoricoGeracaoIARepository;
import studojurata_api.model.Alternativa;
import studojurata_api.model.ConteudoPlano;
import studojurata_api.model.Disciplina;
import studojurata_api.model.PlanoEnsino;
import studojurata_api.model.Questao;
import studojurata_api.model.QuestaoConteudo;
import studojurata_api.model.Simulado;
import studojurata_api.model.enums.NivelDificuldade;
import studojurata_api.model.enums.OrigemQuestao;
import studojurata_api.model.enums.StatusQuestao;
import studojurata_api.model.enums.TipoQuestao;
import studojurata_api.repository.AlternativaRepository;
import studojurata_api.repository.ConteudoPlanoRepository;
import studojurata_api.repository.QuestaoConteudoRepository;
import studojurata_api.repository.QuestaoRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Coração do módulo de IA: obtém, para um conteúdo/dificuldade/tipo/
 * quantidade solicitados, um conjunto de Questao prontas para compor um
 * simulado — combinando três fontes, nesta ordem de prioridade (ver itens
 * 3.3 e 9.3 da Análise Crítica):
 *
 * 1. CACHE: questões já aprovadas anteriormente para o mesmo conteúdo e
 *    dificuldade (reaproveitamento — evita chamar a IA de novo para
 *    conteúdo/dificuldade já cobertos, o que também economiza tokens/custo,
 *    relevante no MVP conforme o item 9.3);
 * 2. GEMINI: o que faltar é gerado pela API do Gemini. Questões novas nascem
 *    com status PENDENTE (origem IA) — exigem revisão humana do professor
 *    antes de poderem ser reaproveitadas ou de o simulado que as contém ser
 *    lançado (ver StatusQuestao e SimuladoService.lancar, já existentes);
 * 3. FALLBACK: se a chamada ao Gemini falhar, tenta completar a quantidade
 *    ainda faltante com outras questões já aprovadas do mesmo conteúdo,
 *    relaxando o filtro de dificuldade/tipo exatos se necessário (nunca
 *    relaxando o requisito de já estar APROVADA).
 *
 * Cada chamada gera um registro em HistoricoGeracaoIA, sucesso ou falha.
 */
@Service
@RequiredArgsConstructor
public class GeracaoQuestaoIAService {

    private final QuestaoRepository questaoRepository;
    private final QuestaoConteudoRepository questaoConteudoRepository;
    private final AlternativaRepository alternativaRepository;
    private final ConteudoPlanoRepository conteudoPlanoRepository;
    private final HistoricoGeracaoIARepository historicoRepository;
    private final GeminiQuestaoClient geminiQuestaoClient;

    @Transactional
    public List<Questao> gerar(Long conteudoPlanoId, NivelDificuldade nivel, TipoQuestao tipo, int quantidade, Simulado simuladoVinculado) {
        if (quantidade <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "quantidade deve ser maior que zero.");
        }

        ConteudoPlano conteudo = conteudoPlanoRepository.findById(conteudoPlanoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conteúdo do plano de ensino não encontrado."));
        Disciplina disciplina = resolverDisciplina(conteudo);

        List<QuestaoConteudo> vinculosExistentes = questaoConteudoRepository.findByConteudoPlano_Id(conteudoPlanoId);

        // 1) Cache: questões já aprovadas, mesmo conteúdo + mesma dificuldade/tipo.
        List<Questao> selecionadas = new ArrayList<>();
        Set<Long> idsUsados = new LinkedHashSet<>();
        for (QuestaoConteudo vinculo : vinculosExistentes) {
            if (selecionadas.size() >= quantidade) break;
            Questao questao = vinculo.getQuestao();
            if (questao.getStatus() == StatusQuestao.APROVADA
                    && (nivel == null || questao.getNivelDificuldade() == nivel)
                    && (tipo == null || questao.getTipo() == tipo)
                    && idsUsados.add(questao.getId())) {
                selecionadas.add(questao);
            }
        }
        int reaproveitadasCache = selecionadas.size();

        // 2) Gemini para completar o que falta.
        int faltanteAntesGemini = quantidade - selecionadas.size();
        int geradasGemini = 0;
        String erroGemini = null;
        long tempoRespostaMs = 0L;

        if (faltanteAntesGemini > 0) {
            try {
                long inicio = System.currentTimeMillis();
                String textoConteudo = montarTextoConteudo(conteudo);
                List<GeminiQuestaoGeradaDTO> geradas = geminiQuestaoClient.gerarQuestoes(textoConteudo, nivel, tipo, faltanteAntesGemini);
                tempoRespostaMs = System.currentTimeMillis() - inicio;

                for (GeminiQuestaoGeradaDTO dto : geradas) {
                    Questao questao = persistirQuestaoGerada(dto, disciplina, conteudo, nivel, tipo);
                    selecionadas.add(questao);
                    geradasGemini++;
                }
            } catch (GeminiIndisponivelException e) {
                erroGemini = e.getMessage();
            }
        }

        // 3) Fallback: banco de questões aprovadas do mesmo conteúdo, relaxando dificuldade/tipo.
        int faltanteAposGemini = quantidade - selecionadas.size();
        int fallbackBanco = 0;
        if (faltanteAposGemini > 0) {
            for (QuestaoConteudo vinculo : vinculosExistentes) {
                if (selecionadas.size() >= quantidade) break;
                Questao questao = vinculo.getQuestao();
                if (questao.getStatus() == StatusQuestao.APROVADA && idsUsados.add(questao.getId())) {
                    selecionadas.add(questao);
                    fallbackBanco++;
                }
            }
        }

        OrigemResultadoGeracao origemResultado = calcularOrigemResultado(
                reaproveitadasCache, geradasGemini, fallbackBanco, erroGemini, selecionadas.isEmpty());

        registrarHistorico(conteudo, disciplina, simuladoVinculado, nivel, tipo, quantidade,
                reaproveitadasCache, geradasGemini, fallbackBanco, origemResultado, erroGemini, tempoRespostaMs);

        if (selecionadas.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Não foi possível obter questões para este conteúdo: IA indisponível" +
                            (erroGemini != null ? " (" + erroGemini + ")" : "") +
                            " e não há questões aprovadas suficientes no banco para fallback.");
        }

        return selecionadas;
    }

    private Disciplina resolverDisciplina(ConteudoPlano conteudo) {
        PlanoEnsino planoEnsino = conteudo.getPlanoEnsino();
        if (planoEnsino != null && planoEnsino.getTurmaDisciplina() != null) {
            return planoEnsino.getTurmaDisciplina().getDisciplina();
        }
        return null;
    }

    private String montarTextoConteudo(ConteudoPlano conteudo) {
        StringBuilder sb = new StringBuilder();
        if (conteudo.getTitulo() != null) sb.append(conteudo.getTitulo()).append("\n");
        if (conteudo.getDescricao() != null) sb.append(conteudo.getDescricao());
        return sb.toString().trim();
    }

    private Questao persistirQuestaoGerada(GeminiQuestaoGeradaDTO dto, Disciplina disciplina, ConteudoPlano conteudo,
                                            NivelDificuldade nivel, TipoQuestao tipo) {
        Questao questao = new Questao();
        questao.setEnunciado(dto.getEnunciado());
        questao.setTipo(tipo);
        questao.setDisciplina(disciplina);
        questao.setNivelDificuldade(nivel);
        questao.setOrigem(OrigemQuestao.IA);
        // Questões de origem IA nascem PENDENTE: exigem revisão humana antes de reaproveitamento (item 7.3).
        questao.setStatus(StatusQuestao.PENDENTE);
        questao = questaoRepository.save(questao);

        int ordem = 1;
        if (dto.getAlternativas() != null) {
            for (AlternativaGeradaDTO altDto : dto.getAlternativas()) {
                Alternativa alternativa = new Alternativa();
                alternativa.setQuestao(questao);
                alternativa.setTexto(altDto.getTexto());
                alternativa.setCorreta(altDto.isCorreta());
                alternativa.setOrdem(ordem++);
                alternativaRepository.save(alternativa);
            }
        }

        QuestaoConteudo vinculo = new QuestaoConteudo();
        vinculo.setQuestao(questao);
        vinculo.setConteudoPlano(conteudo);
        questaoConteudoRepository.save(vinculo);

        return questao;
    }

    private OrigemResultadoGeracao calcularOrigemResultado(int cache, int gemini, int fallback, String erroGemini, boolean vazio) {
        if (vazio) return OrigemResultadoGeracao.FALHA;
        if (erroGemini != null && fallback > 0) return OrigemResultadoGeracao.FALLBACK_BANCO;
        if (gemini > 0 && cache > 0) return OrigemResultadoGeracao.MISTA;
        if (gemini > 0) return OrigemResultadoGeracao.GEMINI;
        return OrigemResultadoGeracao.CACHE;
    }

    private void registrarHistorico(ConteudoPlano conteudo, Disciplina disciplina, Simulado simulado,
                                     NivelDificuldade nivel, TipoQuestao tipo, int quantidadeSolicitada,
                                     int cache, int gemini, int fallback, OrigemResultadoGeracao origemResultado,
                                     String erroGemini, long tempoRespostaMs) {
        HistoricoGeracaoIA historico = new HistoricoGeracaoIA();
        historico.setConteudoPlano(conteudo);
        historico.setDisciplina(disciplina);
        historico.setSimulado(simulado);
        historico.setNivelDificuldade(nivel);
        historico.setTipoQuestao(tipo);
        historico.setQuantidadeSolicitada(quantidadeSolicitada);
        historico.setQuantidadeReaproveitadaCache(cache);
        historico.setQuantidadeGeradaGemini(gemini);
        historico.setQuantidadeFallbackBanco(fallback);
        historico.setOrigemResultado(origemResultado);
        historico.setModeloUtilizado(geminiQuestaoClient.getModelo());
        historico.setTempoRespostaMs(tempoRespostaMs);
        historico.setMensagemErro(erroGemini);
        historico.setDataGeracao(LocalDateTime.now());
        historicoRepository.save(historico);
    }
}
