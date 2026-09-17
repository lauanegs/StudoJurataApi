package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import studojurata_api.model.Escola;
import studojurata_api.repository.EscolaRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EscolaService {

    private final EscolaRepository repository;

    public List<Escola> listar() { return repository.findAll(); }
}
