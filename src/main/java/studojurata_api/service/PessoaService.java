package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import studojurata_api.exception.RegraNegocioException;
import studojurata_api.exception.RequisicaoInvalidaException;
import studojurata_api.model.Pessoa;
import studojurata_api.model.enums.StatusAtivoInativo;
import studojurata_api.repository.PessoaRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PessoaService {

    private final PessoaRepository repository;

    public List<Pessoa> listar() { return repository.findAll(); }

    public Pessoa salvar(Pessoa obj) {
        validarCpfUnico(obj.getCpf(), null);
        validarSexo(obj);
        if (obj.getStatus() == null) obj.setStatus(StatusAtivoInativo.ATIVO);
        return repository.save(obj);
    }

    public Pessoa atualizar(Long id, Pessoa obj) {
        validarCpfUnico(obj.getCpf(), id);
        validarSexo(obj);
        obj.setId(id);
        return repository.save(obj);
    }

    /**
     * Sexo é obrigatório em todo cadastro de pessoa (aluno, professor e
     * responsável usam esta mesma porta): é dado de identificação usado pelas
     * listagens da secretaria e não há valor padrão que faça sentido supor.
     */
    private void validarSexo(Pessoa obj) {
        if (obj.getSexo() == null) {
            throw new RequisicaoInvalidaException("Sexo é obrigatório.");
        }
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
}
