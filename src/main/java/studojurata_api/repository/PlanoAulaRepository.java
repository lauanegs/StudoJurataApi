package studojurata_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.PlanoAula;

import java.util.Collection;
import java.util.List;

public interface PlanoAulaRepository extends JpaRepository<PlanoAula, Long> {

    List<PlanoAula> findByTurmaDisciplina_Id(Long turmaDisciplinaId);

    List<PlanoAula> findByPlanoEnsino_Id(Long planoEnsinoId);

    /** Planos de aula dos vínculos informados — escopo do professor/aluno. */
    List<PlanoAula> findByTurmaDisciplina_IdIn(Collection<Long> turmaDisciplinaIds);
}
