package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.model.Curso;
import studojurata_api.model.Turma;
import studojurata_api.model.enums.StatusAtivoInativo;
import studojurata_api.model.enums.StatusMatricula;
import studojurata_api.model.enums.StatusTurma;
import studojurata_api.repository.AlunoTurmaRepository;
import studojurata_api.repository.CursoRepository;
import studojurata_api.repository.TurmaRepository;
import studojurata_api.security.EscolaContext;

import java.util.List;

/**
 * Correção 2.2 da Terceira Análise Crítica (isolamento multi-tenant):
 * listar() passa a filtrar pela escola do usuário autenticado (via
 * EscolaContext), em vez de devolver as turmas de todas as escolas para
 * qualquer usuário. Correção 2.3: deletar() passa a ser soft-delete
 * (Turma.status = INATIVA) — o campo já existia mas nunca era usado, e uma
 * Turma pode ter AlunoTurma/TurmaDisciplina/histórico de simulados
 * vinculados, o mesmo risco que motivou soft-delete nas demais entidades.
 * Correção 2.4: curso passa a ser obrigatório (ver Turma.curso), e agora é
 * a entidade Curso — validarCurso resolve o Curso completo a partir do id
 * enviado, garantindo que ele existe (404 amigável em vez de a constraint
 * do banco estourar como erro 500). O horário semanal da turma passou a
 * ser tratado à parte, em HorarioTurma/HorarioTurmaService (1 Turma : N
 * HorarioTurma, pois uma turma tem aula em mais de um dia da semana).
 *
 * Correção 3.1 da Quarta Análise Crítica (isolamento por escola na
 * escrita): validarCurso agora também recusa (403) um Curso que não
 * pertence à escola do usuário autenticado — antes, qualquer Curso
 * existente podia ser vinculado a qualquer Turma só pelo id, mesmo sendo
 * de outra escola. Correção 3.3: recusa (409) vincular uma turma a um
 * Curso já INATIVO (soft-deletado/descontinuado).
 */
@Service
@RequiredArgsConstructor
public class TurmaService {

    private final TurmaRepository repository;
    private final AlunoTurmaRepository alunoTurmaRepository;
    private final CursoRepository cursoRepository;
    private final EscolaContext escolaContext;

    /** Filtra pela escola do usuário autenticado; se não houver escola resolvível, devolve tudo (bootstrapping). */
    public List<Turma> listar() {
        Long escolaId = escolaContext.escolaAtualId();
        return escolaId != null ? repository.findByEscola_Id(escolaId) : repository.findAll();
    }

    public Turma buscar(Long id) { return repository.findById(id).orElseThrow(); }

    public Turma salvar(Turma obj) {
        validarCapacidadeMaxima(obj);
        validarCurso(obj);
        if (obj.getStatus() == null) obj.setStatus(StatusTurma.ATIVA);
        return repository.save(obj);
    }

    public Turma atualizar(Long id, Turma obj) {
        validarCapacidadeMaxima(obj);
        validarCurso(obj);
        obj.setId(id);

        // Se a capacidade máxima está sendo reduzida abaixo da quantidade de
        // alunos já ativos, alertamos em vez de permitir uma turma "estourada"
        // silenciosamente.
        if (obj.getCapacidadeMaxima() != null) {
            long ativos = alunoTurmaRepository.countByTurmaIdAndStatus(id, StatusMatricula.ATIVA);
            if (ativos > obj.getCapacidadeMaxima()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Não é possível reduzir a capacidade máxima para " + obj.getCapacidadeMaxima()
                                + ": a turma já possui " + ativos + " aluno(s) com matrícula ativa.");
            }
        }

        return repository.save(obj);
    }

    /** Quantidade de alunos com matrícula ativa na turma, sempre derivada das matrículas (nunca persistida em Turma). */
    public long contarAlunosAtivos(Long turmaId) {
        return alunoTurmaRepository.countByTurmaIdAndStatus(turmaId, StatusMatricula.ATIVA);
    }

    /** Soft-delete (correção 2.3): preserva o histórico de matrículas/disciplinas vinculadas à turma. */
    public void deletar(Long id) {
        Turma turma = buscar(id);
        turma.setStatus(StatusTurma.INATIVA);
        repository.save(turma);
    }

    private void validarCapacidadeMaxima(Turma obj) {
        if (obj.getCapacidadeMaxima() != null && obj.getCapacidadeMaxima() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Capacidade máxima deve ser maior que zero.");
        }
    }

    private void validarCurso(Turma obj) {
        if (obj.getCurso() == null || obj.getCurso().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Curso é obrigatório: o aluno matriculado na turma sempre segue o curso vinculado a ela.");
        }
        Curso curso = cursoRepository.findById(obj.getCurso().getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Curso " + obj.getCurso().getId() + " não encontrado."));

        if (curso.getStatus() == StatusAtivoInativo.INATIVO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O curso \"" + curso.getNome() + "\" está inativo e não pode receber novas turmas.");
        }

        // Correção 3.1 da Quarta Análise Crítica: o Curso precisa pertencer à
        // mesma escola do usuário autenticado — sem isso, qualquer Curso
        // cadastrado no sistema (de qualquer escola) podia ser vinculado a
        // esta Turma só sabendo o id.
        Long escolaId = escolaContext.escolaAtualId();
        if (escolaId != null && curso.getEscola() != null && !escolaId.equals(curso.getEscola().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Este curso pertence a outra escola e não pode ser vinculado a esta turma.");
        }

        obj.setCurso(curso);
    }
}