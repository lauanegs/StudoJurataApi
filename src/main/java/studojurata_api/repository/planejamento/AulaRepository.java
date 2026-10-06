package studojurata_api.repository.planejamento;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import studojurata_api.model.planejamento.Aula;
import studojurata_api.model.enums.StatusAtivoInativo;

import java.util.List;

public interface AulaRepository extends JpaRepository<Aula, Long> {
    List<Aula> findByPlanoAula_IdOrderByOrdemAsc(Long planoAulaId);

    List<Aula> findByPlanoAula_IdAndStatus(Long planoAulaId, StatusAtivoInativo status);

    long countByPlanoAula_IdAndDataPublicacaoIsNotNull(Long planoAulaId);

    long countByPlanoAula_Id(Long planoAulaId);

    @Query("select coalesce(sum(a.cargaHoraria), 0.0) from Aula a " +
            "where a.planoAula.id = :planoAulaId and a.dataPublicacao is not null")
    double somarCargaHorariaRealizada(@Param("planoAulaId") Long planoAulaId);
}
