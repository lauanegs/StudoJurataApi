package studojurata_api.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import studojurata_api.model.enums.TipoUsuario;

/**
 * Correção 2.1 da Terceira Análise Crítica (IDOR em /notas e /gamificacao):
 * endpoints que recebem um {@code alunoId} livre na URL (histórico de notas,
 * pontuação, compra/equipar skin) precisam garantir que o próprio aluno só
 * acesse os seus dados — sem essa checagem, qualquer aluno autenticado podia
 * trocar o id na URL e ver notas ou gastar moedas de outro aluno.
 *
 * Professor e Administrador continuam com acesso irrestrito a qualquer
 * aluno, por necessidade de gestão pedagógica (consultar boletim, conceder
 * ajuste manual etc.).
 */
@Component
public class AlunoAccessGuard {

    /** Lança 403 se o usuário logado for um Aluno tentando acessar dados de outro aluno. */
    public void garantir(Long alunoId) {
        CustomUserDetails principal = usuarioLogado();
        if (principal == null) {
            // Não deveria acontecer atrás do Spring Security (rota já exige
            // autenticação), mas falha fechado por segurança.
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Não autenticado.");
        }

        var usuario = principal.getUsuario();
        if (usuario.getTipoUsuario() == TipoUsuario.ALUNO) {
            boolean mesmoAluno = usuario.getAluno() != null
                    && usuario.getAluno().getId() != null
                    && usuario.getAluno().getId().equals(alunoId);
            if (!mesmoAluno) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Você só pode acessar os seus próprios dados.");
            }
        }
        // PROFESSOR e ADMINISTRADOR: liberado.
    }

    private CustomUserDetails usuarioLogado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            return null;
        }
        return (CustomUserDetails) authentication.getPrincipal();
    }
}
