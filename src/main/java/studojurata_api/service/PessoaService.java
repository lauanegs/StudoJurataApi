package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import studojurata_api.model.Pessoa;
import studojurata_api.repository.PessoaRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PessoaService {

    private final PessoaRepository repository;

    public List<Pessoa> listar() { return repository.findAll(); }
    public Pessoa buscar(Long id) { return repository.findById(id).orElseThrow(); }
    public Pessoa salvar(Pessoa obj) { return repository.save(obj); }

    public Pessoa atualizar(Long id, Pessoa obj) {
        obj.setId(id);
        return repository.save(obj);
    }

    public void deletar(Long id) { repository.deleteById(id); }
}