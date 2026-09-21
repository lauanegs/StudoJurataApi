package studojurata_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.Simulado;

import java.util.Collection;
import java.util.List;

public interface SimuladoRepository extends JpaRepository<Simulado, Long> {

    /** Simulados das turmas informadas — escopo do professor/aluno. */
    List<Simulado> findByTurma_IdIn(Collection<Long> turmaIds);

    /**
     * Simulados sem turma (orfaos). Preservados por decisao D-C1 enquanto nao
     * houver levantamento de uso e regra definitiva.
     */
    List<Simulado> findByTurmaIsNull();
}
