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

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final EscolaContext escolaContext;

    /** Sem escola resolvível (antes do cadastro inicial da escola), não filtra. */
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

    /** Só gera hash quando uma nova senha foi enviada, para não aplicar hash sobre hash. */
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

    /** Soft-delete: revoga o acesso sem apagar quem fez o quê em AuditLog. */
    public void deletar(Long id) {
        Usuario usuario = buscar(id);
        usuario.setStatus(StatusAtivoInativo.INATIVO);
        repository.save(usuario);
    }

    public Usuario ativar(Long id) {
        Usuario usuario = buscar(id);
        usuario.setStatus(StatusAtivoInativo.ATIVO);
        return repository.save(usuario);
    }
}
