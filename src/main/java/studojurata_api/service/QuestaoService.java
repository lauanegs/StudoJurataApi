package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.exception.RegraNegocioException;
import studojurata_api.model.Questao;
import studojurata_api.model.enums.OrigemQuestao;
import studojurata_api.model.enums.StatusQuestao;
import studojurata_api.repository.QuestaoRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestaoService {

    private final QuestaoRepository repository;

    public List<Questao> listar() { return repository.findAll(); }

    public Questao buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Questão " + id + " não encontrada."));
    }

    /** Status inicial pela origem: IA nasce PENDENTE, professor nasce APROVADA. */
    public Questao salvar(Questao obj) {
        if (obj.getOrigem() == null) {
            obj.setOrigem(OrigemQuestao.PROFESSOR);
        }
        if (obj.getStatus() == null) {
            obj.setStatus(obj.getOrigem() == OrigemQuestao.IA ? StatusQuestao.PENDENTE : StatusQuestao.APROVADA);
        }
        return repository.save(obj);
    }

    public Questao atualizar(Long id, Questao obj) {
        Questao existente = buscar(id);
        obj.setId(id);
        // O status só muda por aprovar()/rejeitar().
        obj.setStatus(existente.getStatus());
        return repository.save(obj);
    }

    public List<Questao> listarPendentes() {
        return repository.findByStatus(StatusQuestao.PENDENTE);
    }

    @Transactional
    public Questao aprovar(Long id) {
        Questao questao = buscar(id);
        if (questao.getStatus() != StatusQuestao.PENDENTE) {
            throw new RegraNegocioException("Apenas questões PENDENTES podem ser aprovadas.");
        }
        questao.setStatus(StatusQuestao.APROVADA);
        return repository.save(questao);
    }

    @Transactional
    public Questao rejeitar(Long id) {
        Questao questao = buscar(id);
        if (questao.getStatus() != StatusQuestao.PENDENTE) {
            throw new RegraNegocioException("Apenas questões PENDENTES podem ser rejeitadas.");
        }
        questao.setStatus(StatusQuestao.REJEITADA);
        return repository.save(questao);
    }
}
