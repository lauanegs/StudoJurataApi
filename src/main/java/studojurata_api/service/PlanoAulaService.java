package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import studojurata_api.model.PlanoAula;
import studojurata_api.model.enums.StatusAtivoInativo;
import studojurata_api.repository.AulaRepository;
import studojurata_api.repository.PlanoAulaRepository;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PlanoAulaService {

    private final PlanoAulaRepository repository;
    private final AulaRepository aulaRepository;

    public List<PlanoAula> listar() { return repository.findAll(); }

    public PlanoAula buscar(Long id) { return repository.findById(id).orElseThrow(); }

    public List<PlanoAula> listarPorTurmaDisciplina(Long turmaDisciplinaId) {
        return repository.findByTurmaDisciplina_Id(turmaDisciplinaId);
    }

    @Transactional
    public PlanoAula salvar(PlanoAula obj) {
        validar(obj);
        if (obj.getStatus() == null) {
            obj.setStatus(StatusAtivoInativo.ATIVO);
        }
        return repository.save(obj);
    }

    @Transactional
    public PlanoAula atualizar(Long id, PlanoAula obj) {
        buscar(id);
        obj.setId(id);
        validar(obj);
        return repository.save(obj);
    }

    /**
     * Soft delete (correção 4.3): mantém o registro para preservar o
     * histórico de aulas/conteúdos vinculados, apenas marcando o plano
     * como INATIVO.
     */
    @Transactional
    public void deletar(Long id) {
        PlanoAula obj = buscar(id);
        obj.setStatus(StatusAtivoInativo.INATIVO);
        repository.save(obj);
    }

    /**
     * Estatísticas exibidas na tela "Aulas" do plano de aula: quantidade de
     * aulas realizadas em relação ao total previsto e carga horária
     * realizada (soma da carga horária das aulas já publicadas).
     */
    public Map<String, Object> estatisticas(Long planoAulaId) {
        buscar(planoAulaId);
        long totalPrevisto = aulaRepository.countByPlanoAula_Id(planoAulaId);
        long realizadas = aulaRepository.countByPlanoAula_IdAndDataPublicacaoIsNotNull(planoAulaId);
        long cargaHorariaRealizada = aulaRepository.somarCargaHorariaRealizada(planoAulaId);
        return Map.of(
                "aulasRealizadas", realizadas,
                "totalAulasPrevistas", totalPrevisto,
                "cargaHorariaRealizada", cargaHorariaRealizada
        );
    }

    private void validar(PlanoAula obj) {
        if (obj.getTurmaDisciplina() == null || obj.getTurmaDisciplina().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Turma/Disciplina é obrigatória para o plano de aula.");
        }
        if (obj.getPlanoEnsino() == null || obj.getPlanoEnsino().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Plano de ensino é obrigatório para o plano de aula.");
        }
    }
}
