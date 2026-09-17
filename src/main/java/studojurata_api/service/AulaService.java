package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import studojurata_api.dto.GerarAulasLoteRequest;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.exception.RequisicaoInvalidaException;
import studojurata_api.model.Aula;
import studojurata_api.model.HorarioTurma;
import studojurata_api.model.PlanoAula;
import studojurata_api.model.PlanoEnsino;
import studojurata_api.model.enums.AcaoAuditoria;
import studojurata_api.model.enums.DiaSemana;
import studojurata_api.model.enums.StatusAtivoInativo;
import studojurata_api.model.enums.StatusPlano;
import studojurata_api.repository.AulaRepository;
import studojurata_api.repository.HorarioTurmaRepository;
import studojurata_api.repository.PlanoAulaRepository;
import studojurata_api.repository.PlanoEnsinoRepository;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AulaService {

    /** Tolerância pra evitar falso positivo por arredondamento de double (ex.: 7.999999999 vs 8). */
    private static final double TOLERANCIA_CARGA_HORARIA = 0.01;

    private final AulaRepository repository;
    private final PlanoAulaRepository planoAulaRepository;
    private final PlanoEnsinoRepository planoEnsinoRepository;
    private final HorarioTurmaRepository horarioTurmaRepository;
    private final AuditLogService auditLogService;

    public Aula buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aula " + id + " não encontrada."));
    }

    public List<Aula> listarPorPlanoAula(Long planoAulaId) {
        return repository.findByPlanoAula_IdOrderByOrdemAsc(planoAulaId);
    }

    @Transactional
    public Aula salvar(Aula obj) {
        validar(obj);
        if (obj.getStatus() == null) {
            obj.setStatus(StatusAtivoInativo.ATIVO);
        }
        Aula salva = repository.save(obj);
        auditLogService.registrar("Aula", salva.getId(), AcaoAuditoria.CRIACAO, "Aula criada: " + salva.getTitulo());
        return salva;
    }

    @Transactional
    public Aula atualizar(Long id, Aula obj) {
        buscar(id);
        obj.setId(id);
        validar(obj);
        Aula salva = repository.save(obj);
        auditLogService.registrar("Aula", salva.getId(), AcaoAuditoria.ATUALIZACAO, "Aula atualizada: " + salva.getTitulo());
        return salva;
    }

    /** A data de publicação é a base das estatísticas de aulas realizadas do plano. */
    @Transactional
    public Aula publicar(Long id, LocalDate dataPublicacao) {
        Aula aula = buscar(id);
        aula.setDataPublicacao(dataPublicacao != null ? dataPublicacao : LocalDate.now());
        Aula salva = repository.save(aula);

        concluirPlanoSeUltimaAula(salva.getPlanoAula());

        return salva;
    }

    /**
     * Publicada a última aula ativa, conclui o plano de aula e o de ensino.
     * Nunca reabre um plano já concluído.
     */
    private void concluirPlanoSeUltimaAula(PlanoAula planoAula) {
        if (planoAula == null || planoAula.getStatus() == StatusPlano.CONCLUIDO) return;

        List<Aula> aulasAtivas = repository.findByPlanoAula_IdAndStatus(planoAula.getId(), StatusAtivoInativo.ATIVO);
        if (aulasAtivas.isEmpty() || aulasAtivas.stream().anyMatch(a -> a.getDataPublicacao() == null)) return;

        planoAula.setStatus(StatusPlano.CONCLUIDO);
        planoAulaRepository.save(planoAula);

        PlanoEnsino planoEnsino = planoAula.getPlanoEnsino();
        if (planoEnsino != null && planoEnsino.getStatus() != StatusPlano.CONCLUIDO) {
            planoEnsino.setStatus(StatusPlano.CONCLUIDO);
            planoEnsinoRepository.save(planoEnsino);
        }
    }

    /**
     * Percorre os horários semanais da turma em ordem de dia/hora a partir de
     * dataInicio até completar a quantidade pedida. Cada aula já sai com o
     * horário vinculado, então a carga horária é calculada.
     */
    @Transactional
    public List<Aula> gerarLote(Long planoAulaId, GerarAulasLoteRequest pedido) {
        PlanoAula planoAula = planoAulaRepository.findById(planoAulaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Plano de aula " + planoAulaId + " não encontrado."));

        if (pedido.getQuantidade() == null || pedido.getQuantidade() <= 0) {
            throw new RequisicaoInvalidaException("Informe uma quantidade de aulas maior que zero.");
        }

        Long turmaId = planoAula.getTurmaDisciplina() != null && planoAula.getTurmaDisciplina().getTurma() != null
                ? planoAula.getTurmaDisciplina().getTurma().getId()
                : null;
        if (turmaId == null) {
            throw new RequisicaoInvalidaException("Este plano de aula não tem turma vinculada.");
        }

        List<HorarioTurma> horarios = horarioTurmaRepository.findByTurma_Id(turmaId).stream()
                .sorted(Comparator.comparing(HorarioTurma::getDiaSemana).thenComparing(HorarioTurma::getHoraInicio))
                .toList();
        if (horarios.isEmpty()) {
            throw new RequisicaoInvalidaException(
                    "Esta turma ainda não tem horário cadastrado — cadastre em Turmas, aba Horários, antes de gerar aulas.");
        }

        int proximaOrdem = repository.findByPlanoAula_IdOrderByOrdemAsc(planoAulaId).stream()
                .mapToInt(a -> a.getOrdem() != null ? a.getOrdem() : 0)
                .max()
                .orElse(0) + 1;

        String tituloBase = pedido.getTituloBase() != null && !pedido.getTituloBase().isBlank()
                ? pedido.getTituloBase().trim()
                : "Aula";

        // O plano de aula não pode ultrapassar a carga horária do plano de ensino; null = sem limite.
        Integer limiteCargaHoraria = planoAula.getPlanoEnsino() != null
                ? planoAula.getPlanoEnsino().getCargaHoraria()
                : null;
        double cargaHorariaAcumulada = limiteCargaHoraria != null
                ? somarCargaHorariaAtiva(planoAulaId, null)
                : 0;

        List<Aula> geradas = new ArrayList<>();
        LocalDate data = pedido.getDataInicio() != null ? pedido.getDataInicio() : LocalDate.now();
        // Trava contra loop infinito: com 1 horário cadastrado, sai 1 aula por
        // semana, então a quantidade pedida nunca precisa de mais semanas que isso.
        int diasVarridosNoMaximo = pedido.getQuantidade() * 7 * horarios.size();
        int diasVarridos = 0;
        boolean limiteAtingido = false;

        while (geradas.size() < pedido.getQuantidade() && diasVarridos < diasVarridosNoMaximo && !limiteAtingido) {
            DiaSemana diaDaData = DiaSemana.values()[data.getDayOfWeek().ordinal()];

            for (HorarioTurma horario : horarios) {
                if (geradas.size() >= pedido.getQuantidade()) break;
                if (horario.getDiaSemana() != diaDaData) continue;

                double horasDoHorario = Duration.between(horario.getHoraInicio(), horario.getHoraFim()).toMinutes() / 60.0;
                if (limiteCargaHoraria != null
                        && cargaHorariaAcumulada + horasDoHorario > limiteCargaHoraria + TOLERANCIA_CARGA_HORARIA) {
                    limiteAtingido = true;
                    break;
                }

                Aula aula = new Aula();
                aula.setPlanoAula(planoAula);
                aula.setHorarioTurma(horario);
                aula.setDataPrevista(data);
                aula.setOrdem(proximaOrdem++);
                aula.setTitulo(tituloBase + " " + aula.getOrdem());
                aula.setStatus(StatusAtivoInativo.ATIVO);
                validar(aula);
                geradas.add(repository.save(aula));
                cargaHorariaAcumulada += horasDoHorario;
            }

            data = data.plusDays(1);
            diasVarridos++;
        }

        if (limiteAtingido && geradas.isEmpty()) {
            throw new RequisicaoInvalidaException(
                    "A carga horária prevista no plano de ensino (" + limiteCargaHoraria
                            + "h) já está totalmente coberta pelas aulas existentes deste plano.");
        }

        auditLogService.registrar("Aula", planoAulaId, AcaoAuditoria.CRIACAO,
                geradas.size() + " aula(s) geradas em lote para o plano de aula " + planoAulaId + ".");

        return geradas;
    }

    @Transactional
    public void deletar(Long id) {
        Aula obj = buscar(id);
        obj.setStatus(StatusAtivoInativo.INATIVO);
        repository.save(obj);
        auditLogService.registrar("Aula", id, AcaoAuditoria.EXCLUSAO, "Aula marcada como INATIVA (soft-delete).");
    }

    private void validar(Aula obj) {
        if (obj.getPlanoAula() == null || obj.getPlanoAula().getId() == null) {
            throw new RequisicaoInvalidaException("Plano de aula é obrigatório para a aula.");
        }
        PlanoAula planoAula = planoAulaRepository.findById(obj.getPlanoAula().getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Plano de aula " + obj.getPlanoAula().getId() + " não encontrado."));
        obj.setPlanoAula(planoAula);

        // Com horário vinculado, a carga horária é calculada e o valor enviado
        // pelo cliente é ignorado; sem horário (reposição), vale o digitado.
        if (obj.getHorarioTurma() != null && obj.getHorarioTurma().getId() != null) {
            HorarioTurma horario = horarioTurmaRepository.findById(obj.getHorarioTurma().getId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException(
                            "Horário " + obj.getHorarioTurma().getId() + " não encontrado."));

            Long turmaDoHorarioId = horario.getTurma().getId();
            Long turmaDaAulaId = planoAula.getTurmaDisciplina() != null && planoAula.getTurmaDisciplina().getTurma() != null
                    ? planoAula.getTurmaDisciplina().getTurma().getId()
                    : null;
            if (!turmaDoHorarioId.equals(turmaDaAulaId)) {
                throw new RequisicaoInvalidaException("O horário escolhido não pertence à turma desta aula.");
            }

            obj.setHorarioTurma(horario);
            obj.setCargaHoraria(Duration.between(horario.getHoraInicio(), horario.getHoraFim()).toMinutes() / 60.0);
        } else {
            obj.setHorarioTurma(null);
            if (obj.getCargaHoraria() == null || obj.getCargaHoraria() <= 0) {
                throw new RequisicaoInvalidaException("Informe a carga horária da aula, ou selecione um horário da turma.");
            }
        }

        // A soma das aulas não pode ultrapassar a carga horária herdada do plano de ensino.
        Integer limiteCargaHoraria = planoAula.getPlanoEnsino() != null
                ? planoAula.getPlanoEnsino().getCargaHoraria()
                : null;
        if (limiteCargaHoraria != null) {
            double jaUsada = somarCargaHorariaAtiva(planoAula.getId(), obj.getId());
            double total = jaUsada + obj.getCargaHoraria();
            if (total > limiteCargaHoraria + TOLERANCIA_CARGA_HORARIA) {
                double restante = Math.max(0, limiteCargaHoraria - jaUsada);
                throw new RequisicaoInvalidaException(
                        "Esta aula excede a carga horária prevista no plano de ensino (" + limiteCargaHoraria
                                + "h). Restam " + restante + "h disponíveis para este plano.");
            }
        }
    }

    /** Soma a carga horária das aulas ATIVAS do plano, opcionalmente excluindo uma (edição de aula existente). */
    private double somarCargaHorariaAtiva(Long planoAulaId, Long excluirAulaId) {
        return repository.findByPlanoAula_IdAndStatus(planoAulaId, StatusAtivoInativo.ATIVO).stream()
                .filter(aula -> excluirAulaId == null || !aula.getId().equals(excluirAulaId))
                .mapToDouble(Aula::getCargaHoraria)
                .sum();
    }
}
