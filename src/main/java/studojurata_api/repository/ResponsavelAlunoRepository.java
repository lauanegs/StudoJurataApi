package studojurata_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import studojurata_api.model.ResponsavelAluno;

import java.util.List;

public interface ResponsavelAlunoRepository extends JpaRepository<ResponsavelAluno, Long> {
    List<ResponsavelAluno> findByResponsavelId(Long responsavelId);
    List<ResponsavelAluno> findByAlunoId(Long alunoId);

    /** Item 9.8: destinatários opt-in de notificação, por aluno (ex.: nova nota) ou para toda a escola (ex.: novo evento). */
    List<ResponsavelAluno> findByAlunoIdAndReceberNotificacoesTrue(Long alunoId);
    List<ResponsavelAluno> findByReceberNotificacoesTrue();
}
