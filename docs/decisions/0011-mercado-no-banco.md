# 0011 — Mercado no banco: anúncios, salário e lançamentos

- **Status:** Aceita (plano aprovado pelo usuário em 2026-10-28, "segue")
- **Data:** 2026-10-28

## Contexto

- As regras do mercado estão na SPEC §9.3 a §9.6 (rev. 40 a 46). O domínio
  já existe e é puro: `economia.PrecoJogador`, `MercadoDaMaquina`, `Leilao`
  e `Premiacao`, com simulações em `docs/simulacoes/`.
- Falta o estado no jogo salvo: quem está à venda, compras e vendas,
  patrocínio, prêmio e leilões. O servidor continua a autoridade (ADR 0004).
- O mercado entra em partes pequenas: (1) anúncios da máquina, (2)
  patrocínio e prêmio no extrato, (3) compra direta, (4) leilão da máquina
  no fluxo da rodada.

## Decisão

- **Anúncio** é uma coluna do jogador (`jogo_jogador.a_venda`, V17), não
  uma tabela própria: só há um tipo de anúncio (compra direta pelo preço
  da tabela, que é calculado e não gravado).
- Os anúncios da máquina são completados por `MercadoRepositorio.reporAVenda`
  ao gravar cada temporada (depois de os treinadores estarem nos clubes) e
  em cada avanço de rodada, na mesma transação. O sorteio usa a semente da
  temporada combinada com a rodada, para ser reproduzível. A mesma chamada
  retira anúncios de clubes de treinador humano.
- **Salário não é gravado por enquanto.** Hoje a força não muda (SPEC §6.1
  em aberto) e o salário segue a força, então a coluna só repetiria o
  cálculo. Ela entra com o pedido de aumento, quando a força puder mudar;
  nessa hora, nulo valerá "salário da força" para os jogos já salvos.
- Patrocínio, prêmio, compras e vendas serão **lançamentos do extrato**
  (`jogo_lancamento`), com chave única que impede pagar duas vezes. V18
  (parte 2): o patrocínio usa a **rodada 0** da temporada (o início) e o
  prêmio a última rodada processada. São lançados por quem cria o jogo
  (`JogosDoTreinadorRepositorio`) e pelo fim de temporada, e não dentro de
  `TemporadaRepositorio.gravar`, porque `CaixaRepositorio` já depende de
  `TemporadaRepositorio` (evita dependência circular).

## Consequências

- Jogos salvos antes da V17 ficam sem anúncios até o próximo avanço de
  rodada (ou a próxima temporada).
- Clube na fila de espera não anuncia; ao subir para uma divisão, ganha
  anúncios na temporada seguinte.
- Ler a lista de anúncios não grava nada: a reposição só acontece em
  operações que já escrevem (avanço de rodada, nova temporada).
