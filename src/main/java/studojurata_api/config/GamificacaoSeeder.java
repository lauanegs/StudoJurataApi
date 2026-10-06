package studojurata_api.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import studojurata_api.model.gamificacao.Skin;
import studojurata_api.repository.gamificacao.SkinRepository;

@Component
@RequiredArgsConstructor
public class GamificacaoSeeder implements CommandLineRunner {
    private final SkinRepository skinRepository;

    @Override
    public void run(String... args) {
        criarSeNaoExistir("Jura", "Jura, o visual padrão do mascote Studo Jurata.", 0, "skins/classico.png");
        criarSeNaoExistir("Bit", "Traje de explorador, desbloqueado com moedas de reforço.", 50, "skins/explorador.png");
        criarSeNaoExistir("Lia", "Jaleco de cientista, para quem completa muitos simulados.", 80, "skins/cientista.png");
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
