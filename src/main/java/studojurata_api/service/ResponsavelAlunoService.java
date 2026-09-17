package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.model.ResponsavelAluno;
import studojurata_api.repository.ResponsavelAlunoRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ResponsavelAlunoService {

    private final ResponsavelAlunoRepository repository;

    public List<ResponsavelAluno> listar() { return repository.findAll(); }

    public ResponsavelAluno buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Vínculo responsável-aluno " + id + " não encontrado."));
    }

    public List<ResponsavelAluno> porAluno(Long alunoId) { return repository.findByAlunoId(alunoId); }

    public List<ResponsavelAluno> porResponsavel(Long responsavelId) { return repository.findByResponsavelId(responsavelId); }

    public ResponsavelAluno salvar(ResponsavelAluno obj) { return repository.save(obj); }

    public ResponsavelAluno atualizar(Long id, ResponsavelAluno obj) {
        obj.setId(id);
        return repository.save(obj);
    }

    public ResponsavelAluno aceitarTermos(Long id, String textoVersao) {
        ResponsavelAluno vinculo = buscar(id);
        vinculo.setAceitouTermos(true);
        vinculo.setDataAceite(LocalDateTime.now());
        vinculo.setTextoVersao(textoVersao);
        return repository.save(vinculo);
    }

    /** Exclusão física é aceitável: o vínculo não guarda histórico pedagógico. */
    public void deletar(Long id) {
        buscar(id);
        repository.deleteById(id);
    }
}
