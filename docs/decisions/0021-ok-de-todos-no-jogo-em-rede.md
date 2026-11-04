# 0021 — OK de todos no jogo em rede

- **Status:** Aceita (SPEC §3.3 itens 4 a 7, rev. 92; parte 5b; modelo 18e)
- **Data:** 2026-11-03

## Contexto

- Depois que a temporada começa, o jogo em rede só segue com o OK de todos (rev. 90). Não há máquina
  jogando pelo ausente, nem tempo limite.
- Os leilões vêm depois do OK da tática e também esperam todos (rev. 92, item 6). Os lances são secretos.
- No jogo individual, "Confirmar e jogar" começa a rodada na hora, e os lances do treinador são
  resolvidos assim que ele os confirma.
- O estado do jogo e o relógio da rodada já são do jogo, não de cada treinador: a rodada ao vivo é uma
  linha por jogo.

## Opções consideradas

1. **Um OK por treinador para o momento atual do jogo; o último OK faz o jogo seguir** (escolhida). O
   momento é calculado do calendário (`RodadaRepositorio.proximo`) e do passo dos leilões.
2. **Um processo no servidor que confere de tempos em tempos se todos deram OK.** Precisa de agendador e
   de cuidado com dois servidores; nada ganha sobre fazer na transação do último OK.
3. **O administrador começa cada rodada.** Contraria "o jogo só segue com o OK de todos".

## Decisão

- Migração V42: `jogo_rede_participante.ok_momento/ok_em`, `jogo_rede.momento_leiloes` e
  `jogo_rede_lance` (lances guardados até todos darem os seus).
- Momentos, por temporada: `t1/r6/tatica`, `t1/r6/lances`, `t1/r6/leiloes` (resultado), `t1/copa2`,
  `t1/continental0`, `t1/mundial1` e `t1/fim`. O OK guarda o momento; quando todos têm o mesmo, o jogo
  segue e todos os OK são apagados.
- `TurnoRepositorio.darOk` trava a linha do jogo. Quem dá o último OK faz o jogo seguir na mesma
  transação: abre os leilões, resolve-os, começa a rodada ou a etapa ao vivo, ou começa a temporada
  seguinte. Um OK com a tela de outro passo é recusado, então um clique atrasado não vale para o passo
  seguinte.
- Com o OK dado, a escalação fica travada no servidor (`PlantelRepositorio`), não só na tela.
- Antes do OK, só as decisões do próprio treinador são exigidas (patrocínio, convite, pedido de
  aumento). As dos outros esperam o OK deles.
- Leilões: quem não pode dar lance em nenhum já entra com o OK dos lances. Se ninguém pode, os leilões
  saem resolvidos. Os lances de todos entram de uma vez no mesmo sorteio de empates do jogo individual.
- A faixa "Aguardando" consulta a cada 3 s e marca a presença. Quando o momento muda, a tela vai para o
  plantel, que leva aos leilões ou à rodada.

## Consequências

- **Provisório:** o resultado dos leilões também pede o OK de todos ("Ir para a rodada"), para ninguém
  perder o começo da rodada lendo o resultado. São três OK nas rodadas com leilão em que alguém pode dar
  lance.
- Os lances de um treinador no leilão de outro treinador já funcionam aqui (parte 5d), porque o
  domínio dos leilões já aceitava lances de vários humanos.
- A rodada ao vivo ainda é a do jogo individual: qualquer um pausa e muda a velocidade. A pausa de 60 s
  e a velocidade do administrador são a parte 5c.
- A lista de jogos ainda não mostra "Aguardando o seu OK" (modelo 18a).
