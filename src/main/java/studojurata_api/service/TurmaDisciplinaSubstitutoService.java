package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.exception.RegraNegocioException;
import studojurata_api.model.TurmaDisciplina;
import studojurata_api.model.TurmaDisciplinaSubstituto;
import studojurata_api.model.enums.StatusAtivoInativo;
import studojurata_api.repository.TurmaDisciplinaRepository;
import studojurata_api.repository.TurmaDisciplinaSubstitutoRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TurmaDisciplinaSubstitutoService {

    private final TurmaDisciplinaSubstitutoRepository repository;
    private final TurmaDisciplinaRepository turmaDisciplinaRepository;

    public List<TurmaDisciplinaSubstituto> listarPorTurmaDisciplina(Long turmaDisciplinaId) {
        return repository.findByTurmaDisciplina_Id(turmaDisciplinaId);
    }

    public TurmaDisciplinaSubstituto adicionar(Long turmaDisciplinaId, TurmaDisciplinaSubstituto obj) {
        TurmaDisciplina turmaDisciplina = turmaDisciplinaRepository.findById(turmaDisciplinaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Vínculo turma-disciplina " + turmaDisciplinaId + " não encontrado."));

        if (obj.getProfessor() == null || obj.getProfessor().getId() == null) {
            throw new RegraNegocioException("Selecione o professor substituto.");
        }

        if (turmaDisciplina.getProfessor() != null
                && turmaDisciplina.getProfessor().getId().equals(obj.getProfessor().getId())) {
            throw new RegraNegocioException("O professor titular não precisa ser cadastrado como substituto.");
        }

        boolean jaSubstituto = repository.findByTurmaDisciplina_Id(turmaDisciplinaId).stream()
                .anyMatch(existente -> existente.getStatus() != StatusAtivoInativo.INATIVO
                        && existente.getProfessor().getId().equals(obj.getProfessor().getId()));
        if (jaSubstituto) {
            throw new RegraNegocioException("Este professor já é substituto desta disciplina na turma.");
        }

        obj.setId(null);
        obj.setTurmaDisciplina(turmaDisciplina);
        obj.setStatus(StatusAtivoInativo.ATIVO);
        return repository.save(obj);
    }

    /** Soft-delete: mantém o histórico de quem já registrou aula como substituto. */
    public void remover(Long id) {
        TurmaDisciplinaSubstituto vinculo = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Substituto " + id + " não encontrado."));
        vinculo.setStatus(StatusAtivoInativo.INATIVO);
        repository.save(vinculo);
    }
}
