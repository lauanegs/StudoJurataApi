package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.exception.RegraNegocioException;
import studojurata_api.model.Pessoa;
import studojurata_api.model.enums.StatusAtivoInativo;
import studojurata_api.repository.PessoaRepository;

import java.util.List;

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

    /** A constraint unique do banco já impede, mas checar antes dá uma mensagem clara em vez de 500. */
    private void validarCpfUnico(String cpf, Long ignorarId) {
        if (cpf == null || cpf.isBlank()) return;

        boolean jaExiste = ignorarId == null
                ? repository.existsByCpf(cpf)
                : repository.existsByCpfAndIdNot(cpf, ignorarId);

        if (jaExiste) {
            throw new RegraNegocioException("Já existe uma pessoa cadastrada com o CPF " + cpf + ".");
        }
    }

    /** Soft-delete: Pessoa é a base de Aluno, Professor, Responsavel e Usuario. */
    public void deletar(Long id) {
        Pessoa pessoa = buscar(id);
        pessoa.setStatus(StatusAtivoInativo.INATIVO);
        repository.save(pessoa);
    }
}
