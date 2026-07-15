package studojurata_api.ia.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.ia.model.RevisaoConteudo;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RevisaoConteudoRepository extends JpaRepository<RevisaoConteudo, Long> {

    List<RevisaoConteudo> findByAlunoId(Long alunoId);

    Optional<RevisaoConteudo> findByAlunoIdAndConteudoPlanoId(Long alunoId, Long conteudoPlanoId);

    /** Repetição espaçada devida: dataProximoReforco já atingida (ver item 1.5). */
    List<RevisaoConteudo> findByDataProximoReforcoLessThanEqual(LocalDate data);

    List<RevisaoConteudo> findByAlunoIdAndDataProximoReforcoLessThanEqual(Long alunoId, LocalDate data);
}
