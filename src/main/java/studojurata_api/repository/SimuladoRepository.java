package studojurata_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.Simulado;
import studojurata_api.model.enums.StatusSimulado;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface SimuladoRepository extends JpaRepository<Simulado, Long> {

    /** Simulados das turmas informadas — escopo do professor/aluno. */
    List<Simulado> findByTurma_IdIn(Collection<Long> turmaIds);

    /** Rascunhos cuja data de início já chegou — candidatos à publicação automática. */
    List<Simulado> findByStatusAndDataInicioLessThanEqual(StatusSimulado status, LocalDateTime limite);

    /** Publicados cuja data final já passou — candidatos ao encerramento automático. */
    List<Simulado> findByStatusAndDataFimLessThanEqual(StatusSimulado status, LocalDateTime limite);
}
