package studojurata_api.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.model.enums.StatusAtivoInativo;

/**
 * Correção 9.1 da Segunda Análise Crítica ("Escola/tenant — decisão
 * confirmada"): mesmo havendo hoje uma única escola (Hero Geek), introduzir
 * o tenant desde já evita uma migração ordens de magnitude mais cara depois.
 * Referenciada por Turma, Usuario e Disciplina (ver esses modelos).
 */
@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class Escola extends BaseEntity {

    @Column(nullable = false)
    private String nome;

    @Column(unique = true, length = 18)
    private String cnpj;

    @Enumerated(EnumType.STRING)
    private StatusAtivoInativo status;
}
