package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.model.QuestaoAluno;
import studojurata_api.repository.QuestaoAlunoRepository;

import java.util.List;

/**
 * CRUD de apoio para QuestaoAluno (conceitualmente "RespostaAluno" — ver
 * item 2.4 da Análise Crítica). O caminho principal de escrita é
 * SimuladoAlunoService.finalizar, que grava/atualiza as respostas de uma
 * tentativa inteira de uma vez; este service permanece para consultas
 * pontuais (ex.: tela de detalhamento do simulado por aluno).
 */
@Service
@RequiredArgsConstructor
public class QuestaoAlunoService {

    private final QuestaoAlunoRepository repository;

    public List<QuestaoAluno> listar() { return repository.findAll(); }

    public QuestaoAluno buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("QuestaoAluno " + id + " não encontrada."));
    }

    public List<QuestaoAluno> listarPorSimuladoAluno(Long simuladoAlunoId) {
        return repository.findBySimuladoAlunoId(simuladoAlunoId);
    }

    public QuestaoAluno salvar(QuestaoAluno obj) { return repository.save(obj); }

    public QuestaoAluno atualizar(Long id, QuestaoAluno obj) {
        obj.setId(id);
        return repository.save(obj);
    }

    public void deletar(Long id) { repository.deleteById(id); }
}
