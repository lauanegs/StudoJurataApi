package studojurata_api.ia.model;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.ia.model.enums.MotivoRecomendacao;
import studojurata_api.model.Aluno;
import studojurata_api.model.BaseEntity;
import studojurata_api.model.ConteudoPlano;
import studojurata_api.model.Simulado;

/**
 * Aluno, conteúdo e motivo que originaram um simulado gerado pela IA. Fica
 * fora de Simulado porque simulados criados pelo professor nunca têm esse
 * contexto.
 */
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

    /**
     * Data em que a repetição espaçada venceu ou, para baixo aproveitamento, a
     * data de geração. Ainda em RASCUNHO depois dela, o simulado está atrasado.
     */
    private LocalDate prazoLancamento;
}
