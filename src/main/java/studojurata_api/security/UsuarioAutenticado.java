package studojurata_api.security;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import studojurata_api.model.Usuario;
import studojurata_api.model.enums.TipoUsuario;

/**
 * Quem está autenticado nesta requisição.
 *
 * <p>Existe para que services possam escopar listagens sem depender do
 * controller: antes do C2, o único jeito de saber "quem está perguntando" era
 * o parâmetro da URL, que é justamente o dado manipulável pelo cliente.
 *
 * <p>Responsabilidade única: identificar o usuário logado. Decidir autorização
 * continua sendo papel dos guardas ({@link AlunoAccessGuard},
 * {@link ProfessorAccessGuard}) e resolver escopo, do
 * {@link EscopoProfessor}.
 */
@Component
public class UsuarioAutenticado {

    /** Usuário logado; sem principal reconhecido falha fechado com 403. */
    public Usuario atual() {
        return opcional().orElseThrow(
                () -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Não autenticado."));
    }

    public Optional<Usuario> opcional() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails principal)) {
            return Optional.empty();
        }
        return Optional.ofNullable(principal.getUsuario());
    }

    public boolean ehAdministrador() {
        return opcional()
                .map(usuario -> usuario.getTipoUsuario() == TipoUsuario.ADMINISTRADOR)
                .orElse(false);
    }

    /** Id do Professor vinculado ao usuário logado, quando o perfil for PROFESSOR. */
    public Long professorId() {
        return opcional()
                .filter(usuario -> usuario.getTipoUsuario() == TipoUsuario.PROFESSOR)
                .map(Usuario::getProfessor)
                .map(professor -> professor.getId())
                .orElse(null);
    }

    /** Id do Aluno vinculado ao usuário logado, quando o perfil for ALUNO. */
    public Long alunoId() {
        return opcional()
                .filter(usuario -> usuario.getTipoUsuario() == TipoUsuario.ALUNO)
                .map(Usuario::getAluno)
                .map(aluno -> aluno.getId())
                .orElse(null);
    }
}
