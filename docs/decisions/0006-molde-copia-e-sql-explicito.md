# 0006 — Molde editável, cópia por jogo e SQL explícito

- **Status:** Aceita
- **Data:** 2026-10-27

## Contexto

- O banco padrão traz ligas de vários países (Brasil, Argentina, Colômbia
  e outros principais), e a pessoa pode criar ou alterar esses dados no
  editor (SPEC §3.5).
- Quem joga escolhe **um** país. Os clubes dos outros países existem só
  para copas continentais e Mundial; as ligas deles não são jogadas
  (SPEC §8.3.3).
- O jogo em andamento muda forças, gols, divisões e fila de espera a cada
  temporada.
- A 0005 adiou a escolha da forma de acesso ao banco para a Etapa 3.

## Opções consideradas

**Dados editáveis x jogo em andamento:**
1. **Molde + cópia:** o banco padrão ou editado é um molde. Ao iniciar um
   jogo, o servidor copia o molde para o jogo, que evolui sem alterá-lo.
2. **Uma só cópia:** o jogo altera diretamente os dados editados.

**Acesso ao banco:**
1. **SQL explícito** com `JdbcClient`, em repositórios dedicados.
2. **Spring Data JDBC.**
3. **JPA/Hibernate.**

## Decisão

Escolhidas pelo usuário em 2026-10-27:
- **Molde + cópia.** As tabelas do molde têm prefixo `molde_`; as do jogo
  em andamento terão prefixo `jogo_`.
- **SQL explícito com `JdbcClient`.** Os repositórios ficam em
  `persistencia` e convertem linhas nos records do domínio, que continuam
  sem dependência de Spring.

## Consequências

- Editar o molde não altera jogos já começados.
- Vários jogos podem nascer do mesmo molde.
- Cada país do molde tem uma liga nacional (nome, divisões e clubes). Um
  clube do país sem divisão está na fila de espera daquela liga.
- O SQL é escrito à mão: cada consulta nova precisa de teste com
  PostgreSQL real (Testcontainers).
- Mais código de mapeamento do que com um ORM, em troca de controle total
  sobre transações e consultas.
