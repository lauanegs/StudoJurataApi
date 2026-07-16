package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.model.Usuario;
import studojurata_api.model.enums.StatusAtivoInativo;
import studojurata_api.repository.UsuarioRepository;
import studojurata_api.security.EscolaContext;

import java.util.List;

/**
 * Correção 5.2 da Segunda Análise Crítica ("UsuarioService é código morto e
 * não hasheia a senha"): este service deixa de ser código morto — passa a
 * ser o único caminho usado por UsuarioController — e passa a hashear a
 * senha aqui (regra de negócio pertence ao service, não ao controller).
 * Antes havia dois caminhos de escrita para Usuario: o controller (que
 * hasheava certo) e este service (que não hasheava, e nunca era chamado).
 * Agora existe um único caminho, e ele está correto.
 *
 * Correção 2.2 da Terceira Análise Crítica (isolamento multi-tenant):
 * listar() filtra pela escola do usuário autenticado — antes, qualquer
 * Administrador via a lista de usuários de todas as escolas.
 */
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final EscolaContext escolaContext;

    /** Filtra pela escola do usuário autenticado; se não houver escola resolvível, devolve tudo (bootstrapping). */
    public List<Usuario> listar() {
        Long escolaId = escolaContext.escolaAtualId();
        return escolaId != null ? repository.findByEscola_Id(escolaId) : repository.findAll();
    }

    public Usuario buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário " + id + " não encontrado."));
    }

    public Usuario salvar(Usuario obj) {
        obj.setSenha(passwordEncoder.encode(obj.getSenha()));
        if (obj.getStatus() == null) obj.setStatus(StatusAtivoInativo.ATIVO);
        return repository.save(obj);
    }

    /**
     * Só re-hasheia a senha se uma nova senha em texto puro foi enviada;
     * evita hashear novamente um hash já persistido (item 10.1).
     */
    public Usuario atualizar(Long id, Usuario obj) {
        obj.setId(id);
        if (obj.getSenha() != null && !obj.getSenha().isBlank()) {
            obj.setSenha(passwordEncoder.encode(obj.getSenha()));
        } else {
            Usuario existente = buscar(id);
            obj.setSenha(existente.getSenha());
        }
        return repository.save(obj);
    }

    /** Soft-delete (item 4.3/5.1): revoga o acesso sem apagar o histórico de quem fez o quê (AuditLog.usuario). */
    public void deletar(Long id) {
        Usuario usuario = buscar(id);
        usuario.setStatus(StatusAtivoInativo.INATIVO);
        repository.save(usuario);
    }
}
