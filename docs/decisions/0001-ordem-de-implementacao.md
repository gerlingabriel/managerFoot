# 0001 — Ordem única de implementação

- **Status:** Aceita
- **Data:** 2026-10-26
- **Decidido por:** agente, com delegação do usuário ("pode decidir qual a
  melhor forma de começar").

## Contexto

A SPEC tinha dois planos conflitantes (§15, começando pelo motor; §18,
começando pela base técnica e colocando interface antes da economia), e o
CLAUDE.md §12 sugeria uma terceira ordem.

## Opções consideradas

1. Motor de partidas primeiro, sem projeto Spring (só Java puro).
2. Base técnica completa (Spring, banco, telas) antes de qualquer regra.
3. Base técnica mínima, depois motor e liga como domínio puro, e só então
   telas, economia e multiplayer.

## Decisão

Opção 3. A ordem oficial está na SPEC §15.

1. Base técnica mínima: Maven, Spring Boot, PostgreSQL, Flyway, Testcontainers
   e health check.
2. Motor de partidas como domínio puro (sem Spring nem banco), com semente
   e simulador em lote para desenvolvimento.
3. Liga e turno: calendário em turno e returno, classificação, desempate,
   promoção, rebaixamento e fila de espera.
4. Contas, dados padrão e interface do modo individual.
5. Economia.
6. Multiplayer.
7. Copas, competições continentais e Mundial.

## Consequências

- O motor e a liga ficam testáveis sem banco nem interface (CLAUDE.md §15).
- A base técnica mínima evita refazer a estrutura de pacotes depois.
- A economia chega depois do jogo jogável. Até lá, as partidas não geram
  receita.
