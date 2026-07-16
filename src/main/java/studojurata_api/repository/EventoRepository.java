package studojurata_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.Evento;

import java.util.List;

public interface EventoRepository extends JpaRepository<Evento, Long> {
    List<Evento> findByConcluidoOrderByDataHorarioAsc(Boolean concluido);
    List<Evento> findAllByOrderByDataHorarioAsc();
}
