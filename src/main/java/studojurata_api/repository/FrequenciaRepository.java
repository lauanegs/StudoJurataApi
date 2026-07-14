package studojurata_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.Frequencia;

import java.util.List;
import java.util.Optional;

public interface FrequenciaRepository extends JpaRepository<Frequencia, Long> {

    List<Frequencia> findByAula_Id(Long aulaId);

    List<Frequencia> findByAluno_IdOrderByAula_DataPrevistaDesc(Long alunoId);

    Optional<Frequencia> findByAluno_IdAndAula_Id(Long alunoId, Long aulaId);

    boolean existsByAluno_IdAndAula_Id(Long alunoId, Long aulaId);

    long countByAula_IdAndPresenteTrue(Long aulaId);

    long countByAluno_IdAndPresenteFalse(Long alunoId);
}
