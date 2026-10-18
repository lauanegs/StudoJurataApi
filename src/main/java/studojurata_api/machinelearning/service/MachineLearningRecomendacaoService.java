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
import studojurata_api.model.pessoa.Aluno;
import studojurata_api.model.planejamento.ConteudoPlano;
import studojurata_api.model.curriculo.Disciplina;
import studojurata_api.model.simulado.Questao;
import studojurata_api.model.simulado.Simulado;
import studojurata_api.model.turma.TurmaDisciplina;
import studojurata_api.model.enums.AcaoAuditoria;
import studojurata_api.model.enums.NivelDificuldade;
import studojurata_api.repository.pessoa.AlunoRepository;
import studojurata_api.repository.planejamento.ConteudoPlanoRepository;
import studojurata_api.repository.simulado.QuestaoAlunoRepository;
import studojurata_api.service.auditoria.AuditLogService;

/** Orquestra a recomendação de simulado individual */
@Service
@RequiredArgsConstructor
public class MachineLearningRecomendacaoService {
    private static final Logger log = LoggerFactory.getLogger(MachineLearningRecomendacaoService.class);

    /** Proporção das questões no nível prioritário quando há um nível definido */
    private static final double PROPORCAO_NIVEL_PRIORITARIO = 0.6;

    private final AtributosAlunoService atributosAlunoService;
    private final RepeticaoEspacadaService repeticaoEspacadaService;
    private final SelecaoQuestoesService selecaoQuestoesService;
    private final DecisaoGeracaoIAService decisaoGeracaoIAService;
    private final BaixoAproveitamentoColetivoService baixoAproveitamentoColetivoService;
    private final RecomendacaoService recomendacaoService;
    private final RecomendacaoSimuladoRepository recomendacaoSimuladoRepository;
    private final RegistroDeRecomendacao registroDeRecomendacao;
    private final AlunoRepository alunoRepository;
    private final ConteudoPlanoRepository conteudoPlanoRepository;
    private final QuestaoAlunoRepository questaoAlunoRepository;
    private final AuditLogService auditLogService;

    /** Recomenda (e registra) um simulado individual para o aluno no conteúdo */
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

        NecessidadeRevisao necessidade = repeticaoEspacadaService.classificar(atributos);
        OrigemDecisao origem = atributos.temHistorico()
                ? OrigemDecisao.REGRA_DETERMINISTICA
                : OrigemDecisao.FALLBACK_SEM_HISTORICO;

        // Defensivo: a tabela de quantidades já respeita o limite, mas o simulado nunca pode passar de 10 questões
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

        String motivo = registroDeRecomendacao.montarMotivo(necessidade, motivosEfetivos, atributos, selecao, decisao, bloqueadoPeloAlerta);
        if (bloqueadoPeloAlerta) {
            auditLogService.registrar("RecomendacaoSimulado", alunoId, AcaoAuditoria.CRIACAO, alertaProfessor);
            log.info("Geração individual bloqueada por baixo aproveitamento coletivo (aluno {}, conteúdo {})",
                    alunoId, conteudoPlanoId);
        }

        RecomendacaoSimulado registro = registroDeRecomendacao.persistir(aluno, disciplina, conteudo, atributos, necessidade, origem,
                decisao, quantidade, intervalo, motivosEfetivos, motivo, selecao, dadosInsuficientes, bloqueadoPeloAlerta);

        return registroDeRecomendacao.paraDto(registro, conteudo, prioritarios, distribuicao, selecao, motivosEfetivos, motivo, necessidade,
                origem, dadosInsuficientes, alertaProfessor, nivelPrioritario);
    }

    /** Liga o simulado gerado à recomendação que o originou (fecha o rastro do aprendizado) */
    @Transactional
    public void vincularSimulado(Long recomendacaoId, Simulado simulado) {
        if (recomendacaoId == null || simulado == null) return;

        recomendacaoSimuladoRepository.findById(recomendacaoId).ifPresent(registro -> {
            registro.setSimulado(simulado);
            recomendacaoSimuladoRepository.save(registro);
        });
    }

    /** Registra o desfecho real do aluno nas recomendações ainda em aberto que tocaram as questões aplicadas */
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

        // Sem vínculo explícito (ex.: simulado tradicional com as mesmas questões): usa a recomendação aberta mais
        return pendentes.stream()
                .filter(registro -> registro.getQuestoesSelecionadas().stream()
                        .map(Questao::getId)
                        .anyMatch(questaoIdsAplicadas::contains))
                .findFirst()
                .orElse(null);
    }

    /** Conteúdo alvo primeiro e, depois, os conteúdos da mesma disciplina/turma em que a regra pedagógica já apontou */
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

    /** Distribuição de dificuldade do simulado */
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

        // O arredondamento para baixo nunca pode reduzir o simulado
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
