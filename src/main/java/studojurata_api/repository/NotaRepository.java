package studojurata_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.Nota;

public interface NotaRepository extends JpaRepository<Nota, Long> {
}