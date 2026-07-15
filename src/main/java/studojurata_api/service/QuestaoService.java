package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
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

    public Questao buscar(Long id) { return repository.findById(id).orElseThrow(); }

    /**
     * Ver item 7.3 da Análise Crítica: questões de origem IA nascem PENDENTE
     * e só entram no banco de reaproveitamento após aprovação do professor;
     * questões digitadas manualmente pelo professor (origem PROFESSOR)
     * nascem já APROVADA, pois já passaram pelo julgamento humano na criação.
     */
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
        obj.setId(id);
        return repository.save(obj);
    }

    public void deletar(Long id) { repository.deleteById(id); }

    /** Fila de revisão do professor — tela "Revisão" (itens 1.4 e 7.3). */
    public List<Questao> listarPendentes() {
        return repository.findByStatus(StatusQuestao.PENDENTE);
    }

    @Transactional
    public Questao aprovar(Long id) {
        Questao questao = buscar(id);
        if (questao.getStatus() != StatusQuestao.PENDENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Apenas questões PENDENTES podem ser aprovadas.");
        }
        questao.setStatus(StatusQuestao.APROVADA);
        return repository.save(questao);
    }

    @Transactional
    public Questao rejeitar(Long id) {
        Questao questao = buscar(id);
        if (questao.getStatus() != StatusQuestao.PENDENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Apenas questões PENDENTES podem ser rejeitadas.");
        }
        questao.setStatus(StatusQuestao.REJEITADA);
        return repository.save(questao);
    }
}
