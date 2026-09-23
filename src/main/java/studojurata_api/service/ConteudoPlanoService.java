package studojurata_api.service;

import java.util.ArrayList;

import studojurata_api.security.EscopoUsuario;
import studojurata_api.security.PlanejamentoAccessGuard;
import studojurata_api.security.UsuarioAutenticado;
import studojurata_api.model.enums.TipoUsuario;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.exception.RegraNegocioException;
import studojurata_api.model.AulaConteudo;
import studojurata_api.model.ConteudoPlano;
import studojurata_api.model.enums.StatusAtivoInativo;
import studojurata_api.repository.AulaConteudoRepository;
import studojurata_api.repository.ConteudoPlanoRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConteudoPlanoService {

    private final ConteudoPlanoRepository repository;
    private final AulaConteudoRepository aulaConteudoRepository;
    private final UsuarioAutenticado usuarioAutenticado;
    private final EscopoUsuario escopoUsuario;
    private final PlanejamentoAccessGuard planejamentoAccessGuard;

    /**
     * Escopado: ADMINISTRADOR ve tudo; PROFESSOR e ALUNO veem os conteudos dos
     * planos dos seus vinculos. Conteudo de plano generico ou sem plano
     * permanece visivel, mesma cautela de D-C2 ate haver levantamento de uso.
     */
    public List<ConteudoPlano> listar() {
        var usuario = usuarioAutenticado.atual();

        if (usuario.getTipoUsuario() == TipoUsuario.ADMINISTRADOR) {
            return repository.findAll();
        }

        var vinculoIds = escopoUsuario.turmaDisciplinaIds();

        List<ConteudoPlano> visiveis = new ArrayList<>();
        if (!vinculoIds.isEmpty()) {
            visiveis.addAll(repository.findByPlanoEnsino_TurmaDisciplina_IdIn(vinculoIds));
        }
        visiveis.addAll(repository.findByPlanoEnsino_TurmaDisciplinaIsNull());
        visiveis.addAll(repository.findByPlanoEnsinoIsNull());
        return visiveis;
    }

    public ConteudoPlano buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Conteúdo do plano " + id + " não encontrado."));
    }

    /** Leitura individual com a mesma visibilidade da listagem (genéricos e sem plano por D-C2). */
    public ConteudoPlano buscarParaLeitura(Long id) {
        ConteudoPlano conteudo = buscar(id);
        planejamentoAccessGuard.garantirLeitura(vinculoId(conteudo),
                "Você só pode acessar conteúdos das suas turmas.");
        return conteudo;
    }

    public ConteudoPlano salvar(ConteudoPlano obj) {
        planejamentoAccessGuard.garantirEscrita(vinculoId(obj),
                "Você só pode criar conteúdos em planos de ensino das suas turmas.");
        if (obj.getStatus() == null) obj.setStatus(StatusAtivoInativo.ATIVO);
        return repository.save(obj);
    }

    public ConteudoPlano atualizar(Long id, ConteudoPlano obj) {
        // Escopo das duas pontas: o conteúdo atual e o plano enviado.
        planejamentoAccessGuard.garantirEscrita(vinculoId(buscar(id)),
                "Você só pode alterar conteúdos das suas turmas.");
        planejamentoAccessGuard.garantirEscrita(vinculoId(obj),
                "Você só pode mover conteúdos para planos de ensino das suas turmas.");
        obj.setId(id);
        return repository.save(obj);
    }

    /**
     * Soft-delete, recusado quando o conteúdo já foi ministrado (vinculado a
     * uma aula com dataPublicacao).
     */
    public void deletar(Long id) {
        ConteudoPlano conteudo = buscar(id);
        planejamentoAccessGuard.garantirEscrita(vinculoId(conteudo),
                "Você só pode inativar conteúdos das suas turmas.");

        boolean jaMinistrado = aulaConteudoRepository.findByConteudoPlano_Id(id).stream()
                .map(AulaConteudo::getAula)
                .anyMatch(aula -> aula != null && aula.getDataPublicacao() != null);

        if (jaMinistrado) {
            throw new RegraNegocioException(
                    "Este conteúdo já foi ministrado em alguma aula e não pode ser inativado.");
        }

        conteudo.setStatus(StatusAtivoInativo.INATIVO);
        repository.save(conteudo);
    }

    private static Long vinculoId(ConteudoPlano conteudo) {
        return conteudo != null && conteudo.getPlanoEnsino() != null && conteudo.getPlanoEnsino().getTurmaDisciplina() != null
                ? conteudo.getPlanoEnsino().getTurmaDisciplina().getId()
                : null;
    }
}
