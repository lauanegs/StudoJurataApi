package studojurata_api.repository.pessoa;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import studojurata_api.model.pessoa.Aluno;

public interface AlunoRepository extends JpaRepository<Aluno, Long> {
    boolean existsByMatricula(String matricula);

    @Query("select max(a.matricula) from Aluno a where a.matricula like concat(:prefixo, '%')")
    Optional<String> buscarUltimaMatriculaComPrefixo(@Param("prefixo") String prefixo);
}
