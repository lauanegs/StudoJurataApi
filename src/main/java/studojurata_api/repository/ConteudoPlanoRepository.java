package studojurata_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.ConteudoPlano;

import java.util.Collection;
import java.util.List;

public interface ConteudoPlanoRepository extends JpaRepository<ConteudoPlano, Long> {

    /** Conteúdos dos planos de ensino dos vínculos informados. */
    List<ConteudoPlano> findByPlanoEnsino_TurmaDisciplina_IdIn(Collection<Long> turmaDisciplinaIds);

    /** Conteúdos de plano genérico (sem turma/disciplina) — preservados (D-C2). */
    List<ConteudoPlano> findByPlanoEnsino_TurmaDisciplinaIsNull();

    /** Conteúdos sem plano de ensino — preservados enquanto não houver levantamento. */
    List<ConteudoPlano> findByPlanoEnsinoIsNull();
}
