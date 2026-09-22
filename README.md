# Corpo Forte Tracker

Plataforma de treino e (no futuro) comunidade de calistenia. Construída em
etapas pequenas — ver o plano completo e o roadmap de fases no histórico do
projeto.

## Stack

Java 21 · Spring Boot 3.5 · Spring Data JPA · Bean Validation · PostgreSQL ·
Flyway · Thymeleaf · Maven.

## Como rodar

1. Subir o banco:
   ```
   docker compose up -d
   ```
2. Rodar a aplicação:
   ```
   mvn spring-boot:run
   ```
3. Abrir http://localhost:8090 (redireciona para `/perfil`). A avaliação
   física fica em `/avaliacao`, o registro de peso em `/peso`.

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
4. Equipamentos disponíveis + catálogo de exercícios.
5. Geração automática do treino do dia + checklist.
6. Login com Google (multiusuário).
7. Comunidade/blog (posts, comentários, curtidas) — desenho futuro.
# corpo-forte
