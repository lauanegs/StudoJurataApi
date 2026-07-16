package studojurata_api.repository.gamificacao;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.gamificacao.Skin;

import java.util.List;

public interface SkinRepository extends JpaRepository<Skin, Long> {
    List<Skin> findByDisponivelTrue();
    boolean existsByNome(String nome);
}
