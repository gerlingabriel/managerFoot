# 0023 — Tirar um treinador do jogo em rede começado

- **Status:** Aceita (SPEC §11.4, rev. 104; tela aprovada pelo usuário)
- **Data:** 2026-11-04

## Contexto

- O jogo em rede só segue com o OK de todos (ADR 0021), sem máquina jogando pelo ausente. Quem some
  depois que a temporada começa trava o jogo de todos.
- Antes de começar, o administrador já tira alguém da sala (`SalaRepositorio.tirar`): o clube volta à
  máquina.
- O administrador do jogo é quem o criou (`jogo.dono_id`), e não qualquer conta com papel de
  administrador.

## Opções consideradas

1. **Deixar como está:** o jogo espera indefinidamente.
2. **O administrador tira o treinador entre as rodadas** (escolhida pelo usuário).
3. **Tempo limite automático:** a máquina assume depois de um tempo sem conexão. Tira de alguém a decisão
   que o usuário quis com o administrador.

## Decisão

- `TurnoRepositorio.tirarDoJogo`, com a trava do jogo. Só quem criou o jogo, nunca a si mesmo, e só
  entre as rodadas (com o turno aberto: tática, lances ou resultado dos leilões; nunca ao vivo).
- O clube passa à máquina como na sala: sem `jogo_treinador`, sem escalação gravada e com o salário solto
  (`forca_salario` nulo, como na troca de treinador). O treinador da máquina já está ligado ao clube.
- O que esperava a decisão de quem sai: o patrocínio não escolhido é assinado como fixo, o convite para
  outro clube é recusado (a vez passa ao seguinte) e os lances guardados são apagados.
- Depois de tirar, se todos os que ficaram já deram OK no momento atual, o jogo segue na mesma transação,
  como no último OK.
- Tela: "Tirar do jogo" na faixa "Treinadores" só para o administrador; a confirmação fica numa tela à
  parte (`/jogos/{id}/rede/tirar`), porque a faixa se refaz a cada segundo e fecharia uma janela aberta
  dentro dela.

## Consequências

- Quem sai perde o jogo da lista e não volta: depois que a temporada começa, ninguém entra.
- O histórico de treinadores do clube continua com o nome de quem saiu nas temporadas já jogadas.
