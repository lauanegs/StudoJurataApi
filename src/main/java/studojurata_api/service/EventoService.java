package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.model.Evento;
import studojurata_api.repository.EventoRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventoService {

    private final EventoRepository repository;

    public List<Evento> listar() { return repository.findAllByOrderByDataHorarioAsc(); }

    public List<Evento> listarPendentes() { return repository.findByConcluidoOrderByDataHorarioAsc(false); }

    public Evento buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Evento " + id + " não encontrado."));
    }

    public Evento salvar(Evento obj) {
        if (obj.getConcluido() == null) obj.setConcluido(false);
        return repository.save(obj);
    }

    public Evento atualizar(Long id, Evento obj) {
        obj.setId(id);
        // criadoPor e dado do servidor e nao aparece na resposta de GET /eventos,
        // entao o cliente nunca o devolve: preservar o registro atual evita que uma
        // edicao apague quem criou o evento.
        if (obj.getCriadoPor() == null) {
            repository.findById(id).ifPresent(existente -> obj.setCriadoPor(existente.getCriadoPor()));
        }
        return repository.save(obj);
    }

    /** Evento não tem histórico pedagógico sensível — exclusão física é aceitável aqui. */
    public void deletar(Long id) {
        buscar(id);
        repository.deleteById(id);
    }
}
