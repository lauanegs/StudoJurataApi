package studojurata_api.security;

import java.util.Set;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import studojurata_api.model.Usuario;
import studojurata_api.model.enums.TipoUsuario;

/**
 * Vínculos turma+disciplina que o usuário logado pode enxergar — o recorte
 * comum das listagens pedagógicas (planos, aulas, conteúdo e, nos blocos
 * seguintes, simulados e questões).
 *
 * <p>Regra única, num só lugar, para que professor e aluno não ganhem critérios
 * divergentes entre telas:
 * <ul>
 *   <li>PROFESSOR: vínculos em que é titular;</li>
 *   <li>ALUNO: vínculos das turmas em que está matriculado;</li>
 *   <li>qualquer outro perfil (inclusive ADMINISTRADOR): conjunto vazio — o
 *       admin é tratado por quem chama, com o comportamento irrestrito de
 *       sempre, e a lista vazia aqui nunca significa "todos".</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class EscopoUsuario {

    private final EscopoProfessor escopoProfessor;
    private final EscopoAluno escopoAluno;
    private final UsuarioAutenticado usuarioAutenticado;

    /** Vínculos do usuário logado; vazio quando não for professor nem aluno. */
    public Set<Long> turmaDisciplinaIds() {
        Usuario usuario = usuarioAutenticado.opcional().orElse(null);
        if (usuario == null) {
            return Set.of();
        }
        if (usuario.getTipoUsuario() == TipoUsuario.PROFESSOR) {
            Long professorId = usuario.getProfessor() != null ? usuario.getProfessor().getId() : null;
            return escopoProfessor.turmaDisciplinaIdsDoProfessor(professorId);
        }
        if (usuario.getTipoUsuario() == TipoUsuario.ALUNO) {
            Long alunoId = usuario.getAluno() != null ? usuario.getAluno().getId() : null;
            return escopoAluno.turmaDisciplinaIdsDoAluno(alunoId);
        }
        return Set.of();
    }
}
