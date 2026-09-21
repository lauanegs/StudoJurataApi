package studojurata_api.security;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import studojurata_api.model.Usuario;
import studojurata_api.model.enums.TipoUsuario;

/**
 * Proteção contra IDOR em endpoints indexados por aluno ou por turma: sem ela,
 * um aluno poderia trocar o id e ver notas, respostas, frequência ou gastar
 * moedas de outro, e um professor poderia ler dados de alunos de outra turma.
 *
 * <p>Leitura e escrita têm regras diferentes de propósito:
 * <ul>
 *   <li>{@link #garantir(Long)} (leitura) — o professor acessa alunos das suas
 *       turmas, porque acompanhar o desempenho deles é a função dele;</li>
 *   <li>{@link #garantirEscritaDoAluno(Long)} (escrita) — só o próprio aluno age
 *       em nome dele; professor tentando finalizar tentativa ou comprar/equipar
 *       skin recebe 403;</li>
 *   <li>{@link #garantirAcessoDeGestao()} — recursos agregados de gestão
 *       pedagógica, sem uso pelo aluno;</li>
 *   <li>{@link #garantirAcessoATurma(Long)} — dados recortados por turma.</li>
 * </ul>
 *
 * <p>O ADMINISTRADOR continua autorizado dentro das fronteiras de escola já
 * existentes (o recorte por escola é do {@link EscolaContext} e das consultas
 * por escola nos services de gestão). Sem autenticação, todos os métodos falham
 * fechado com 403.
 */
@Component
@RequiredArgsConstructor
public class AlunoAccessGuard {

    private final EscopoProfessor escopoProfessor;

    /**
     * Autoriza <b>leitura</b> de dados de um aluno: o próprio aluno, o professor
     * que leciona para ele ou o administrador.
     */
    public void garantir(Long alunoId) {
        Usuario usuario = principalAutenticado().getUsuario();

        if (usuario.getTipoUsuario() == TipoUsuario.ADMINISTRADOR) {
            return;
        }

        if (usuario.getTipoUsuario() == TipoUsuario.ALUNO) {
            if (!alunoLogadoEh(usuario, alunoId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Você só pode acessar os seus próprios dados.");
            }
            return;
        }

        // PROFESSOR: só alunos das suas turmas. Custo fixo de consultas, sem 1+N.
        Long professorId = professorLogadoId(usuario);
        if (professorId == null || !escopoProfessor.lecionaPara(professorId, alunoId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Você só pode acessar alunos das suas turmas.");
        }
    }

    /**
     * Autoriza <b>escrita</b> em nome de um aluno: apenas o próprio aluno (ou o
     * administrador). O professor não finaliza tentativa nem compra skin para
     * aluno.
     */
    public void garantirEscritaDoAluno(Long alunoId) {
        Usuario usuario = principalAutenticado().getUsuario();

        if (usuario.getTipoUsuario() == TipoUsuario.ADMINISTRADOR) {
            return;
        }

        if (usuario.getTipoUsuario() == TipoUsuario.ALUNO && alunoLogadoEh(usuario, alunoId)) {
            return;
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Apenas o próprio aluno pode executar esta ação.");
    }

    /**
     * Autoriza dados recortados por turma: o professor dono da turma ou o
     * administrador. O aluno não tem uso legítimo para listagens de turma.
     */
    public void garantirAcessoATurma(Long turmaId) {
        Usuario usuario = principalAutenticado().getUsuario();

        if (usuario.getTipoUsuario() == TipoUsuario.ADMINISTRADOR) {
            return;
        }

        if (usuario.getTipoUsuario() != TipoUsuario.PROFESSOR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Apenas professor ou administrador pode acessar dados de turma.");
        }

        Long professorId = professorLogadoId(usuario);
        if (turmaId == null || professorId == null
                || !escopoProfessor.turmaIdsDoProfessor(professorId).contains(turmaId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Você só pode acessar as suas turmas.");
        }
    }

    /** Recursos agregados de gestão pedagógica, que o aluno não consome. */
    public void garantirAcessoDeGestao() {
        Usuario usuario = principalAutenticado().getUsuario();

        if (usuario.getTipoUsuario() == TipoUsuario.PROFESSOR
                || usuario.getTipoUsuario() == TipoUsuario.ADMINISTRADOR) {
            return;
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Apenas professor ou administrador pode acessar este recurso.");
    }

    private boolean alunoLogadoEh(Usuario usuario, Long alunoId) {
        return alunoId != null
                && usuario.getAluno() != null
                && usuario.getAluno().getId() != null
                && usuario.getAluno().getId().equals(alunoId);
    }

    private Long professorLogadoId(Usuario usuario) {
        return usuario.getProfessor() != null ? usuario.getProfessor().getId() : null;
    }

    /**
     * Principal autenticado ou 403. Não deveria acontecer atrás do Spring
     * Security (as rotas já exigem autenticação), mas falha fechado.
     */
    private CustomUserDetails principalAutenticado() {
        CustomUserDetails principal = usuarioLogado();
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Não autenticado.");
        }
        return principal;
    }

    private CustomUserDetails usuarioLogado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            return null;
        }
        return (CustomUserDetails) authentication.getPrincipal();
    }
}
