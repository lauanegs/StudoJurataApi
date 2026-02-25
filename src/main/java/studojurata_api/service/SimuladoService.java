package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import studojurata_api.model.Simulado;
import studojurata_api.repository.SimuladoRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SimuladoService {

    private final SimuladoRepository repository;

    public List<Simulado> listar() { return repository.findAll(); }
    public Simulado buscar(Long id) { return repository.findById(id).orElseThrow(); }
    public Simulado salvar(Simulado obj) { return repository.save(obj); }

    public Simulado atualizar(Long id, Simulado obj) {
        obj.setId(id);
        return repository.save(obj);
    }

    public void deletar(Long id) { repository.deleteById(id); }
}