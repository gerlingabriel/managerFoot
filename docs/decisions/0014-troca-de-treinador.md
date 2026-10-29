# 0014 — Troca de treinador pela moral no banco

- **Status:** Aceita (SPEC §5.4, rev. 59 a 63; modelos 11a, 11b e 11c aprovados)
- **Data:** 2026-10-29

## Contexto

- Depois de cada rodada, quem estava no vermelho e continua é demitido (o jogo decisivo, §5.4), máquina
  e usuário pelas mesmas regras. O demitido chama o treinador de um clube inferior (até 12 pontos) de
  moral verde: os dois trocam de clube e recomeçam com moral 22. Entre os 3 inferiores mais fortes, o
  usuário decide primeiro (convite); quem recusa passa a vez ao seguinte; sem ninguém, o treinador fica.
- O clube do usuário guarda o seu treinador da máquina escondido (ADR 0013).
- A rodada já espera decisões do usuário (patrocínio, aumento) com `DecisaoPendenteException`.

## Decisão

- **Regra no domínio:** `temporada.TrocaDeTreinador` dá a ordem dos chamados; `Moral.demite`, o
  gatilho. `persistencia.TrocaRepositorio` aplica no fim de `RodadaRepositorio.avancar`, na mesma
  transação, do clube mais forte ao mais fraco; um clube entra em no máximo uma troca por rodada.
- **Uma operação de troca:** os dois clubes trocam os treinadores da máquina e, se houver, o usuário.
  Como o clube do usuário tem o seu treinador da máquina escondido, todo clube continua com exatamente
  um. Dois usuários nunca trocam entre si: o usuário demitido só troca com a máquina (multiplayer:
  rever). Quem passa a ter usuário recebe a escalação da máquina e grava os salários (rev. 58).
- **Tabela `jogo_troca_treinador` (V28):** histórico das trocas e estado do convite e do aviso
  (`TROCA`, `CONVITE`, `RECUSADO`, `CONTINUA`). Um convite aberto por jogo (índice parcial). O convite
  trava a rodada seguinte (`RodadaAoVivoRepositorio.iniciar` e, antes dos leilões, `RodadaController`).
  Aceitar e recusar travam a linha do convite (`FOR UPDATE`): clique repetido não faz nada.
- **Telas:** convite (11a) e aviso (11b) abrem no plantel, que é para onde a rodada volta ao terminar,
  antes de patrocínio e aumento; a faixa do clube (11c) fica no topo da janela do clube.

## Consequências

- A troca é recalculada na recusa com a moral gravada, sem os clubes já envolvidos naquela rodada.
- Quem avança a rodada sem responder (testes, chamadas diretas) deixa os dois clubes do convite fora
  das trocas seguintes até a resposta.
- **Não feito:** treinador sem clube (fila de treinadores) para clubes de força até 10: o jogo ainda
  não cria treinadores sem clube. O usuário que cai para a fila de clubes continua com o sorteio da
  rev. 36.
