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
 * Correção 5.1: controller passa a usar este service, não mais o Repository.
 * QuestaoConteudo é um vínculo N:N puro (não guarda histórico pedagógico em
 * si — quem guarda é QuestaoAluno/SimuladoQuestao), então exclusão física
 * continua sendo aceitável aqui.
 *
 * vincular/desvincular (usados pelo QuestaoEditor, aba "Conteúdo") seguem o
 * mesmo padrão de AulaConteudoService: sem esse vínculo, a questão fica
 * invisível pro cálculo de desempenho por conteúdo (RecomendacaoService) —
 * hoje só as questões geradas pela IA ganham esse vínculo automaticamente
 * (GeracaoQuestaoIAService), então questões criadas manualmente pelo
 * professor precisam do vínculo explícito aqui pra entrar na métrica.
 */
@Service
@RequiredArgsConstructor
public class QuestaoConteudoService {

    private final QuestaoConteudoRepository repository;
    private final QuestaoRepository questaoRepository;
    private final ConteudoPlanoRepository conteudoPlanoRepository;

    public List<QuestaoConteudo> listar() { return repository.findAll(); }

    public QuestaoConteudo buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Vínculo questão-conteúdo " + id + " não encontrado."));
    }

    public QuestaoConteudo salvar(QuestaoConteudo obj) { return repository.save(obj); }

    public QuestaoConteudo atualizar(Long id, QuestaoConteudo obj) {
        obj.setId(id);
        return repository.save(obj);
    }

    public void deletar(Long id) {
        buscar(id);
        repository.deleteById(id);
    }

    public List<QuestaoConteudo> listarPorQuestao(Long questaoId) { return repository.findByQuestao_Id(questaoId); }

    /**
     * Só permite vincular conteúdos da MESMA disciplina da questão — uma
     * questão não tem turma, só disciplina (Questao.disciplina), então não
     * dá pra restringir por plano de ensino específico como AulaConteudo faz;
     * a disciplina é o recorte que faz sentido aqui.
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
