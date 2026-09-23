# Corpo Forte Tracker

Rede social de calistenia: gerar e acompanhar o próprio treino e
compartilhar a rotina com a comunidade. Construída em fases pequenas — as
já concluídas estão em "Fases" abaixo.

## Stack

Java 21 · Spring Boot 3.5 · Spring Data JPA · Spring Security (login OAuth2 +
JWT) · Bean Validation · PostgreSQL · Flyway · Thymeleaf · springdoc-openapi ·
Maven.

## Como rodar

1. Subir o banco:
   ```
   docker compose up -d
   ```
2. Rodar a aplicação:
   ```
   mvn spring-boot:run
   ```
3. Abrir http://localhost:8090 — pede login com Google antes de mostrar
   qualquer página (ver "Login com Google" abaixo pra configurar
   credenciais reais). Depois de logado: `/perfil`, `/avaliacao` (avaliação
   física), `/peso` (registro de peso), `/equipamentos`, `/exercicios`
   (catálogo), `/treino-do-dia` e `/feed` (comunidade).

## Login com Google

A partir da Fase 6, todo acesso exige login. Sem credenciais configuradas a
aplicação ainda sobe normalmente, mas o login nunca vai completar de
verdade — pra testar o fluxo real:

1. No [Google Cloud Console](https://console.cloud.google.com/), crie (ou
   reuse) um projeto → **APIs e serviços → Credenciais → Criar
   credenciais → ID do cliente OAuth**.
2. Tipo de aplicativo: **Aplicativo da Web**.
3. URI de redirecionamento autorizado:
   `http://localhost:8090/login/oauth2/code/google`.
4. Copie o Client ID e o Client Secret gerados e exporte como variável de
   ambiente antes de rodar a aplicação, junto com o seu próprio e-mail do
   Google (nunca commitar isso — não tem nenhum arquivo desses no
   repositório, e `.env`/`application-local.yml` já estão no `.gitignore`
   se preferir usar um deles):
   ```
   export GOOGLE_CLIENT_ID=...
   export GOOGLE_CLIENT_SECRET=...
   export APP_OWNER_EMAIL=seu-email@gmail.com
   mvn spring-boot:run
   ```

O primeiro login **do e-mail configurado em `APP_OWNER_EMAIL`** reivindica
a conta local que já existe no banco (se sobrar exatamente uma sem Google
vinculado) em vez de criar uma conta do zero — preserva perfil/histórico já
cadastrados antes do login existir. Qualquer outro e-mail sempre cria uma
conta nova, mesmo que a conta local ainda esteja sem dono — sem
`APP_OWNER_EMAIL` configurado, a reivindicação fica desativada e ninguém
herda a conta local.

## API REST

A partir da Fase 10 o back-end expõe uma API JSON em `/api/v1`, autenticada
por token (sem sessão e sem cookie). O login com Google acontece no cliente,
com o Google Sign-In; o cliente troca o ID token recebido do Google por
tokens do Corpo Forte:

| Rota | O que faz |
|---|---|
| `POST /api/v1/auth/google` | `{"idToken": "..."}` → access token (JWT, 15 min) + refresh token (30 dias) |
| `POST /api/v1/auth/refresh` | `{"refreshToken": "..."}` → par novo; o refresh usado deixa de valer |
| `POST /api/v1/auth/logout` | `{"refreshToken": "..."}` → encerra aquela sessão |
| `GET /api/v1/me` | conta dona do access token (`Authorization: Bearer ...`) |

O refresh token é rotativo: reapresentar um que já foi trocado é tratado
como cópia roubada e encerra todas as sessões da conta. Todo erro da API
sai no formato [ProblemDetail (RFC 7807)](https://www.rfc-editor.org/rfc/rfc7807),
com a lista de campos inválidos quando é erro de validação.

Mesma conta do login web: o mesmo Google cai na mesma conta pelos dois
caminhos, inclusive a reivindicação por `APP_OWNER_EMAIL`.

Variáveis de ambiente (todas opcionais pra rodar local, nunca commitadas):

| Variável | Para quê | Sem ela |
|---|---|---|
| `APP_JWT_SECRET` | segredo do access token, com 32+ bytes | segredo aleatório a cada boot (tokens morrem ao reiniciar) |
| `APP_GOOGLE_CLIENT_IDS` | client IDs OAuth do Google aceitos no login pela API, separados por vírgula | login pela API desligado |
| `APP_CORS_ORIGINS` | origens web que podem chamar a API pelo navegador, separadas por vírgula | nenhuma origem externa |

### Testar a API localmente (perfil `dev`)

```
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

1. Logar pela tela web normalmente (http://localhost:8090).
2. Abrir http://localhost:8090/dev/token-api: devolve um par de tokens pra
   conta logada, sem precisar de um cliente com Google Sign-In.
3. Abrir o Swagger UI em http://localhost:8090/swagger-ui.html, clicar em
   **Authorize** e colar o `accessToken`.

A especificação OpenAPI fica em http://localhost:8090/v3/api-docs. O Swagger
UI, a especificação e o `/dev/token-api` só existem no perfil `dev`.

## Testes

Unitários (rápidos, sem dependências — regra de negócio isolada):
```
mvn test
```

Unitários + integração (sobe um PostgreSQL real via
[Testcontainers](https://testcontainers.com/) pra testar acoplamento entre
módulos e constraints do banco — precisa do Docker rodando localmente):
```
mvn verify
```

Convenção: `*Test.java` roda no `mvn test` (Surefire); `*IT.java` roda só no
`mvn verify` (Failsafe). Ao escrever um teste novo, o sufixo do nome do
arquivo decide em qual dos dois ele entra.

## Fases

1. **Perfil** — cadastro do usuário + cálculo de TMB/TDEE/IMC/macros. ✅
2. **Avaliação física** — teste de repetições máximas em 6 padrões de
   movimento + cálculo do volume de treino inicial. ✅
3. **Registro de peso** — histórico diário de peso + tendência semanal
   (média da semana atual vs. semana anterior). ✅
4. **Equipamentos + catálogo de exercícios** — usuário marca o que tem
   disponível; catálogo de 54 exercícios (6 padrões de movimento × 3
   níveis), filtrável por nível/padrão/equipamento e pelo que o usuário
   consegue fazer com o que marcou. ✅
5. **Treino do dia** — full body gerado automaticamente (1 exercício por
   padrão de movimento, sorteado uma vez por dia dentre o que é compatível
   com nível/equipamento), série×repetição a partir do volume inicial da
   avaliação física, com checklist de conclusão. ✅
6. **Login com Google** — multiusuário real, cada conta só vê o próprio
   dado; usuário local existente vira o primeiro usuário autenticado no
   primeiro login. ✅
7. **Comunidade/blog** (posts, comentários, curtidas):
   - 7a. **Feed social** — post de texto livre + feed global (todo mundo vê
     post de todo mundo, de propósito — é a primeira entidade
     intencionalmente compartilhada do sistema, diferente de tudo o resto,
     que é isolado por usuário desde a Fase 6). ✅
   - 7b. **Comentários + curtidas** — comentário de texto e curtida
     (toggle) em qualquer post do feed, inclusive o dos outros: são os
     primeiros endpoints do sistema em que usar o ID de um recurso alheio
     é a função, não uma falha de isolamento. ✅
   - 7c. **Apagar post e comentário** — o autor apaga o próprio post
     (comentários e curtidas saem junto, pelo banco); comentário pode ser
     apagado por quem escreveu ou pelo dono do post. Sem edição. ✅
8. **Periodização** — o volume do treino sobe a cada semana em que houve
   treino concluído (usa o incremento que a Fase 2 já calculava e ninguém
   consumia), em ciclos de 8 semanas com aviso pra refazer a avaliação
   física ao fim. ✅
9. **Histórico de avaliação física** — refazer a avaliação deixa de apagar
   a anterior: uma medição por dia por usuário, com comparação movimento a
   movimento entre a avaliação atual e a anterior ("10 → 15, +5") e
   histórico completo na tela `/avaliacao`. ✅
10. **API REST e autenticação por token** — API JSON em `/api/v1` com login
    Google feito no cliente e trocado por tokens próprios (access token de
    15 minutos + refresh token rotativo e revogável), erros em ProblemDetail,
    especificação OpenAPI e Swagger UI no perfil `dev`. O vínculo de login
    virou uma tabela própria (provedor + identificador), migrando as contas
    já vinculadas ao Google sem perder nenhuma. ✅
