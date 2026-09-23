package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import studojurata_api.exception.RequisicaoInvalidaException;
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

    /**
     * Usuário da mesma escola do logado. A validação fica aqui porque
     * {@code atualizar}, {@code deletar} e {@code ativar} passam por este método
     * — é o único ponto que todos compartilham.
     */
    public Usuario buscar(Long id) {
        Usuario usuario = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário " + id + " não encontrado."));
        exigirMesmaEscola(usuario);
        return usuario;
    }

    public Usuario salvar(Usuario obj) {
        exigirEscolaDoCorpo(obj);
        obj.setSenha(passwordEncoder.encode(obj.getSenha()));
        if (obj.getStatus() == null) obj.setStatus(StatusAtivoInativo.ATIVO);
        return repository.save(obj);
    }

    /** Só gera hash quando uma nova senha foi enviada, para não aplicar hash sobre hash. */
    public Usuario atualizar(Long id, Usuario obj) {
        Usuario existente = buscar(id);
        obj.setId(id);
        // Um usuário nunca muda de escola (mesma regra do CursoService), então a
        // escola do corpo não é aceita como origem de escrita.
        obj.setEscola(existente.getEscola());
        if (obj.getSenha() != null && !obj.getSenha().isBlank()) {
            obj.setSenha(passwordEncoder.encode(obj.getSenha()));
        } else {
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

    /** 403 quando o alvo é de outra escola — mesmo critério de CursoService. */
    private void exigirMesmaEscola(Usuario alvo) {
        Long escolaId = escolaContext.escolaAtualId();
        if (escolaId != null && alvo.getEscola() != null && !escolaId.equals(alvo.getEscola().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Este usuário pertence a outra escola.");
        }
    }

    /**
     * Criação: o corpo precisa apontar para a escola do logado. Sem escola
     * resolvida no contexto (cadastro inicial, antes de existir Escola) o
     * comportamento antigo é preservado.
     */
    private void exigirEscolaDoCorpo(Usuario obj) {
        Long escolaId = escolaContext.escolaAtualId();
        if (escolaId == null) {
            return;
        }

        Long escolaDoCorpo = obj.getEscola() != null ? obj.getEscola().getId() : null;
        if (escolaDoCorpo == null) {
            throw new RequisicaoInvalidaException("Escola é obrigatória para cadastrar usuário.");
        }
        if (!escolaId.equals(escolaDoCorpo)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Este usuário pertence a outra escola.");
        }
    }
}
