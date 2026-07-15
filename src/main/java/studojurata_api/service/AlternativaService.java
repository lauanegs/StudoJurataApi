package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import studojurata_api.model.Alternativa;
import studojurata_api.repository.AlternativaRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AlternativaService {

    private final AlternativaRepository repository;

    public List<Alternativa> listar() { return repository.findAll(); }

    public Alternativa buscar(Long id) { return repository.findById(id).orElseThrow(); }

    /**
     * Garante no máximo uma alternativa correta por Questao — regra
     * implícita indispensável para a correção automática dos simulados
     * (ver itens 2.4 e 7.1: SimuladoAlunoService.finalizar depende de haver
     * exatamente uma alternativa correta por questão para apurar acertos).
     */
    @Transactional
    public Alternativa salvar(Alternativa obj) {
        validarCorretaUnica(obj, null);
        return repository.save(obj);
    }

    @Transactional
    public Alternativa atualizar(Long id, Alternativa obj) {
        obj.setId(id);
        validarCorretaUnica(obj, id);
        return repository.save(obj);
    }

    public void deletar(Long id) { repository.deleteById(id); }

    private void validarCorretaUnica(Alternativa obj, Long ignorarId) {
        if (!Boolean.TRUE.equals(obj.getCorreta()) || obj.getQuestao() == null || obj.getQuestao().getId() == null) {
            return;
        }
        boolean existeOutraCorreta = repository.findByQuestaoIdAndCorretaTrue(obj.getQuestao().getId()).stream()
                .anyMatch(a -> ignorarId == null || !a.getId().equals(ignorarId));
        if (existeOutraCorreta) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Esta questão já possui uma alternativa marcada como correta.");
        }
    }
}
