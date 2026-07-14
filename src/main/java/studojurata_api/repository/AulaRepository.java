package studojurata_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import studojurata_api.model.Aula;
import studojurata_api.model.enums.StatusAtivoInativo;

import java.util.List;

public interface AulaRepository extends JpaRepository<Aula, Long> {

    List<Aula> findByPlanoAula_IdOrderByOrdemAsc(Long planoAulaId);

    List<Aula> findByPlanoAula_IdAndStatus(Long planoAulaId, StatusAtivoInativo status);

    /** Aulas realizadas = aquelas cuja data de publicação já foi preenchida. */
    long countByPlanoAula_IdAndDataPublicacaoIsNotNull(Long planoAulaId);

    long countByPlanoAula_Id(Long planoAulaId);

    /** Soma da carga horária apenas das aulas já realizadas (para estatística de "carga horária realizada"). */
    @Query("select coalesce(sum(a.cargaHoraria), 0) from Aula a " +
            "where a.planoAula.id = :planoAulaId and a.dataPublicacao is not null")
    long somarCargaHorariaRealizada(@Param("planoAulaId") Long planoAulaId);
}
