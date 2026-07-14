package studojurata_api.model;

import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.model.enums.StatusTurma;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class Turma extends BaseEntity {

    private String titulo;
    private Integer capacidadeMaxima;

    /*
     * quantidadeAlunos foi removido: era um campo persistido e redundante,
     * sujeito a desincronização em relação às matrículas ativas reais.
     * A contagem de alunos ativos agora é sempre derivada via
     * AlunoTurmaRepository.countByTurmaIdAndStatus(turmaId, ATIVA)
     * (ver TurmaService.contarAlunosAtivos / AlunoTurmaService), nunca
     * armazenada como fonte de verdade.
     */

    @Enumerated(EnumType.STRING)
    private StatusTurma status;

    private LocalDate dataInicio;
    private LocalDate dataFim;
}