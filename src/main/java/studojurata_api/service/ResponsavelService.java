package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.model.Responsavel;
import studojurata_api.model.enums.StatusAtivoInativo;
import studojurata_api.repository.ResponsavelRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ResponsavelService {

    private final ResponsavelRepository repository;

    public List<Responsavel> listar() { return repository.findAll(); }

    public Responsavel buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Responsável " + id + " não encontrado."));
    }

    public Responsavel salvar(Responsavel obj) { return repository.save(obj); }

    public Responsavel atualizar(Long id, Responsavel obj) {
        obj.setId(id);
        return repository.save(obj);
    }

    /** Soft-delete pela Pessoa, já que Responsavel não tem status próprio. */
    @Transactional
    public void deletar(Long id) {
        Responsavel responsavel = buscar(id);
        if (responsavel.getPessoa() != null) {
            responsavel.getPessoa().setStatus(StatusAtivoInativo.INATIVO);
        }
    }

    @Transactional
    public Responsavel ativar(Long id) {
        Responsavel responsavel = buscar(id);
        if (responsavel.getPessoa() != null) {
            responsavel.getPessoa().setStatus(StatusAtivoInativo.ATIVO);
        }
        return responsavel;
    }
}
