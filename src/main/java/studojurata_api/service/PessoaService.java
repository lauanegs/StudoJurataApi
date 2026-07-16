package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.model.Pessoa;
import studojurata_api.model.enums.StatusAtivoInativo;
import studojurata_api.repository.PessoaRepository;

import java.util.List;

/** Correção 5.1: controller passa a usar este service, não mais o Repository. */
@Service
@RequiredArgsConstructor
public class PessoaService {

    private final PessoaRepository repository;

    public List<Pessoa> listar() { return repository.findAll(); }

    public Pessoa buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pessoa " + id + " não encontrada."));
    }

    public Pessoa salvar(Pessoa obj) {
        if (obj.getStatus() == null) obj.setStatus(StatusAtivoInativo.ATIVO);
        return repository.save(obj);
    }

    public Pessoa atualizar(Long id, Pessoa obj) {
        obj.setId(id);
        return repository.save(obj);
    }

    /** Soft-delete (item 4.3/5.1): Pessoa é a base de Aluno/Professor/Responsavel/Usuario. */
    public void deletar(Long id) {
        Pessoa pessoa = buscar(id);
        pessoa.setStatus(StatusAtivoInativo.INATIVO);
        repository.save(pessoa);
    }
}
