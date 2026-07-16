package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import studojurata_api.model.Aula;
import studojurata_api.model.PlanoAula;
import studojurata_api.model.enums.AcaoAuditoria;
import studojurata_api.model.enums.StatusAtivoInativo;
import studojurata_api.repository.AulaRepository;
import studojurata_api.repository.PlanoAulaRepository;

import java.time.LocalDate;
import java.util.List;

/** Correção 2.9: Aula é uma das 3 entidades priorizadas para AuditLog (junto com Nota e SimuladoAluno). */
@Service
@RequiredArgsConstructor
public class AulaService {

    private final AulaRepository repository;
    private final PlanoAulaRepository planoAulaRepository;
    private final AuditLogService auditLogService;

    public List<Aula> listar() { return repository.findAll(); }

    public Aula buscar(Long id) { return repository.findById(id).orElseThrow(); }

    /** Aulas de um plano de aula, na ordem em que devem ser ministradas. */
    public List<Aula> listarPorPlanoAula(Long planoAulaId) {
        return repository.findByPlanoAula_IdOrderByOrdemAsc(planoAulaId);
    }

    @Transactional
    public Aula salvar(Aula obj) {
        validar(obj);
        if (obj.getStatus() == null) {
            obj.setStatus(StatusAtivoInativo.ATIVO);
        }
        Aula salva = repository.save(obj);
        auditLogService.registrar("Aula", salva.getId(), AcaoAuditoria.CRIACAO, "Aula criada: " + salva.getTitulo());
        return salva;
    }

    @Transactional
    public Aula atualizar(Long id, Aula obj) {
        buscar(id);
        obj.setId(id);
        validar(obj);
        Aula salva = repository.save(obj);
        auditLogService.registrar("Aula", salva.getId(), AcaoAuditoria.ATUALIZACAO, "Aula atualizada: " + salva.getTitulo());
        return salva;
    }

    /**
     * Marca a aula como efetivamente ministrada, preenchendo a data de
     * publicação (correção 2.5/interface: "data que realmente foi passada
     * a aula"). É a partir desse campo que as estatísticas de "aulas
     * realizadas" e "carga horária realizada" do plano de aula são
     * calculadas.
     */
    @Transactional
    public Aula publicar(Long id, LocalDate dataPublicacao) {
        Aula aula = buscar(id);
        aula.setDataPublicacao(dataPublicacao != null ? dataPublicacao : LocalDate.now());
        return repository.save(aula);
    }

    /**
     * Soft delete (correção 4.3): mantém o registro (e a frequência /
     * conteúdos já vinculados a ele) para preservar o histórico
     * pedagógico, apenas marcando a aula como INATIVA.
     */
    @Transactional
    public void deletar(Long id) {
        Aula obj = buscar(id);
        obj.setStatus(StatusAtivoInativo.INATIVO);
        repository.save(obj);
        auditLogService.registrar("Aula", id, AcaoAuditoria.EXCLUSAO, "Aula marcada como INATIVA (soft-delete).");
    }

    private void validar(Aula obj) {
        if (obj.getPlanoAula() == null || obj.getPlanoAula().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Plano de aula é obrigatório para a aula.");
        }
        PlanoAula planoAula = planoAulaRepository.findById(obj.getPlanoAula().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plano de aula não encontrado."));
        obj.setPlanoAula(planoAula);
    }
}
