package studojurata_api.machinelearning.service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.ia.dto.RecomendacaoDTO;
import studojurata_api.ia.model.enums.MotivoRecomendacao;
import studojurata_api.ia.service.RecomendacaoService;
import studojurata_api.machinelearning.dto.AtributosRecomendacao;
import studojurata_api.machinelearning.dto.RecomendacaoSimuladoDTO;
import studojurata_api.machinelearning.model.RecomendacaoSimulado;
import studojurata_api.machinelearning.model.enums.DecisaoGeracao;
import studojurata_api.machinelearning.model.enums.NecessidadeRevisao;
import studojurata_api.machinelearning.model.enums.OrigemDecisao;
import studojurata_api.machinelearning.repository.RecomendacaoSimuladoRepository;
import studojurata_api.model.Aluno;
import studojurata_api.model.ConteudoPlano;
import studojurata_api.model.Disciplina;
import studojurata_api.model.Questao;
import studojurata_api.model.Simulado;
import studojurata_api.model.TurmaDisciplina;
import studojurata_api.model.enums.AcaoAuditoria;
import studojurata_api.model.enums.NivelDificuldade;
import studojurata_api.repository.AlunoRepository;
import studojurata_api.repository.ConteudoPlanoRepository;
import studojurata_api.repository.QuestaoAlunoRepository;
import studojurata_api.service.AuditLogService;

/**
 * Orquestra a recomendação de simulado individual: monta os atributos reais do
 * aluno, tenta a classificação pelo Weka, cai para a regra determinística quando
 * não há modelo, calcula o intervalo de revisão, seleciona questões do banco,
 * decide entre reutilizar o banco ou gerar por IA e registra tudo para o
 * aprendizado futuro.
 *
 * <p>Não gera simulado nem chama a IA: essa parte continua em
 * {@code GeracaoSimuladoIAService}, que consome o resultado daqui. Também não
 * trata autorização — quem decide se o usuário pode pedir isso é o controller e
 * o {@code SecurityConfig}, como no resto do projeto.
 *
 * <p>As regras pedagógicas que já existiam (motivo do reforço, nível
 * prioritário) são reaproveitadas de {@link RecomendacaoService} em vez de
 * reescritas aqui.
 */
@Service
@RequiredArgsConstructor
public class MachineLearningRecomendacaoService {

    private static final Logger log = LoggerFactory.getLogger(MachineLearningRecomendacaoService.class);

    /** Proporção das questões no nível prioritário quando há um nível definido. */
    private static final double PROPORCAO_NIVEL_PRIORITARIO = 0.6;

    private final AtributosAlunoService atributosAlunoService;
    private final WekaModeloService wekaModeloService;
    private final RepeticaoEspacadaService repeticaoEspacadaService;
    private final SelecaoQuestoesService selecaoQuestoesService;
    private final DecisaoGeracaoIAService decisaoGeracaoIAService;
    private final BaixoAproveitamentoColetivoService baixoAproveitamentoColetivoService;
    private final RecomendacaoService recomendacaoService;
    private final RecomendacaoSimuladoRepository recomendacaoSimuladoRepository;
    private final AlunoRepository alunoRepository;
    private final ConteudoPlanoRepository conteudoPlanoRepository;
    private final QuestaoAlunoRepository questaoAlunoRepository;
    private final AuditLogService auditLogService;

    /**
     * Recomenda (e registra) um simulado individual para o aluno no conteúdo.
     *
     * @param nivelForcado quando o professor escolhe o nível manualmente, ele prevalece
     *                     sobre o nível sugerido pela análise;
     * @param motivos      motivos já conhecidos pelo chamador; quando vazios, são
     *                     resolvidos pela regra pedagógica existente.
     */
    @Transactional
    public RecomendacaoSimuladoDTO recomendar(
            Long alunoId, Long conteudoPlanoId, NivelDificuldade nivelForcado, Set<MotivoRecomendacao> motivos) {

        Aluno aluno = alunoRepository.findById(alunoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aluno não encontrado."));
        ConteudoPlano conteudo = conteudoPlanoRepository.findById(conteudoPlanoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Conteúdo não encontrado."));

        Disciplina disciplina = disciplinaDoConteudo(conteudo);
        Long disciplinaId = disciplina != null ? disciplina.getId() : null;

        AtributosRecomendacao atributos = atributosAlunoService.montar(alunoId, conteudoPlanoId, disciplinaId);

        List<RecomendacaoDTO> pedagogicas = recomendacaoService.recomendarParaAluno(alunoId);
        RecomendacaoDTO doConteudo = pedagogicas.stream()
                .filter(item -> conteudoPlanoId.equals(item.getConteudoPlanoId()))
                .findFirst()
                .orElse(null);

        Set<MotivoRecomendacao> motivosEfetivos = motivos != null && !motivos.isEmpty()
                ? motivos
                : doConteudo != null ? doConteudo.getMotivos() : Set.of();
        NivelDificuldade nivelPrioritario = nivelForcado != null
                ? nivelForcado
                : doConteudo != null ? doConteudo.getNivelPrioritario() : null;

        Optional<NecessidadeRevisao> previsao = wekaModeloService.classificar(atributos);
        NecessidadeRevisao necessidade = previsao.orElseGet(() -> repeticaoEspacadaService.classificar(atributos));
        OrigemDecisao origem = previsao.isPresent()
                ? OrigemDecisao.WEKA
                : atributos.temHistorico() ? OrigemDecisao.REGRA_DETERMINISTICA : OrigemDecisao.FALLBACK_SEM_HISTORICO;

        // Defensivo: a tabela de quantidades já respeita o limite, mas o simulado
        // nunca pode passar de 10 questões, venha de onde vier.
        int quantidade = Math.min(
                repeticaoEspacadaService.quantidadeQuestoes(necessidade), SelecaoQuestoesService.MAXIMO_QUESTOES);
        int intervalo = repeticaoEspacadaService.intervaloDias(atributos, necessidade);
        List<Long> prioritarios = conteudosPrioritarios(conteudo, pedagogicas);
        Map<NivelDificuldade, Integer> distribuicao = distribuicaoDeDificuldade(quantidade, nivelPrioritario, necessidade);

        SelecaoQuestoesService.Resultado selecao = selecaoQuestoesService.selecionar(
                new SelecaoQuestoesService.Entrada(
                        disciplinaId,
                        prioritarios,
                        distribuicao,
                        quantidade,
                        questoesJaRespondidas(alunoId)));

        DecisaoGeracao decisao = decisaoGeracaoIAService.decidir(true, motivosEfetivos, selecao.bancoSuficiente());

        String alertaProfessor = baixoAproveitamentoColetivoService.avaliar(conteudo);
        boolean bloqueadoPeloAlerta = alertaProfessor != null;
        boolean dadosInsuficientes = !atributos.temHistorico();

        String motivo = montarMotivo(necessidade, motivosEfetivos, atributos, selecao, decisao, bloqueadoPeloAlerta);
        if (bloqueadoPeloAlerta) {
            auditLogService.registrar("RecomendacaoSimulado", alunoId, AcaoAuditoria.CRIACAO, alertaProfessor);
            log.info("Geração individual bloqueada por baixo aproveitamento coletivo (aluno {}, conteúdo {})",
                    alunoId, conteudoPlanoId);
        }

        RecomendacaoSimulado registro = persistir(aluno, disciplina, conteudo, atributos, necessidade, origem,
                decisao, quantidade, intervalo, motivosEfetivos, motivo, selecao, dadosInsuficientes, bloqueadoPeloAlerta);

        return paraDto(registro, conteudo, prioritarios, distribuicao, selecao, motivosEfetivos, motivo, necessidade,
                origem, dadosInsuficientes, previsao.isPresent(), alertaProfessor, nivelPrioritario);
    }

    /** Liga o simulado gerado à recomendação que o originou (fecha o rastro do aprendizado). */
    @Transactional
    public void vincularSimulado(Long recomendacaoId, Simulado simulado) {
        if (recomendacaoId == null || simulado == null) return;

        recomendacaoSimuladoRepository.findById(recomendacaoId).ifPresent(registro -> {
            registro.setSimulado(simulado);
            recomendacaoSimuladoRepository.save(registro);
        });
    }

    /**
     * Registra o desfecho real do aluno nas recomendações ainda em aberto que
     * tocaram as questões aplicadas. É esse número que vira rótulo de treino do
     * Weka depois — por isso vem do simulado corrigido, nunca de estimativa.
     *
     * <p>Uma tentativa rotula no máximo <b>uma</b> recomendação: primeiro a que
     * gerou este simulado (vínculo explícito) e, quando não há vínculo, a mais
     * recente em aberto que contenha as questões aplicadas. Assim o mesmo
     * desfecho não vira rótulo de duas recomendações diferentes.
     */
    @Transactional
    public int registrarResultado(
            Long alunoId, Long simuladoId, double percentualAcerto, Collection<Long> questaoIdsAplicadas) {
        if (alunoId == null || questaoIdsAplicadas == null || questaoIdsAplicadas.isEmpty()) {
            return 0;
        }

        Set<Long> aplicadas = Set.copyOf(questaoIdsAplicadas);
        RecomendacaoSimulado alvo = recomendacaoQueExplicaATentativa(alunoId, simuladoId, aplicadas);
        if (alvo == null) {
            return 0;
        }

        alvo.setPercentualAcertoPosterior(percentualAcerto);
        alvo.setDataResultado(LocalDate.now());
        recomendacaoSimuladoRepository.save(alvo);

        // Dado novo de treino: o próximo uso reconstrói o modelo.
        wekaModeloService.invalidarModelo();
        return 1;
    }

    private RecomendacaoSimulado recomendacaoQueExplicaATentativa(
            Long alunoId, Long simuladoId, Set<Long> questaoIdsAplicadas) {

        List<RecomendacaoSimulado> pendentes = recomendacaoSimuladoRepository
                .findByAluno_IdAndPercentualAcertoPosteriorIsNullOrderByCreatedAtDesc(alunoId);
        if (pendentes.isEmpty()) {
            return null;
        }

        if (simuladoId != null) {
            Optional<RecomendacaoSimulado> doSimulado = pendentes.stream()
                    .filter(registro -> registro.getSimulado() != null
                            && simuladoId.equals(registro.getSimulado().getId()))
                    .findFirst();
            if (doSimulado.isPresent()) {
                return doSimulado.get();
            }
        }

        // Sem vínculo explícito (ex.: simulado tradicional com as mesmas questões):
        // usa a recomendação aberta mais recente que tocou o que foi aplicado.
        return pendentes.stream()
                .filter(registro -> registro.getQuestoesSelecionadas().stream()
                        .map(Questao::getId)
                        .anyMatch(questaoIdsAplicadas::contains))
                .findFirst()
                .orElse(null);
    }

    // --- montagem ----------------------------------------------------------

    private RecomendacaoSimulado persistir(Aluno aluno, Disciplina disciplina, ConteudoPlano conteudo,
            AtributosRecomendacao atributos, NecessidadeRevisao necessidade, OrigemDecisao origem,
            DecisaoGeracao decisao, int quantidade, int intervalo, Set<MotivoRecomendacao> motivos, String motivo,
            SelecaoQuestoesService.Resultado selecao, boolean dadosInsuficientes, boolean bloqueadoPeloAlerta) {

        RecomendacaoSimulado registro = new RecomendacaoSimulado();
        registro.setAluno(aluno);
        registro.setDisciplina(disciplina);
        registro.setConteudoPlano(conteudo);

        registro.setPercentualAcertoConteudo(atributos.percentualAcertoConteudo());
        registro.setPercentualAcertoRecente(atributos.percentualAcertoRecente());
        registro.setPercentualAcertoDisciplina(atributos.percentualAcertoDisciplina());
        registro.setQuantidadeTentativas(atributos.quantidadeTentativas());
        registro.setQuantidadeRevisoes(atributos.quantidadeRevisoes());
        registro.setDiasDesdeUltimaResposta(atributos.diasDesdeUltimaResposta());
        registro.setDiasDesdeUltimaRevisao(atributos.diasDesdeUltimaRevisao());
        registro.setDificuldadeMediaRespondida(atributos.dificuldadeMediaRespondida());
        registro.setQuantidadeQuestoesRespondidas(atributos.quantidadeQuestoesRespondidas());

        registro.setNecessidadeRevisao(necessidade);
        registro.setOrigemDecisao(origem);
        registro.setDecisaoGeracao(decisao);
        registro.setQuantidadeRecomendada(quantidade);
        registro.setIntervaloRevisaoDias(intervalo);
        registro.setNecessitaGeracaoIA(!bloqueadoPeloAlerta && decisao == DecisaoGeracao.GERAR_POR_IA);
        registro.setDadosInsuficientes(dadosInsuficientes);
        registro.setMotivos(motivos != null ? new LinkedHashSet<>(motivos) : new LinkedHashSet<>());
        registro.setMotivo(motivo);
        registro.setQuestoesSelecionadas(List.copyOf(selecao.selecionadas()));

        return recomendacaoSimuladoRepository.save(registro);
    }

    private RecomendacaoSimuladoDTO paraDto(RecomendacaoSimulado registro, ConteudoPlano conteudo,
            List<Long> prioritarios, Map<NivelDificuldade, Integer> distribuicao,
            SelecaoQuestoesService.Resultado selecao, Set<MotivoRecomendacao> motivos, String motivo,
            NecessidadeRevisao necessidade, OrigemDecisao origem, boolean dadosInsuficientes,
            boolean modeloUsado, String alertaProfessor, NivelDificuldade nivelPrioritario) {

        RecomendacaoSimuladoDTO dto = new RecomendacaoSimuladoDTO();
        dto.setId(registro.getId());
        dto.setAlunoId(registro.getAluno().getId());
        dto.setDisciplinaId(registro.getDisciplina() != null ? registro.getDisciplina().getId() : null);
        dto.setConteudoPlanoId(conteudo.getId());
        dto.setConteudoTitulo(conteudo.getTitulo());
        dto.setQuantidadeRecomendada(registro.getQuantidadeRecomendada());
        dto.setConteudosPrioritarios(prioritarios);
        dto.setDistribuicaoDeDificuldade(distribuicao);
        dto.setQuestoesCandidatas(selecao.selecionadas());
        dto.setDecisaoGeracao(registro.getDecisaoGeracao());
        dto.setNecessitaGeracaoIA(Boolean.TRUE.equals(registro.getNecessitaGeracaoIA()));
        dto.setMotivos(motivos != null ? motivos : Set.of());
        dto.setMotivo(motivo);
        dto.setIntervaloRevisaoDias(registro.getIntervaloRevisaoDias());
        dto.setNivelPrioritario(nivelPrioritario);
        dto.setNecessidadeRevisao(necessidade);
        dto.setOrigemDecisao(origem);
        dto.setDadosInsuficientes(dadosInsuficientes);
        dto.setModeloIndisponivel(!modeloUsado);
        dto.setAtributos(new AtributosRecomendacao(registro.getAluno().getId(), conteudo.getId(),
                dto.getDisciplinaId(), registro.getPercentualAcertoConteudo(), registro.getPercentualAcertoRecente(),
                registro.getPercentualAcertoDisciplina(), registro.getQuantidadeTentativas(),
                registro.getQuantidadeRevisoes(), registro.getDiasDesdeUltimaResposta(),
                registro.getDiasDesdeUltimaRevisao(), registro.getDificuldadeMediaRespondida(),
                registro.getQuantidadeQuestoesRespondidas()));
        dto.setAlertaProfessor(alertaProfessor);
        return dto;
    }

    /**
     * Conteúdo alvo primeiro e, depois, os conteúdos da mesma disciplina/turma
     * em que a regra pedagógica já apontou baixo aproveitamento.
     */
    private List<Long> conteudosPrioritarios(ConteudoPlano conteudo, List<RecomendacaoDTO> pedagogicas) {
        Set<Long> prioritarios = new LinkedHashSet<>();
        prioritarios.add(conteudo.getId());

        Long vinculoId = turmaDisciplinaId(conteudo);
        if (vinculoId == null) {
            return List.copyOf(prioritarios);
        }

        Set<Long> conteudosDaDisciplina = conteudoPlanoRepository
                .findByPlanoEnsino_TurmaDisciplina_IdIn(Set.of(vinculoId)).stream()
                .map(ConteudoPlano::getId)
                .collect(Collectors.toSet());

        pedagogicas.stream()
                .filter(item -> item.getMotivos() != null
                        && item.getMotivos().contains(MotivoRecomendacao.BAIXO_APROVEITAMENTO))
                .map(RecomendacaoDTO::getConteudoPlanoId)
                .filter(conteudosDaDisciplina::contains)
                .forEach(prioritarios::add);

        return List.copyOf(prioritarios);
    }

    /**
     * Distribuição de dificuldade do simulado: com nível prioritário definido, a
     * maior parte vai nele (com um pouco de base e de desafio); sem nível, a
     * distribuição acompanha a urgência da revisão (revisar agora pede base).
     */
    private Map<NivelDificuldade, Integer> distribuicaoDeDificuldade(
            int quantidade, NivelDificuldade nivelPrioritario, NecessidadeRevisao necessidade) {

        Map<NivelDificuldade, Double> proporcoes = new LinkedHashMap<>();

        if (nivelPrioritario != null) {
            NivelDificuldade base = nivelAnterior(nivelPrioritario);
            NivelDificuldade desafio = nivelSeguinte(nivelPrioritario);
            double sobra = 1 - PROPORCAO_NIVEL_PRIORITARIO;
            if (base != null) {
                proporcoes.put(base, sobra / 2);
            }
            proporcoes.merge(nivelPrioritario, PROPORCAO_NIVEL_PRIORITARIO, Double::sum);
            if (desafio != null) {
                proporcoes.merge(desafio, sobra / 2, Double::sum);
            }
        } else if (necessidade == NecessidadeRevisao.REVISAR_AGORA) {
            proporcoes.put(NivelDificuldade.FACIL, 0.5);
            proporcoes.put(NivelDificuldade.MEDIA, 0.5);
        } else if (necessidade == NecessidadeRevisao.REVISAR_EM_BREVE) {
            proporcoes.put(NivelDificuldade.FACIL, 0.3);
            proporcoes.put(NivelDificuldade.MEDIA, 0.4);
            proporcoes.put(NivelDificuldade.DIFICIL, 0.3);
        } else {
            proporcoes.put(NivelDificuldade.FACIL, 0.2);
            proporcoes.put(NivelDificuldade.MEDIA, 0.4);
            proporcoes.put(NivelDificuldade.DIFICIL, 0.4);
        }

        Map<NivelDificuldade, Integer> quantidades = new LinkedHashMap<>();
        int alocado = 0;
        for (Map.Entry<NivelDificuldade, Double> entrada : proporcoes.entrySet()) {
            int doNivel = (int) Math.floor(quantidade * entrada.getValue());
            if (doNivel > 0) {
                quantidades.merge(entrada.getKey(), doNivel, Integer::sum);
                alocado += doNivel;
            }
        }

        // O arredondamento para baixo nunca pode reduzir o simulado: o resto vai
        // para o nível prioritário (ou para o de maior peso).
        NivelDificuldade preferido = nivelPrioritario != null
                ? nivelPrioritario
                : proporcoes.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(NivelDificuldade.MEDIA);
        if (alocado < quantidade) {
            quantidades.merge(preferido, quantidade - alocado, Integer::sum);
        }
        return quantidades;
    }

    private Set<Long> questoesJaRespondidas(Long alunoId) {
        return questaoAlunoRepository.findBySimuladoAluno_AlunoId(alunoId).stream()
                .map(resposta -> resposta.getQuestao().getId())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String montarMotivo(NecessidadeRevisao necessidade, Set<MotivoRecomendacao> motivos,
            AtributosRecomendacao atributos, SelecaoQuestoesService.Resultado selecao, DecisaoGeracao decisao,
            boolean bloqueadoPeloAlerta) {

        StringBuilder texto = new StringBuilder("Necessidade: ").append(necessidade).append('.');

        if (!atributos.temHistorico()) {
            texto.append(" Sem histórico real no conteúdo: decisão de partida.");
        } else {
            texto.append(" Acerto no conteúdo: ").append(percentual(atributos.percentualAcertoConteudo()))
                    .append("; acerto recente: ").append(percentual(atributos.percentualAcertoRecente())).append('.');
            if (atributos.diasDesdeUltimaResposta() != null) {
                texto.append(" Última resposta há ").append(atributos.diasDesdeUltimaResposta()).append(" dia(s).");
            }
        }

        if (motivos != null && !motivos.isEmpty()) {
            texto.append(" Motivos: ").append(motivos).append('.');
        }

        texto.append(" Banco: ").append(selecao.selecionadas().size()).append(" questão(ões) selecionada(s)");
        if (!selecao.faltantes().isEmpty()) {
            texto.append("; faltou ").append(selecao.faltantes()).append(" por dificuldade");
        }
        if (selecao.repetidas() > 0) {
            texto.append("; ").append(selecao.repetidas()).append(" repetida(s) do próprio aluno");
        }
        texto.append(". Decisão: ").append(decisao).append('.');

        if (bloqueadoPeloAlerta) {
            texto.append(" Geração individual bloqueada por baixo aproveitamento coletivo.");
        }
        return texto.toString();
    }

    private static String percentual(Double valor) {
        return valor == null ? "sem dado" : Math.round(valor * 100) + "%";
    }

    private static Disciplina disciplinaDoConteudo(ConteudoPlano conteudo) {
        TurmaDisciplina vinculo = turmaDisciplinaDoConteudo(conteudo);
        return vinculo != null ? vinculo.getDisciplina() : null;
    }

    private static Long turmaDisciplinaId(ConteudoPlano conteudo) {
        TurmaDisciplina vinculo = turmaDisciplinaDoConteudo(conteudo);
        return vinculo != null ? vinculo.getId() : null;
    }

    private static TurmaDisciplina turmaDisciplinaDoConteudo(ConteudoPlano conteudo) {
        return conteudo.getPlanoEnsino() != null ? conteudo.getPlanoEnsino().getTurmaDisciplina() : null;
    }

    private static NivelDificuldade nivelAnterior(NivelDificuldade nivel) {
        return switch (nivel) {
            case FACIL -> null;
            case MEDIA -> NivelDificuldade.FACIL;
            case DIFICIL -> NivelDificuldade.MEDIA;
        };
    }

    private static NivelDificuldade nivelSeguinte(NivelDificuldade nivel) {
        return switch (nivel) {
            case FACIL -> NivelDificuldade.MEDIA;
            case MEDIA -> NivelDificuldade.DIFICIL;
            case DIFICIL -> null;
        };
    }
}
