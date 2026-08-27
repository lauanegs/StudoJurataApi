package studojurata_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.TurmaDisciplinaSubstituto;

import java.util.List;

public interface TurmaDisciplinaSubstitutoRepository extends JpaRepository<TurmaDisciplinaSubstituto, Long> {
    List<TurmaDisciplinaSubstituto> findByTurmaDisciplina_Id(Long turmaDisciplinaId);

    /** Usado em ProfessorService.turmasLecionadas para incluir onde o professor é substituto. */
    List<TurmaDisciplinaSubstituto> findByProfessor_Id(Long professorId);
}
