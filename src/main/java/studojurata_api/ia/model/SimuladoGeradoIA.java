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
 * Vínculo entre um Simulado gerado automaticamente por
 * GeracaoSimuladoIAService e o aluno/conteúdo/motivo (RecomendacaoService)
 * que originou a geração.
 *
 * Não fica como campo direto em Simulado porque essa informação só existe
 * pra simulados de reforço automático — um simulado criado manualmente pelo
 * professor (a maioria) nunca tem esse contexto, e Simulado é reaproveitado
 * por todo o módulo (não só o de IA). Registro criado só quando a geração
 * passa por GeracaoSimuladoIAService — simulados anteriores a esta entidade
 * simplesmente não têm vínculo, e quem consome trata isso como "sem dado".
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
     * Prazo pra revisar/aprovar e lançar este simulado — a data em que a
     * repetição espaçada ficou devida (RevisaoConteudo.dataProximoReforco),
     * quando existe; senão, a própria data de geração (baixo aproveitamento
     * não tem agenda própria, é um limiar já atingido agora). Confirmado
     * pelo usuário: passado esse prazo sem o simulado ter sido lançado
     * (Simulado.status ainda RASCUNHO), ele conta como atrasado.
     */
    private LocalDate prazoLancamento;
}
