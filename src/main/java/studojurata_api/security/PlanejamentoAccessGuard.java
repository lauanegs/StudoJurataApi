package studojurata_api.security;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;
import studojurata_api.model.enums.TipoUsuario;

/** Escopo do domínio de planejamento */
@Component
@RequiredArgsConstructor
public class PlanejamentoAccessGuard {
    private final UsuarioAutenticado usuarioAutenticado;
    private final EscopoUsuario escopoUsuario;
    private final EscopoProfessor escopoProfessor;

    /** Leitura: vínculo no escopo ou recurso sem vínculo (D-C2, visível a autenticados) */
    public void garantirLeitura(Long vinculoId, String mensagem) {
        if (usuarioAutenticado.ehAdministrador()) {
            return;
        }
        if (vinculoId == null || escopoUsuario.turmaDisciplinaIds().contains(vinculoId)) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, mensagem);
    }

    /** Escrita: perfil de gestão e vínculo no escopo (recurso sem vínculo é do ADMIN) */
    public void garantirEscrita(Long vinculoId, String mensagem) {
        if (usuarioAutenticado.ehAdministrador()) {
            return;
        }
        exigirPerfilDeEscrita();
        if (vinculoId == null || !escopoUsuario.turmaDisciplinaIds().contains(vinculoId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, mensagem);
        }
    }

    /** Escrita recortada por turma (criação de vínculo turma-disciplina) */
    public void garantirEscritaNaTurma(Long turmaId, String mensagem) {
        if (usuarioAutenticado.ehAdministrador()) {
            return;
        }
        exigirPerfilDeEscrita();
        Long professorId = usuarioAutenticado.professorId();
        if (turmaId == null || professorId == null
                || !escopoProfessor.turmaIdsDoProfessor(professorId).contains(turmaId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, mensagem);
        }
    }

    /** 403 para quem não é professor nem administrador (ALUNO não escreve planejamento) */
    public void exigirPerfilDeEscrita() {
        var usuario = usuarioAutenticado.atual();
        if (usuario.getTipoUsuario() != TipoUsuario.PROFESSOR
                && usuario.getTipoUsuario() != TipoUsuario.ADMINISTRADOR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Apenas professor ou administrador pode alterar o planejamento.");
        }
    }
}
