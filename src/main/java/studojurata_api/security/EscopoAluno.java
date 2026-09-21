package studojurata_api.security;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import studojurata_api.model.AlunoTurma;
import studojurata_api.model.Turma;
import studojurata_api.model.TurmaDisciplina;
import studojurata_api.repository.AlunoTurmaRepository;
import studojurata_api.repository.TurmaDisciplinaRepository;

/**
 * Escopo de leitura de um aluno: as turmas em que está matriculado (qualquer
 * status de matrícula, para preservar o histórico) e os vínculos
 * turma+disciplina dessas turmas.
 *
 * <p>Custo fixo: 1 consulta para as turmas e, quando houver alguma, mais 1 para
 * os vínculos — nunca uma consulta por turma.
 */
@Component
@RequiredArgsConstructor
public class EscopoAluno {

    private final AlunoTurmaRepository alunoTurmaRepository;
    private final TurmaDisciplinaRepository turmaDisciplinaRepository;

    /** Ids das turmas do aluno. */
    public Set<Long> turmaIdsDoAluno(Long alunoId) {
        if (alunoId == null) {
            return Set.of();
        }
        return alunoTurmaRepository.findByAluno_Id(alunoId).stream()
                .map(AlunoTurma::getTurma)
                .filter(Objects::nonNull)
                .map(Turma::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /** Ids dos vínculos turma+disciplina das turmas do aluno. */
    public Set<Long> turmaDisciplinaIdsDoAluno(Long alunoId) {
        Set<Long> turmaIds = turmaIdsDoAluno(alunoId);
        if (turmaIds.isEmpty()) {
            return Set.of();
        }
        return turmaDisciplinaRepository.findByTurma_IdIn(turmaIds).stream()
                .map(TurmaDisciplina::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
