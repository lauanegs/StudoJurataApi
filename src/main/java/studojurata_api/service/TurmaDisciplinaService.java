package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.exception.RequisicaoInvalidaException;
import studojurata_api.model.CursoDisciplina;
import studojurata_api.model.Turma;
import studojurata_api.model.TurmaDisciplina;
import studojurata_api.model.enums.StatusAtivoInativo;
import studojurata_api.repository.CursoDisciplinaRepository;
import studojurata_api.repository.TurmaDisciplinaRepository;
import studojurata_api.repository.TurmaRepository;

import java.util.List;

/**
 * Correção 5.1: controller passa a usar este service, não mais o Repository.
 *
 * Validação adicionada (pedido do usuário): o front (TurmaFormulario) já
 * filtrava o seletor de disciplina pra só mostrar as da grade curricular do
 * curso da turma (CursoDisciplina), mas nada impedia um POST direto na API
 * vincular qualquer disciplina — a trava só existia no front. Agora o
 * service também recusa.
 */
@Service
@RequiredArgsConstructor
public class TurmaDisciplinaService {

    private final TurmaDisciplinaRepository repository;
    private final TurmaRepository turmaRepository;
    private final CursoDisciplinaRepository cursoDisciplinaRepository;

    public List<TurmaDisciplina> listar() { return repository.findAll(); }

    public TurmaDisciplina buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Vínculo turma-disciplina " + id + " não encontrado."));
    }

    public TurmaDisciplina salvar(TurmaDisciplina obj) {
        validar(obj);
        if (obj.getStatus() == null) obj.setStatus(StatusAtivoInativo.ATIVO);
        return repository.save(obj);
    }

    public TurmaDisciplina atualizar(Long id, TurmaDisciplina obj) {
        obj.setId(id);
        validar(obj);
        return repository.save(obj);
    }

    /** Soft-delete (item 4.3/5.1): existem PlanoEnsino/Aula/Simulado pendurados via esta associação. */
    public void deletar(Long id) {
        TurmaDisciplina turmaDisciplina = buscar(id);
        turmaDisciplina.setStatus(StatusAtivoInativo.INATIVO);
        repository.save(turmaDisciplina);
    }

    private void validar(TurmaDisciplina obj) {
        if (obj.getTurma() == null || obj.getTurma().getId() == null) {
            throw new RequisicaoInvalidaException("Turma é obrigatória para o vínculo.");
        }
        if (obj.getDisciplina() == null || obj.getDisciplina().getId() == null) {
            throw new RequisicaoInvalidaException("Disciplina é obrigatória para o vínculo.");
        }

        Turma turma = turmaRepository.findById(obj.getTurma().getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Turma " + obj.getTurma().getId() + " não encontrada."));
        obj.setTurma(turma);

        Long cursoId = turma.getCurso() != null ? turma.getCurso().getId() : null;
        Long disciplinaId = obj.getDisciplina().getId();

        boolean estaNaGrade = cursoId != null && cursoDisciplinaRepository.findByCurso_Id(cursoId).stream()
                .filter(item -> item.getStatus() == StatusAtivoInativo.ATIVO)
                .map(CursoDisciplina::getDisciplina)
                .anyMatch(disciplina -> disciplina != null && disciplinaId.equals(disciplina.getId()));

        if (!estaNaGrade) {
            throw new RequisicaoInvalidaException(
                    "Esta disciplina não faz parte da grade curricular do curso desta turma.");
        }
    }
}
