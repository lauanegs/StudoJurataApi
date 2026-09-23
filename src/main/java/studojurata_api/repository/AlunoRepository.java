package studojurata_api.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import studojurata_api.model.Aluno;

public interface AlunoRepository extends JpaRepository<Aluno, Long> {

    boolean existsByMatricula(String matricula);

    /**
     * Última matrícula do ano, para o próximo código da sequência. O {@code max}
     * é textual e funciona porque as matrículas geradas têm largura fixa
     * (AAAA + 4 dígitos); códigos antigos fora do padrão só fazem a sequência
     * recomeçar, e o laço de colisão em {@code AlunoService} evita repetição.
     */
    @Query("select max(a.matricula) from Aluno a where a.matricula like concat(:prefixo, '%')")
    Optional<String> buscarUltimaMatriculaComPrefixo(@Param("prefixo") String prefixo);
}
