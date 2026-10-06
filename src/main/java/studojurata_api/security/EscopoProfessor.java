package studojurata_api.security;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import studojurata_api.model.turma.AlunoTurma;
import studojurata_api.model.turma.TurmaDisciplina;
import studojurata_api.model.enums.StatusMatricula;
import studojurata_api.repository.turma.AlunoTurmaRepository;
import studojurata_api.repository.turma.TurmaDisciplinaRepository;

/** Resolve o escopo de leitura de um professor: quais ids ele pode enxergar  */
@Component
@RequiredArgsConstructor
public class EscopoProfessor {
    private final TurmaDisciplinaRepository turmaDisciplinaRepository;
    private final AlunoTurmaRepository alunoTurmaRepository;

    /** Ids das turmas em que o professor é titular */
    public Set<Long> turmaIdsDoProfessor(Long professorId) {
        return turmaIdsDosVinculos(vinculosDoProfessor(professorId));
    }

    /** Par turma + disciplina em que o professor é titular */
    public record ParTurmaDisciplina(Long turmaId, Long disciplinaId) {}

    /** Pares turma+disciplina do professor */
    public Set<ParTurmaDisciplina> paresTurmaDisciplinaDoProfessor(Long professorId) {
        return vinculosDoProfessor(professorId).stream()
                .filter(vinculo -> vinculo.getTurma() != null && vinculo.getDisciplina() != null)
                .map(vinculo -> new ParTurmaDisciplina(vinculo.getTurma().getId(), vinculo.getDisciplina().getId()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /** Ids dos vínculos turma+disciplina do professor */
    public Set<Long> turmaDisciplinaIdsDoProfessor(Long professorId) {
        Set<Long> vinculoIds = new LinkedHashSet<>();
        for (TurmaDisciplina vinculo : vinculosDoProfessor(professorId)) {
            if (vinculo.getId() != null) {
                vinculoIds.add(vinculo.getId());
            }
        }
        return vinculoIds;
    }

    /** O professor leciona para este aluno? */
    public boolean lecionaPara(Long professorId, Long alunoId) {
        if (professorId == null || alunoId == null) {
            return false;
        }

        Set<Long> turmaIds = turmaIdsDoProfessor(professorId);
        if (turmaIds.isEmpty()) {
            return false;
        }

        return alunoTurmaRepository.existsByAluno_IdAndTurma_IdIn(alunoId, turmaIds);
    }

    /** Ids das disciplinas que o professor leciona, sem duplicidade */
    public Set<Long> disciplinaIdsDoProfessor(Long professorId) {
        return vinculosDoProfessor(professorId).stream()
                .map(TurmaDisciplina::getDisciplina)
                .filter(Objects::nonNull)
                .map(studojurata_api.model.curriculo.Disciplina::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /** Ids dos alunos com matrícula ATIVA em alguma turma do professor */
    public Set<Long> alunoIdsDoProfessor(Long professorId) {
        Set<Long> alunoIds = new LinkedHashSet<>();

        for (Long turmaId : turmaIdsDosVinculos(vinculosDoProfessor(professorId))) {
            for (AlunoTurma matricula : alunoTurmaRepository.findByTurmaIdAndStatus(turmaId, StatusMatricula.ATIVA)) {
                if (matricula.getAluno() != null && matricula.getAluno().getId() != null) {
                    alunoIds.add(matricula.getAluno().getId());
                }
            }
        }

        return alunoIds;
    }

    /** Vínculos em que o professor é titular; lista vazia quando não há id */
    private List<TurmaDisciplina> vinculosDoProfessor(Long professorId) {
        if (professorId == null) {
            return List.of();
        }
        List<TurmaDisciplina> vinculos = turmaDisciplinaRepository.findByProfessorId(professorId);
        return vinculos != null ? vinculos : List.of();
    }

    /** Turmas distintas dos vínculos, ignorando vínculo sem turma ou turma sem id */
    private Set<Long> turmaIdsDosVinculos(List<TurmaDisciplina> vinculos) {
        Set<Long> turmaIds = new LinkedHashSet<>();
        for (TurmaDisciplina vinculo : vinculos) {
            if (vinculo.getTurma() != null && vinculo.getTurma().getId() != null) {
                turmaIds.add(vinculo.getTurma().getId());
            }
        }
        return turmaIds;
    }
}
