# Deploy (Vercel + Render + Neon)

Arquitetura: o navegador fala só com a Vercel. A Vercel serve o front e
reescreve `/api/*` para o backend no Render (`vercel.json` do front), então o
cookie de sessão é do mesmo domínio e não há CORS nem `SameSite=None`.

## 1. Banco (Neon)
1. Criar um projeto Postgres gratuito e copiar a *connection string*.
2. Montar a URL JDBC: `jdbc:postgresql://<host>/<banco>?sslmode=require`
   (usuário e senha vão em variáveis separadas).

## 2. Backend (Render)
1. New → Web Service → repositório `StudoJurataApi`, runtime **Docker**
   (usa o `Dockerfile` da raiz; já ativa o perfil `prod`).
2. Variáveis de ambiente:

| Variável | Valor |
|---|---|
| `DB_URL` | URL JDBC do Neon |
| `DB_USERNAME` / `DB_PASSWORD` | credenciais do Neon |
| `GEMINI_API_KEY` | chave **nova** (a antiga foi exposta; revogar) — opcional, sem ela a IA usa o banco de questões |
| `ADMIN_USERNAME` / `ADMIN_PASSWORD` | primeiro administrador (criado só se não existir nenhum) |
| `CORS_ALLOWED_ORIGINS` | opcional; vazio quando a API é chamada pelo proxy da Vercel |

3. Depois do primeiro acesso, trocar a senha do admin e completar o cadastro
   (CPF provisório `000.000.000-00`).
4. Plano gratuito do Render "dorme" após inatividade: a primeira requisição
   demora ~1 min, e os jobs agendados (publicação/encerramento de simulados,
   geração por IA) só rodam enquanto o serviço está acordado.

## 3. Front (Vercel)
1. Importar o repositório `StudoJurataFront` (preset Vite; build `npm run build`,
   saída `dist`).
2. Em `vercel.json`, trocar `SEU-BACKEND.onrender.com` pela URL do serviço no
   Render e fazer commit.
3. Não definir `VITE_API_BASE_URL` (o padrão `/api` é o que o proxy usa).

## Pontos de atenção conhecidos
- `spring.jpa.hibernate.ddl-auto=update`: o schema é criado/alterado pela
  aplicação, sem migrations versionadas (Flyway ainda não adotado). Faça backup
  antes de mudar entidades.
- O perfil `seed` **apaga todos os dados**; nunca ativá-lo em produção.
- Sessão em memória: reiniciar o serviço desloga todos os usuários.
- Swagger e detalhes de erro ficam desligados no perfil `prod`.
