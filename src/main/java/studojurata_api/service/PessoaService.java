package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.exception.RegraNegocioException;
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
        validarCpfUnico(obj.getCpf(), null);
        if (obj.getStatus() == null) obj.setStatus(StatusAtivoInativo.ATIVO);
        return repository.save(obj);
    }

    public Pessoa atualizar(Long id, Pessoa obj) {
        validarCpfUnico(obj.getCpf(), id);
        obj.setId(id);
        return repository.save(obj);
    }

    /**
     * A constraint @Column(unique = true) do banco já impede duas Pessoas com
     * o mesmo CPF, mas isso estourava como erro 500 genérico de constraint
     * violation. Checar antes devolve uma mensagem amigável (409).
     */
    private void validarCpfUnico(String cpf, Long ignorarId) {
        if (cpf == null || cpf.isBlank()) return;

        boolean jaExiste = ignorarId == null
                ? repository.existsByCpf(cpf)
                : repository.existsByCpfAndIdNot(cpf, ignorarId);

        if (jaExiste) {
            throw new RegraNegocioException("Já existe uma pessoa cadastrada com o CPF " + cpf + ".");
        }
    }

    /** Soft-delete (item 4.3/5.1): Pessoa é a base de Aluno/Professor/Responsavel/Usuario. */
    public void deletar(Long id) {
        Pessoa pessoa = buscar(id);
        pessoa.setStatus(StatusAtivoInativo.INATIVO);
        repository.save(pessoa);
    }
}
