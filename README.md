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
   física fica em `/avaliacao`.

## Testes

```
mvn test
```

## Fases

1. **Perfil** — cadastro do usuário + cálculo de TMB/TDEE/IMC/macros. ✅
2. **Avaliação física** — teste de repetições máximas em 6 padrões de
   movimento + cálculo do volume de treino inicial. ✅
3. Registro de peso + tendência semanal.
4. Equipamentos disponíveis + catálogo de exercícios.
5. Geração automática do treino do dia + checklist.
6. Login com Google (multiusuário).
7. Comunidade/blog (posts, comentários, curtidas) — desenho futuro.
# corpo-forte
