package studojurata_api.security;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import studojurata_api.model.Usuario;
import studojurata_api.model.enums.TipoUsuario;

/**
 * Perfil de gestão (PROFESSOR ou ADMINISTRADOR) — o mesmo pré-requisito de
 * QuestaoService, AlternativaService, QuestaoConteudoService e NotaService.
 * A regra vivia copiada em cada service; aqui ela é uma só.
 *
 * <p>A mensagem é de quem chama, porque cada domínio fala do seu próprio
 * recurso ("...pode acessar questoes.", "...pode listar alternativas."). O 403
 * de cada service continua com o texto que já devolvia.
 *
 * <p>Decide só o perfil. Escopo — turma/disciplina do professor, aluno dono do
 * dado — continua nos guardas específicos ({@link AlunoAccessGuard},
 * {@link ProfessorAccessGuard}, {@link SimuladoAccessGuard},
 * {@link PlanejamentoAccessGuard}) e no {@link EscopoProfessor}.
 *
 * <p>Não é bean do Spring porque não guarda estado: o
 * {@link UsuarioAutenticado} já resolvido é o que chega por parâmetro, o que
 * mantém os services com o construtor que já tinham.
 */
public final class PerfilDeGestao {

    private PerfilDeGestao() {
    }

    /**
     * 403 com a mensagem recebida quando o usuário não é PROFESSOR nem
     * ADMINISTRADOR. Sem usuário também recusa — o mesmo falhar fechado dos
     * guardas.
     */
    public static void exigir(Usuario usuario, String mensagem) {
        if (usuario == null
                || (usuario.getTipoUsuario() != TipoUsuario.PROFESSOR
                        && usuario.getTipoUsuario() != TipoUsuario.ADMINISTRADOR)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, mensagem);
        }
    }
}
