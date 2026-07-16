package studojurata_api.repository.gamificacao;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.gamificacao.SkinAluno;

import java.util.List;
import java.util.Optional;

public interface SkinAlunoRepository extends JpaRepository<SkinAluno, Long> {
    List<SkinAluno> findByAluno_Id(Long alunoId);
    Optional<SkinAluno> findByAluno_IdAndSkin_Id(Long alunoId, Long skinId);
    Optional<SkinAluno> findByAluno_IdAndAtivaTrue(Long alunoId);
    boolean existsByAluno_IdAndSkin_Id(Long alunoId, Long skinId);
}
