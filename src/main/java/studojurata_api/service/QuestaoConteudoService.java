package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.model.QuestaoConteudo;
import studojurata_api.repository.QuestaoConteudoRepository;

import java.util.List;

/**
 * Correção 5.1: controller passa a usar este service, não mais o Repository.
 * QuestaoConteudo é um vínculo N:N puro (não guarda histórico pedagógico em
 * si — quem guarda é QuestaoAluno/SimuladoQuestao), então exclusão física
 * continua sendo aceitável aqui.
 */
@Service
@RequiredArgsConstructor
public class QuestaoConteudoService {

    private final QuestaoConteudoRepository repository;

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
}
