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

Tudo que as telas fazem também existe na API (Fase 11), sempre sobre a
conta do access token:

| Módulo | Rotas |
|---|---|
| Perfil | `GET`/`PUT /api/v1/perfil` (com TMB, TDEE, IMC e macros) |
| Avaliação física | `GET`/`POST /api/v1/avaliacoes`, `GET /api/v1/avaliacoes/comparacao` |
| Peso | `GET`/`POST /api/v1/pesos`, `GET /api/v1/pesos/tendencia` |
| Equipamentos e catálogo | `GET`/`PUT /api/v1/equipamentos`, `GET /api/v1/exercicios` |
| Treino do dia | `GET /api/v1/treino-do-dia`, `PUT`/`DELETE /api/v1/treino-do-dia/itens/{id}/conclusao` |
| Feed | `GET /api/v1/feed/seguindo`, `GET /api/v1/feed/descobrir`, `GET`/`DELETE /api/v1/posts/{id}`, `POST /api/v1/posts` (JSON, ou multipart com até 4 fotos), `GET`/`POST /api/v1/posts/{id}/comentarios`, `DELETE /api/v1/comentarios/{id}`, `PUT`/`DELETE /api/v1/posts/{id}/curtida` |
| Onboarding | `POST /api/v1/onboarding`, `GET /api/v1/usernames/{username}/disponivel` |
| Perfil público | `GET /api/v1/usuarios/{username}`, `GET /api/v1/usuarios/{username}/posts`, `PUT /api/v1/perfil/publico` (username e bio), `PUT`/`DELETE /api/v1/perfil/foto` (multipart), `GET /api/v1/usuarios?busca=` |
| Seguir | `PUT`/`DELETE /api/v1/usuarios/{username}/seguimento`, `GET /api/v1/usuarios/{username}/seguidores`, `GET /api/v1/usuarios/{username}/seguindo` |
| Privacidade e bloqueio | `GET`/`PUT /api/v1/privacidade` (visibilidade padrão dos posts), `PUT`/`DELETE /api/v1/usuarios/{username}/bloqueio`, `GET /api/v1/bloqueios` |
| Diário de treino | `POST /api/v1/treino-do-dia/finalizar`, `GET`/`POST /api/v1/atividades` (treino livre), `GET`/`DELETE /api/v1/atividades/{id}` |

Convenções:

- **Conta nova começa pelo onboarding.** Enquanto ele não é feito, toda
  rota da API responde `409` com `type`
  `urn:corpo-forte:problema:onboarding-pendente`, menos `/me`, o próprio
  onboarding e a checagem de username. O cliente olha
  `onboardingConcluido` no `/me` logo depois do login. O onboarding pede
  nome, username, altura, idade, objetivo, nível e peso; o peso vira o
  primeiro registro de peso.
- **Username**: 3 a 30 caracteres, só letras minúsculas, números, `_` e
  `.`; único sem diferenciar maiúsculas; alguns nomes do sistema são
  reservados.
- **Toda lista** vem como `{"itens": [...], "proximoCursor": "..."}`. Pra
  próxima página, repetir a chamada com `?cursor=<proximoCursor>`;
  `proximoCursor` nulo quer dizer que acabou. O cursor marca a posição do
  último item (não um número de página), então post novo chegando entre
  uma página e outra não faz nada repetir nem sumir.
- **Marcar e desmarcar são idempotentes**: `PUT` marca (curtida, item
  concluído, seguir, bloqueio), `DELETE` desmarca. Repetir a mesma
  requisição não desfaz nada.
- **Visibilidade de post**: `PUBLICO`, `SEGUIDORES` ou `SOMENTE_EU`,
  escolhida ao publicar (sem escolher, vale o padrão da conta). Bloqueio
  vale nos dois sentidos: nenhuma das duas contas vê o perfil, os posts
  nem os comentários da outra. O que alguém não pode ver responde `404`,
  igual a algo que não existe, em todas as rotas — feed, perfil, página
  do post, curtir e comentar.
- **Horários** saem em UTC (`2026-09-23T18:54:18.053176Z`); datas de
  calendário (dia do peso, da avaliação, do treino) saem como `2026-09-23`.
- **Pré-requisito faltando** responde `409` com um `type` estável pro
  cliente decidir o que mostrar: `urn:corpo-forte:problema:avaliacao-pendente`
  (treino sem avaliação), `...:avaliacoes-insuficientes` (comparação com
  menos de duas), `...:registro-de-peso-pendente` (tendência sem pesagem),
  `...:treino-sem-itens-concluidos` (finalizar o treino sem nenhum item
  marcado) e `...:treino-ja-finalizado` (o treino do dia vira uma
  atividade só). Username de outra conta responde
  `...:username-indisponivel`.
- **Diário de treino**: cada atividade guarda as séries na ordem em que
  foram feitas (`"series": [10, 10, 8]`). Cada exercício do catálogo tem
  uma `medida`: `REPETICOES`, ou `SEGUNDOS` nos isométricos (ex.: wall
  sit). Finalizar o treino do dia registra os itens marcados com a
  prescrição, e `ajustes` troca o que foi feito diferente (quem fez 8 em
  vez de 10 registra 8). O treino livre é montado com exercícios do
  catálogo. O diário é só da própria conta, e só o treino do dia
  concluído faz o volume progredir.
- **Fotos**: JPEG ou PNG, até 5 MB cada (o tipo é conferido pelo conteúdo
  do arquivo, não pela extensão). Toda foto é re-codificada no servidor:
  metadados — inclusive a localização GPS que o celular grava — não são
  guardados, e a rotação da câmera já vem aplicada. A API devolve URLs
  assinadas que expiram (entre 1 e 2 horas) e funcionam direto num
  `<img>`, sem login; pra URLs novas, basta pedir o post de novo.
- Dado corporal (peso, altura, idade, avaliação) só aparece nas rotas da
  própria conta. No feed e no perfil público aparece só a identidade
  pública: nome, username, foto (a do Google) e bio.

Variáveis de ambiente (todas opcionais pra rodar local, nunca commitadas):

| Variável | Para quê | Sem ela |
|---|---|---|
| `APP_JWT_SECRET` | segredo do access token, com 32+ bytes | segredo aleatório a cada boot (tokens morrem ao reiniciar) |
| `APP_GOOGLE_CLIENT_IDS` | client IDs OAuth do Google aceitos no login pela API, separados por vírgula | login pela API desligado |
| `APP_CORS_ORIGINS` | origens web que podem chamar a API pelo navegador, separadas por vírgula | nenhuma origem externa |
| `APP_ARQUIVOS_DIRETORIO` | onde as fotos são guardadas | `./dados/arquivos` |
| `APP_ARQUIVOS_URL_BASE` | endereço público das fotos, usado nas URLs devolvidas pela API | `http://localhost:8090/arquivos` |
| `APP_ARQUIVOS_SEGREDO` | segredo da assinatura das URLs de foto | aleatório a cada boot (URLs morrem ao reiniciar) |

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
11. **API das funcionalidades existentes** — perfil, avaliação física,
    peso, equipamentos, catálogo, treino do dia e feed pela API, usando os
    mesmos services das telas (regras que moravam em controller de tela,
    como a sincronização do peso, desceram pro service). Listas paginadas
    por cursor, `DELETE` de verdade pra apagar, curtir e concluir item
    idempotentes. ✅
12. **Onboarding e perfil público** — conta nova deixa de usar dados
    inventados: o cadastro inicial pede os dados reais e um `@username`
    (o peso vira o primeiro registro de peso), e a API fica bloqueada até
    ele ser feito. Perfil público por username com foto do Google, bio,
    contagem e posts paginados, sem nenhum dado corporal. Contas que já
    existiam ganharam um username gerado. ✅
13. **Seguir** — seguir é de mão única e sem pedido de aprovação; listas
    de seguidores e de quem a conta segue; feed "Seguindo" (os próprios
    posts e os de quem a pessoa segue) ao lado do "Descobrir" (global);
    página do post; busca de conta pelo começo do username ou do nome. ✅
14. **Privacidade e bloqueio** — cada post escolhe quem vê (todos, só
    seguidores ou só a própria pessoa), com um padrão configurável por
    conta; bloquear some com o perfil, os posts e os comentários nos dois
    sentidos e desfaz o seguir. A regra de quem vê o quê mora num lugar só
    e vale igual pra API e pras telas. ✅
15. **Fotos** — até 4 fotos por post e foto de perfil própria (que passa
    a valer no lugar da do Google). Toda imagem é conferida pelo conteúdo,
    re-codificada sem metadados (a localização GPS some), redimensionada e
    servida por URL assinada com validade, gerada só pra quem pode ver o
    post. Apagar o post apaga os arquivos. ✅
16. **Diário de treino** — o treino feito vira uma atividade: finalizar o
    treino do dia registra os itens marcados (com o que foi feito de
    verdade, se diferente do prescrito), e o treino livre é montado com
    exercícios do catálogo. Isométricos passaram a ser medidos em
    segundos. O diário é privado, e só o treino do dia concluído avança a
    periodização. ✅
