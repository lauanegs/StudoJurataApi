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
import studojurata_api.model.enums.AcaoAuditoria;
import studojurata_api.model.enums.DiaSemana;
import studojurata_api.model.enums.StatusAtivoInativo;
import studojurata_api.repository.AulaRepository;
import studojurata_api.repository.HorarioTurmaRepository;
import studojurata_api.repository.PlanoAulaRepository;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Correção 2.9: Aula é uma das 3 entidades priorizadas para AuditLog (junto com Nota e SimuladoAluno). */
@Service
@RequiredArgsConstructor
public class AulaService {

    private final AulaRepository repository;
    private final PlanoAulaRepository planoAulaRepository;
    private final HorarioTurmaRepository horarioTurmaRepository;
    private final AuditLogService auditLogService;

    public List<Aula> listar() { return repository.findAll(); }

    public Aula buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aula " + id + " não encontrada."));
    }

    /** Aulas de um plano de aula, na ordem em que devem ser ministradas. */
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

    /**
     * Marca a aula como efetivamente ministrada, preenchendo a data de
     * publicação (correção 2.5/interface: "data que realmente foi passada
     * a aula"). É a partir desse campo que as estatísticas de "aulas
     * realizadas" e "carga horária realizada" do plano de aula são
     * calculadas.
     */
    @Transactional
    public Aula publicar(Long id, LocalDate dataPublicacao) {
        Aula aula = buscar(id);
        aula.setDataPublicacao(dataPublicacao != null ? dataPublicacao : LocalDate.now());
        return repository.save(aula);
    }

    /**
     * Geração em lote (pedido explícito: "no início do curso ele já faz a
     * geração ali e vai manipulando depois", em vez de cadastrar uma aula
     * de cada vez) — segue os horários semanais já cadastrados na turma
     * (HorarioTurma), ciclando entre eles em ordem de dia/hora a partir de
     * dataInicio até completar a quantidade pedida. Cada aula gerada já sai
     * com o horário vinculado (mesma regra de validar(): carga horária
     * calculada, não digitada) — o professor edita/exclui as que precisar
     * depois em AulaFormulario, individualmente.
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

        List<Aula> geradas = new ArrayList<>();
        LocalDate data = pedido.getDataInicio() != null ? pedido.getDataInicio() : LocalDate.now();
        // Limite defensivo: no pior caso (1 horário cadastrado), 1 aula por
        // semana — nunca deveria demorar mais que isso pra completar a
        // quantidade pedida, então serve só de trava contra loop infinito.
        int diasVarridosNoMaximo = pedido.getQuantidade() * 7 * horarios.size();
        int diasVarridos = 0;

        while (geradas.size() < pedido.getQuantidade() && diasVarridos < diasVarridosNoMaximo) {
            DiaSemana diaDaData = DiaSemana.values()[data.getDayOfWeek().ordinal()];

            for (HorarioTurma horario : horarios) {
                if (geradas.size() >= pedido.getQuantidade()) break;
                if (horario.getDiaSemana() != diaDaData) continue;

                Aula aula = new Aula();
                aula.setPlanoAula(planoAula);
                aula.setHorarioTurma(horario);
                aula.setDataPrevista(data);
                aula.setOrdem(proximaOrdem++);
                aula.setTitulo(tituloBase + " " + aula.getOrdem());
                aula.setStatus(StatusAtivoInativo.ATIVO);
                validar(aula);
                geradas.add(repository.save(aula));
            }

            data = data.plusDays(1);
            diasVarridos++;
        }

        auditLogService.registrar("Aula", planoAulaId, AcaoAuditoria.CRIACAO,
                geradas.size() + " aula(s) geradas em lote para o plano de aula " + planoAulaId + ".");

        return geradas;
    }

    /**
     * Soft delete (correção 4.3): mantém o registro (e a frequência /
     * conteúdos já vinculados a ele) para preservar o histórico
     * pedagógico, apenas marcando a aula como INATIVA.
     */
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

        // Horário vinculado (opcional): quando informado, a carga horária da
        // aula passa a ser CALCULADA a partir dele (hora fim - hora início),
        // nunca do valor que o cliente mandou — evita erro de digitação e faz
        // o cadastro de horários da turma (antes um cadastro morto, sem
        // nenhum consumidor) finalmente servir pra algo. Sem horário
        // vinculado (reposição, aula fora do horário fixo), a carga horária
        // continua sendo digitada à mão.
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
    }
}
