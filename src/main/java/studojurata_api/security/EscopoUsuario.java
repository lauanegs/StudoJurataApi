package studojurata_api.security;

import java.util.Set;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import studojurata_api.model.pessoa.Usuario;
import studojurata_api.model.enums.TipoUsuario;

/** Vínculos turma+disciplina que o usuário logado pode enxergar */
@Component
@RequiredArgsConstructor
public class EscopoUsuario {
    private final EscopoProfessor escopoProfessor;
    private final EscopoAluno escopoAluno;
    private final UsuarioAutenticado usuarioAutenticado;

    /** Vínculos do usuário logado; vazio quando não for professor nem aluno */
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
