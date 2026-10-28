# 0012 — Leilões da máquina antes da rodada

- **Status:** Aceita (parte 4 do plano da ADR 0011, aprovado pelo usuário em 2026-10-28; modelo 7b aprovado)
- **Data:** 2026-10-28

## Contexto

- SPEC §9.4 (rev. 41 a 45): o leilão é na hora. Os jogadores postos à venda pela máquina vão a
  leilão depois do OK da tática e antes do minuto a minuto; o usuário dá os lances e confirma, e
  depois a máquina dá os dela. Empate por sorteio; sem lance o jogador fica e só volta depois da
  rodada; usuário sem caixa para o lance inicial não é esperado.
- A regra de quem vende e quem compra já foi aprovada e simulada (`economia.MercadoDaMaquina`,
  `docs/simulacoes/mercado-maquina.md`).
- Em rede (depois), vários treinadores darão lances no mesmo leilão.

## Decisão

- **Domínio:** `economia.LeiloesDaRodada` abre os leilões (1 por clube da máquina abaixo do limite,
  na ordem dos clubes) e resolve-os em sequência, com os lances dos humanos e depois os da máquina,
  pela mesma regra da simulação. O caixa e o plantel de cada clube são atualizados a cada leilão:
  um humano não gasta duas vezes o mesmo caixa; cada clube da máquina compra no máximo 1.
- **Banco (V20):** `jogo_leilao` (um por jogador e rodada, chave única temporada + rodada +
  jogador) com a situação `ABERTO`, `VENDIDO`, `SEM_LANCE` ou `CANCELADO`; `jogo_leilao_lance`
  guarda os lances que valeram, de humanos e da máquina, um por clube — pronto para vários
  treinadores em rede.
- **Fluxo:** "Confirmar e jogar rodada" chama `LeilaoRepositorio.preparar`, que trava o jogo, abre
  os leilões da rodada uma vez só e diz a etapa: `RODADA` (nada pendente), `LANCES` (o treinador
  pode dar lance em algum) ou `RESULTADO` (resolvidos agora, sem esperar por ele). A tela
  `/jogos/{id}/leiloes` mostra os lances ou o resultado; "Ir para a rodada" confirma de novo e,
  sem leilão pendente, a rodada ao vivo começa. `RodadaAoVivoRepositorio.iniciar` recusa começar
  com leilão aberto.
- O sorteio dos empates usa a semente da temporada combinada com a rodada (reproduzível).
- Compra e venda viram lançamentos do extrato pela mesma operação da compra direta
  (`TransferenciaRepositorio`, separado para não criar dependência circular entre caixa, temporada
  e mercado).
- O lance é digitado em valor inteiro na moeda da liga; o mínimo aceito é o lance inicial
  arredondado para cima nessa moeda.
- **Proposta do agente:** se o treinador comprar direto um jogador que está em leilão aberto, o
  leilão é cancelado na resolução.

## Consequências

- O treinador vê os leilões a cada rodada em que algum clube da máquina estiver abaixo do limite;
  com o banco padrão, no início do jogo isso é raro.
- O leilão do clube do usuário com saldo negativo (SPEC §9.3, rev. 43) e a venda do usuário por
  leilão ficam para a próxima parte.
- O relatório da simulação (`RelatorioMercadoTest`) ainda tem a própria cópia do laço; ele abre e
  resolve cada leilão em seguida, e o jogo abre todos e depois resolve. A diferença só aparece se
  um comprador ficar abaixo do limite depois de comprar, o que a folga de 5 rodadas impede.

## Adendo (rev. 53) — venda do treinador por leilão

- O treinador põe um jogador do seu plantel em leilão pela ficha, com o lance mínimo que ele
  define; `LeilaoRepositorio.venderPorLeilao` grava o leilão e o resolve na mesma transação, com os
  lances da máquina. A tela é a de resultado do modelo 7b (`/jogos/{id}/leiloes/{leilão}`).
- **V21:** `jogo_leilao.tipo` (`RODADA` ou `VENDA`). Só os do tipo `RODADA` contam para "os leilões
  da rodada já abriram" e aparecem na tela antes da rodada. A chave única (temporada, rodada,
  jogador) continua para os dois tipos: sem lance, o jogador só volta depois da rodada.
- Cada resolução lê quem da máquina já comprou antes da mesma rodada (1 compra por rodada) e usa o
  id do primeiro leilão na semente do sorteio de empates, porque a rodada pode ter várias.
