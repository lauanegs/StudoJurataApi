package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.model.PlanoEnsino;
import studojurata_api.model.enums.StatusAtivoInativo;
import studojurata_api.repository.PlanoEnsinoRepository;

import java.util.List;

/**
 * Correção 5.1: controller passa a usar este service, não mais o Repository.
 * Correção 2.4 da Terceira Análise Crítica: periodoLetivo passa a ser
 * validado (obrigatório) — sem ele, o recálculo automático da Nota do aluno
 * é silenciosamente pulado (ver NotaService/SimuladoAlunoService.finalizar).
 */
@Service
@RequiredArgsConstructor
public class PlanoEnsinoService {

    private final PlanoEnsinoRepository repository;

    public List<PlanoEnsino> listar() { return repository.findAll(); }

    public PlanoEnsino buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Plano de ensino " + id + " não encontrado."));
    }

    public PlanoEnsino salvar(PlanoEnsino obj) {
        validarPeriodoLetivo(obj);
        if (obj.getStatus() == null) obj.setStatus(StatusAtivoInativo.ATIVO);
        return repository.save(obj);
    }

    public PlanoEnsino atualizar(Long id, PlanoEnsino obj) {
        validarPeriodoLetivo(obj);
        obj.setId(id);
        return repository.save(obj);
    }

    private void validarPeriodoLetivo(PlanoEnsino obj) {
        if (obj.getPeriodoLetivo() == null || obj.getPeriodoLetivo().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Período letivo é obrigatório: sem ele, a nota do aluno nesta disciplina não pode ser calculada.");
        }
    }

    /** Soft-delete (item 4.3/5.1 + caso extremo "Plano de ensino alterado após simulados já realizados"). */
    public void deletar(Long id) {
        PlanoEnsino plano = buscar(id);
        plano.setStatus(StatusAtivoInativo.INATIVO);
        repository.save(plano);
    }
}
