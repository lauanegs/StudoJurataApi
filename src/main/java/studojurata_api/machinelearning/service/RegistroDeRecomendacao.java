package studojurata_api.machinelearning.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import studojurata_api.ia.model.enums.MotivoRecomendacao;
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
import studojurata_api.model.enums.NivelDificuldade;

/** Registro da recomendação: grava o que foi decidido (atributos, necessidade, decisão, questões selecionadas) */
@Component
@RequiredArgsConstructor
public class RegistroDeRecomendacao {
    private final RecomendacaoSimuladoRepository recomendacaoSimuladoRepository;

    public RecomendacaoSimulado persistir(Aluno aluno, Disciplina disciplina, ConteudoPlano conteudo,
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
        registro.setQuestoesSelecionadas(new java.util.ArrayList<>(selecao.selecionadas()));

        return recomendacaoSimuladoRepository.save(registro);
    }

    public RecomendacaoSimuladoDTO paraDto(RecomendacaoSimulado registro, ConteudoPlano conteudo,
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

    public String montarMotivo(NecessidadeRevisao necessidade, Set<MotivoRecomendacao> motivos,
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
}
