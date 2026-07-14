package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import studojurata_api.dto.ChamadaRequest;
import studojurata_api.model.Aluno;
import studojurata_api.model.Aula;
import studojurata_api.model.Frequencia;
import studojurata_api.model.enums.StatusMatricula;
import studojurata_api.repository.AlunoRepository;
import studojurata_api.repository.AlunoTurmaRepository;
import studojurata_api.repository.AulaRepository;
import studojurata_api.repository.FrequenciaRepository;

import java.util.List;

/**
 * Implementa a aba "Realizar chamada" da tela de registro de aula
 * (correção 1.1 da Análise Crítica: antes não existia nenhuma entidade
 * para persistir a presença marcada pelo professor).
 */
@Service
@RequiredArgsConstructor
public class FrequenciaService {

    private final FrequenciaRepository repository;
    private final AulaRepository aulaRepository;
    private final AlunoRepository alunoRepository;
    private final AlunoTurmaRepository alunoTurmaRepository;

    public List<Frequencia> listarPorAula(Long aulaId) { return repository.findByAula_Id(aulaId); }

    public List<Frequencia> listarPorAluno(Long alunoId) { return repository.findByAluno_IdOrderByAula_DataPrevistaDesc(alunoId); }

    /**
     * Registra (ou atualiza) a chamada de uma aula de uma só vez, para
     * todos os alunos informados. Só é permitido lançar frequência para
     * alunos com matrícula ATIVA na turma da aula.
     */
    @Transactional
    public List<Frequencia> registrarChamada(Long aulaId, ChamadaRequest request) {
        if (request == null || request.getAlunos() == null || request.getAlunos().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe ao menos um aluno para realizar a chamada.");
        }

        Aula aula = aulaRepository.findById(aulaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aula não encontrada."));
        Long turmaId = aula.getPlanoAula().getTurmaDisciplina().getTurma().getId();

        return request.getAlunos().stream()
                .map(item -> registrarPresenca(aula, turmaId, item))
                .toList();
    }

    private Frequencia registrarPresenca(Aula aula, Long turmaId, ChamadaRequest.Item item) {
        if (item.getAlunoId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "alunoId é obrigatório para cada item da chamada.");
        }
        if (item.getPresente() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "presente é obrigatório para cada item da chamada.");
        }

        Aluno aluno = alunoRepository.findById(item.getAlunoId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aluno não encontrado: " + item.getAlunoId()));

        boolean matriculaAtiva = alunoTurmaRepository.existsByAluno_IdAndTurma_IdAndStatus(
                aluno.getId(), turmaId, StatusMatricula.ATIVA);
        if (!matriculaAtiva) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Aluno " + aluno.getId() + " não possui matrícula ativa na turma desta aula.");
        }

        Frequencia frequencia = repository.findByAluno_IdAndAula_Id(aluno.getId(), aula.getId())
                .orElseGet(Frequencia::new);
        frequencia.setAluno(aluno);
        frequencia.setAula(aula);
        frequencia.setPresente(item.getPresente());
        frequencia.setJustificativa(item.getJustificativa());
        return repository.save(frequencia);
    }

    @Transactional
    public void deletar(Long id) { repository.deleteById(id); }
}
