# managerFoot

Simulador de gestão e partidas de futebol inspirado no Elifoot 98.
Requisitos em [`SPEC.md`](SPEC.md), diretrizes em [`CLAUDE.md`](CLAUDE.md),
decisões em [`docs/decisions/`](docs/decisions/README.md).

## Pré-requisitos

- JDK 21 ou superior. O projeto compila com `release 21`.
- Docker, usado pelo banco local e pelos testes de integração.
- Maven não é necessário: use o wrapper `./mvnw`.

## Configuração local

```bash
cp .env.example .env      # ajuste DB_PASSWORD; .env não vai para o git
docker compose up -d      # sobe PostgreSQL 18 na porta 5432
```

## Executar a aplicação

```bash
set -a && . ./.env && set +a
./mvnw spring-boot:run
curl localhost:8080/actuator/health   # {"status":"UP",...}
```

As migrações Flyway (`src/main/resources/db/migration`) são aplicadas
automaticamente na inicialização.

Alternativa sem `.env`: `./mvnw spring-boot:test-run` sobe a aplicação com um
PostgreSQL temporário via Testcontainers.

## Testes

| Comando | O que roda |
|---|---|
| `./mvnw test` | Testes unitários (`*Test.java`), rápidos, sem Docker |
| `./mvnw verify` | Unitários + integração (`*IT.java`) com PostgreSQL real via Testcontainers |

## Estrutura de pacotes

`br.com.managerfoot`:
- `dominio`: regras do jogo.
- `motor`: simulação de partidas.
- `temporada`: calendário e competições.
- `economia`: finanças e mercado.
- `multiplayer`: sessões em rede.
- `persistencia`: acesso ao banco e saves.
- `web`: telas.

Ver [ADR 0005](docs/decisions/0005-pacotes-e-persistencia-inicial.md).
