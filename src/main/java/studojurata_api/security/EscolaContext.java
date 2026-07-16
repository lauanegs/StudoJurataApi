package studojurata_api.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Correção 2.2 da Terceira Análise Crítica ("Escola existe como coluna, mas
 * o isolamento multi-tenant não foi implementado"): ponto único para obter
 * a escola do usuário autenticado, usado para filtrar listagens de Turma,
 * Disciplina e Usuario por escola em vez de devolver os dados de todas as
 * escolas para qualquer usuário autenticado.
 *
 * Retorna {@code null} quando não há usuário autenticado reconhecido ou
 * quando o usuário ainda não tem escola associada (ex.: primeiro
 * Administrador cadastrado antes de existir qualquer Escola) — quem chama
 * deve tratar {@code null} como "não filtrar", para não travar o sistema
 * antes do cadastro inicial da escola.
 */
@Component
public class EscolaContext {

    public Long escolaAtualId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails principal)) {
            return null;
        }
        var usuario = principal.getUsuario();
        return usuario.getEscola() != null ? usuario.getEscola().getId() : null;
    }
}
