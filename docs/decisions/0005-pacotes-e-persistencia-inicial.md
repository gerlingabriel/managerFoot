# 0005 — Estrutura de pacotes e persistência inicial

- **Status:** Aceita
- **Data:** 2026-10-26

## Contexto

A Etapa 0 (SPEC §15) pede a base técnica e a estrutura de pacotes antes do
domínio. O usuário aprovou o pacote base `br.com.managerfoot`, um
docker-compose para o banco local e o uso da versão estável atual do Spring
Boot.

## Decisões

1. **Spring Boot 4.1.2.** Era a versão estável padrão do start.spring.io em
   2026-10-26. Java alvo: 21.
2. **PostgreSQL 18**, com versão principal fixada. É a mesma imagem no
   docker-compose e nos testes, para não divergir. Não usar `latest`.
3. **Pacotes por responsabilidade**, com nomes em português:
   - `dominio`, `motor`, `temporada`, `economia`, `multiplayer`,
     `persistencia`, `web`;
   - seguem as camadas do CLAUDE.md §4;
   - o vocabulário do jogo (escalação, rebaixamento, temporada) é
     brasileiro, e traduzir os nomes para o inglês criaria uma segunda
     linguagem.
4. **Só JDBC por enquanto, sem JPA.**
   - O Flyway precisa apenas de um DataSource.
   - A escolha entre JPA, Spring Data JDBC ou JDBC puro afeta o modelo
     persistido. Fica para a Etapa 3, quando houver tabelas reais, como
     decisão própria.
5. **Credenciais via variáveis de ambiente** (`DB_URL`, `DB_USER`,
   `DB_PASSWORD`):
   - `.env` fica no `.gitignore`;
   - `.env.example` documenta as variáveis;
   - a senha não tem valor padrão.
6. **Só o health check do Actuator exposto.**

## Consequências

- O motor e o domínio podem ser escritos sem nenhuma dependência de Spring.
- Thymeleaf e HTMX ainda não foram adicionados. Entram na Etapa 3, junto
  com as primeiras telas.
