package studojurata_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.ResponsavelAluno;

import java.util.List;

public interface ResponsavelAlunoRepository extends JpaRepository<ResponsavelAluno, Long> {
    List<ResponsavelAluno> findByResponsavelId(Long responsavelId);
    List<ResponsavelAluno> findByAlunoId(Long alunoId);
}
