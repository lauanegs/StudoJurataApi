package studojurata_api.service;

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

    public List<PlanoAula> listar() { return repository.findAll(); }

    public PlanoAula buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Plano de aula " + id + " não encontrado."));
    }

    public List<PlanoAula> listarPorTurmaDisciplina(Long turmaDisciplinaId) {
        return repository.findByTurmaDisciplina_Id(turmaDisciplinaId);
    }

    /** Usado pelo botão "Plano de aula" dentro do Plano de Ensino (ver PlanoEnsinoFormulario no front). */
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
     * Pedido explícito: não existe mais tela pra criar plano de aula na mão
     * — nasce sozinho junto com o plano de ensino (ver
     * PlanoEnsinoService.salvar/atualizar), sempre que ele já tiver
     * turma/disciplina definida. Reaproveita salvar() (mesma validação e
     * status padrão) só que sem stack trace pro professor: não gera de
     * novo se um plano de aula pra este plano de ensino já existir, e não
     * gera nada pra plano de ensino genérico (sem turma).
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

    /**
     * Soft delete (correção 4.3): mantém o registro para preservar o
     * histórico de aulas/conteúdos vinculados, marcando o plano como
     * CONCLUIDO (matrícula cíclica: a turma pode receber um novo plano).
     */
    @Transactional
    public void deletar(Long id) {
        PlanoAula obj = buscar(id);
        obj.setStatus(StatusPlano.CONCLUIDO);
        repository.save(obj);
    }

    /**
     * Estatísticas exibidas na tela "Aulas" do plano de aula: quantidade de
     * aulas realizadas em relação ao total previsto, carga horária
     * realizada (soma da carga horária das aulas já publicadas) e carga
     * horária prevista (PlanoEnsino.cargaHoraria) — sem essa segunda,
     * "carga horária realizada" era um número solto, sem "de quanto".
     */
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

        // Pedido explícito: relação 1-para-1 entre PlanoEnsino e PlanoAula —
        // cada plano de ensino tem no máximo um plano de aula (e vice-versa,
        // já garantido estruturalmente pelo FK único em PlanoAula).
        boolean jaExistePlanoParaEsseEnsino = repository.findByPlanoEnsino_Id(obj.getPlanoEnsino().getId()).stream()
                .anyMatch(existente -> !existente.getId().equals(obj.getId()));
        if (jaExistePlanoParaEsseEnsino) {
            throw new RequisicaoInvalidaException(
                    "Este plano de ensino já tem um plano de aula vinculado.");
        }

        // Pedido explícito: uma turma só pode ter um plano de aula ATIVO por
        // disciplina — evita dois ciclos "correndo" ao mesmo tempo pra mesma
        // combinação turma+disciplina. Planos CONCLUIDO não contam (matrícula
        // cíclica: o histórico fica, só não pode haver dois ativos juntos).
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
