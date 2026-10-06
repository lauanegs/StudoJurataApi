package studojurata_api.ia.model;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.ia.model.enums.MotivoRecomendacao;
import studojurata_api.model.pessoa.Aluno;
import studojurata_api.model.BaseEntity;
import studojurata_api.model.planejamento.ConteudoPlano;
import studojurata_api.model.simulado.Simulado;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class SimuladoGeradoIA extends BaseEntity {
    @OneToOne(optional = false)
    private Simulado simulado;

    @ManyToOne(optional = false)
    private Aluno aluno;

    @ManyToOne(optional = false)
    private ConteudoPlano conteudoPlano;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    private Set<MotivoRecomendacao> motivos = new LinkedHashSet<>();

    private LocalDate prazoLancamento;
}
