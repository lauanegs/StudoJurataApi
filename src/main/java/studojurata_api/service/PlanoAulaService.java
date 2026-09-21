package studojurata_api.service;

import studojurata_api.security.EscopoUsuario;
import studojurata_api.security.UsuarioAutenticado;
import studojurata_api.model.enums.TipoUsuario;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.exception.RequisicaoInvalidaException;
import studojurata_api.model.PlanoAula;
import studojurata_api.model.PlanoEnsino;
import studojurata_api.model.enums.StatusPlano;
import studojurata_api.repository.AulaRepository;
import studojurata_api.repository.PlanoAulaRepository;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PlanoAulaService {

    private final PlanoAulaRepository repository;
    private final AulaRepository aulaRepository;
    private final UsuarioAutenticado usuarioAutenticado;
    private final EscopoUsuario escopoUsuario;

    /**
     * Escopado: ADMINISTRADOR ve tudo; PROFESSOR e ALUNO veem apenas os planos
     * de aula dos seus vinculos. turmaDisciplina e obrigatorio no plano de aula,
     * entao nao ha caso "generico" a preservar.
     */
    public List<PlanoAula> listar() {
        var usuario = usuarioAutenticado.atual();

        if (usuario.getTipoUsuario() == TipoUsuario.ADMINISTRADOR) {
            return repository.findAll();
        }

        var vinculoIds = escopoUsuario.turmaDisciplinaIds();
        return vinculoIds.isEmpty() ? List.of() : repository.findByTurmaDisciplina_IdIn(vinculoIds);
    }

    public PlanoAula buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Plano de aula " + id + " não encontrado."));
    }

    public List<PlanoAula> listarPorTurmaDisciplina(Long turmaDisciplinaId) {
        return repository.findByTurmaDisciplina_Id(turmaDisciplinaId);
    }

    public List<PlanoAula> listarPorPlanoEnsino(Long planoEnsinoId) {
        return repository.findByPlanoEnsino_Id(planoEnsinoId);
    }

    @Transactional
    public PlanoAula salvar(PlanoAula obj) {
        validar(obj);
        if (obj.getStatus() == null) {
            obj.setStatus(StatusPlano.ATIVO);
        }
        return repository.save(obj);
    }

    @Transactional
    public PlanoAula atualizar(Long id, PlanoAula obj) {
        buscar(id);
        obj.setId(id);
        validar(obj);
        return repository.save(obj);
    }

    /**
     * O plano de aula nasce junto com o plano de ensino que já tem turma. Não
     * gera de novo se já existir, nem para plano de ensino genérico.
     */
    @Transactional
    public void gerarSeNecessario(PlanoEnsino planoEnsino) {
        if (planoEnsino.getTurmaDisciplina() == null) return;
        if (!repository.findByPlanoEnsino_Id(planoEnsino.getId()).isEmpty()) return;

        PlanoAula planoAula = new PlanoAula();
        planoAula.setPlanoEnsino(planoEnsino);
        planoAula.setTurmaDisciplina(planoEnsino.getTurmaDisciplina());
        salvar(planoAula);
    }

    /** Soft delete: vira CONCLUIDO, preservando aulas e conteúdos vinculados. */
    @Transactional
    public void deletar(Long id) {
        PlanoAula obj = buscar(id);
        obj.setStatus(StatusPlano.CONCLUIDO);
        repository.save(obj);
    }

    public Map<String, Object> estatisticas(Long planoAulaId) {
        PlanoAula planoAula = buscar(planoAulaId);
        long totalPrevisto = aulaRepository.countByPlanoAula_Id(planoAulaId);
        long realizadas = aulaRepository.countByPlanoAula_IdAndDataPublicacaoIsNotNull(planoAulaId);
        double cargaHorariaRealizada = aulaRepository.somarCargaHorariaRealizada(planoAulaId);
        Integer cargaHorariaPrevista = planoAula.getPlanoEnsino() != null
                ? planoAula.getPlanoEnsino().getCargaHoraria()
                : null;

        Map<String, Object> resultado = new java.util.HashMap<>();
        resultado.put("aulasRealizadas", realizadas);
        resultado.put("totalAulasPrevistas", totalPrevisto);
        resultado.put("cargaHorariaRealizada", cargaHorariaRealizada);
        resultado.put("cargaHorariaPrevista", cargaHorariaPrevista);
        return resultado;
    }

    private void validar(PlanoAula obj) {
        if (obj.getTurmaDisciplina() == null || obj.getTurmaDisciplina().getId() == null) {
            throw new RequisicaoInvalidaException("Turma/Disciplina é obrigatória para o plano de aula.");
        }
        if (obj.getPlanoEnsino() == null || obj.getPlanoEnsino().getId() == null) {
            throw new RequisicaoInvalidaException("Plano de ensino é obrigatório para o plano de aula.");
        }

        // 1:1 entre PlanoEnsino e PlanoAula.
        boolean jaExistePlanoParaEsseEnsino = repository.findByPlanoEnsino_Id(obj.getPlanoEnsino().getId()).stream()
                .anyMatch(existente -> !existente.getId().equals(obj.getId()));
        if (jaExistePlanoParaEsseEnsino) {
            throw new RequisicaoInvalidaException(
                    "Este plano de ensino já tem um plano de aula vinculado.");
        }

        // Um único plano ATIVO por turma+disciplina, para não haver dois ciclos
        // simultâneos; planos CONCLUIDO ficam como histórico.
        StatusPlano statusFinal = obj.getStatus() != null ? obj.getStatus() : StatusPlano.ATIVO;
        if (statusFinal == StatusPlano.ATIVO) {
            boolean jaTemAtivoNaTurmaDisciplina = repository
                    .findByTurmaDisciplina_Id(obj.getTurmaDisciplina().getId()).stream()
                    .anyMatch(existente -> !existente.getId().equals(obj.getId())
                            && existente.getStatus() == StatusPlano.ATIVO);
            if (jaTemAtivoNaTurmaDisciplina) {
                throw new RequisicaoInvalidaException(
                        "Esta turma já tem um plano de aula ativo para esta disciplina.");
            }
        }
    }
}
