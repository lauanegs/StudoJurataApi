package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.model.Evento;
import studojurata_api.repository.EventoRepository;

import java.util.List;

/**
 * Correção 2.8. Item 4.4 da primeira Análise Crítica (filtro Pendentes/
 * Concluídos) coberto por listarPendentes/listarConcluidos.
 */
@Service
@RequiredArgsConstructor
public class EventoService {

    private final EventoRepository repository;

    public List<Evento> listar() { return repository.findAllByOrderByDataHorarioAsc(); }

    public List<Evento> listarPendentes() { return repository.findByConcluidoOrderByDataHorarioAsc(false); }

    public List<Evento> listarConcluidos() { return repository.findByConcluidoOrderByDataHorarioAsc(true); }

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
        return repository.save(obj);
    }

    /** Evento não tem histórico pedagógico sensível — exclusão física é aceitável aqui. */
    public void deletar(Long id) {
        buscar(id);
        repository.deleteById(id);
    }
}
