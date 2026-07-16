package studojurata_api.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import studojurata_api.model.gamificacao.Skin;
import studojurata_api.repository.gamificacao.SkinRepository;

/**
 * Correção 8.2 da Segunda Análise Crítica: "no lançamento, apenas 3 skins
 * disponíveis" — popula o catálogo inicial na primeira subida da aplicação,
 * sem sobrescrever se já existirem (idempotente).
 */
@Component
@RequiredArgsConstructor
public class GamificacaoSeeder implements CommandLineRunner {

    private final SkinRepository skinRepository;

    @Override
    public void run(String... args) {
        criarSeNaoExistir("Mascote Clássico", "Visual padrão do mascote Studo Jurata.", 0, "skins/classico.png");
        criarSeNaoExistir("Mascote Explorador", "Traje de explorador, desbloqueado com moedas de reforço.", 50, "skins/explorador.png");
        criarSeNaoExistir("Mascote Cientista", "Jaleco de cientista, para quem completa muitos simulados.", 80, "skins/cientista.png");
    }

    private void criarSeNaoExistir(String nome, String descricao, int custoMoedas, String urlAsset) {
        if (skinRepository.existsByNome(nome)) return;
        Skin skin = new Skin();
        skin.setNome(nome);
        skin.setDescricao(descricao);
        skin.setCustoMoedas(custoMoedas);
        skin.setUrlAsset(urlAsset);
        skin.setDisponivel(true);
        skinRepository.save(skin);
    }
}
