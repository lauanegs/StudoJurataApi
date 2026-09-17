package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.exception.RegraNegocioException;
import studojurata_api.exception.RequisicaoInvalidaException;
import studojurata_api.model.ConteudoPlano;
import studojurata_api.model.Questao;
import studojurata_api.model.QuestaoConteudo;
import studojurata_api.repository.ConteudoPlanoRepository;
import studojurata_api.repository.QuestaoConteudoRepository;
import studojurata_api.repository.QuestaoRepository;

import java.util.List;

/**
 * Sem o vínculo questão-conteúdo, a questão fica invisível para o cálculo de
 * desempenho por conteúdo (RecomendacaoService). Só as questões geradas pela
 * IA recebem o vínculo automaticamente; as criadas pelo professor dependem
 * deste service. Exclusão física é aceitável porque o vínculo não guarda
 * histórico pedagógico (quem guarda é QuestaoAluno/SimuladoQuestao).
 */
@Service
@RequiredArgsConstructor
public class QuestaoConteudoService {

    private final QuestaoConteudoRepository repository;
    private final QuestaoRepository questaoRepository;
    private final ConteudoPlanoRepository conteudoPlanoRepository;

    public List<QuestaoConteudo> listarPorQuestao(Long questaoId) { return repository.findByQuestao_Id(questaoId); }

    /**
     * Restringe pela disciplina, não pelo plano de ensino como em
     * AulaConteudo, porque a questão não tem turma.
     */
    @Transactional
    public QuestaoConteudo vincular(Long questaoId, Long conteudoPlanoId) {
        Questao questao = questaoRepository.findById(questaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Questão " + questaoId + " não encontrada."));
        ConteudoPlano conteudoPlano = conteudoPlanoRepository.findById(conteudoPlanoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Conteúdo " + conteudoPlanoId + " não encontrado."));

        Long disciplinaDaQuestaoId = questao.getDisciplina() != null ? questao.getDisciplina().getId() : null;
        Long disciplinaDoConteudoId = conteudoPlano.getPlanoEnsino() != null
                && conteudoPlano.getPlanoEnsino().getTurmaDisciplina() != null
                && conteudoPlano.getPlanoEnsino().getTurmaDisciplina().getDisciplina() != null
                ? conteudoPlano.getPlanoEnsino().getTurmaDisciplina().getDisciplina().getId()
                : null;
        if (disciplinaDaQuestaoId == null || !disciplinaDaQuestaoId.equals(disciplinaDoConteudoId)) {
            throw new RequisicaoInvalidaException(
                    "Só é possível vincular conteúdos da mesma disciplina da questão.");
        }

        if (repository.existsByQuestao_IdAndConteudoPlano_Id(questaoId, conteudoPlanoId)) {
            throw new RegraNegocioException("Este conteúdo já está vinculado a esta questão.");
        }

        QuestaoConteudo questaoConteudo = new QuestaoConteudo();
        questaoConteudo.setQuestao(questao);
        questaoConteudo.setConteudoPlano(conteudoPlano);
        return repository.save(questaoConteudo);
    }

    @Transactional
    public void desvincular(Long questaoId, Long conteudoPlanoId) {
        repository.deleteByQuestao_IdAndConteudoPlano_Id(questaoId, conteudoPlanoId);
    }
}
