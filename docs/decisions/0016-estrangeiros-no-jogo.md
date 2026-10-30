# 0016 — Clubes estrangeiros no jogo para a competição continental

- **Status:** Aceita (SPEC §8.3.6, rev. 72 a 75)
- **Data:** 2026-10-29

## Contexto

- A competição continental põe clubes do país jogado contra clubes de outros países. Os de fora não
  têm liga jogada, caixa, mercado nem evolução (rev. 73), mas jogam partidas com eventos, cartões e
  lesões.
- `jogo_partida`, `jogo_evento`, a escalação e a rodada ao vivo apontam para `jogo_clube` e
  `jogo_jogador`. Tabelas à parte para os estrangeiros obrigariam a duplicar esse caminho.
- Várias consultas tratam "todos os clubes do jogo" como a liga: `TabelasLiga.carregar` poria os
  clubes sem divisão na fila de espera, e o caixa abriria saldo para eles.

## Opções consideradas

1. **Tabelas próprias** (`jogo_clube_estrangeiro`, `jogo_jogador_estrangeiro`): isolamento total, mas as
   partidas, os eventos e a tela ao vivo precisariam aceitar dois tipos de clube.
2. **`jogo_clube` com o país do estrangeiro** (`pais_codigo`, nulo = país jogado) e filtro nas
   consultas que percorrem os clubes do jogo.
3. **Copiar os estrangeiros a cada temporada:** o jogo teria clubes que aparecem e somem.

## Decisão

Opção 2 (V33).

- Na criação, o jogo copia do molde a competição da região do país jogado (`jogo_continental`,
  `jogo_continental_vaga`) e os clubes que podem ocupar as vagas: de um país com liga, os mais fortes
  no tamanho da 1ª divisão; de um país gerado, todos (rev. 75).
- Um estrangeiro não tem divisão (`ck_jogo_clube_estrangeiro_sem_divisao`) e fica fora de:
  `TabelasLiga.carregar` (liga, fila, planteis, evolução, mercado da máquina), `abrirCaixas` e
  `folhas`. As consultas que só pegam clubes com divisão já os deixam de fora.
- Continuam valendo para ele o que é da partida: cartões, suspensões e lesões.
- `ContinentalNoJogoIT` joga rodadas e confere que nada muda nos estrangeiros.

## Consequências

- Toda consulta nova que percorra os clubes do jogo sem filtrar pela divisão precisa filtrar
  `pais_codigo IS NULL`.
- Jogos criados antes da V33 não têm estrangeiros nem continental.
- O editor de moldes, quando existir, pode trocar os estrangeiros só no molde; o jogo já criado
  mantém a cópia.

## Adendo (2026-10-29, V34): partidas da continental no calendário

- A partida de grupo fica em `jogo_partida` com `grupo` e `jogo_do_grupo`, sem divisão e sem confronto;
  o mata-mata reusa `jogo_copa_confronto` com `competicao = 'CONTINENTAL'`. Uma partida tem
  exatamente um entre divisão, confronto e grupo.
- **A liga passa a ser reconhecida pela divisão** (`divisao_id IS NOT NULL`), e não mais pela falta de
  confronto, nas consultas da liga (calendário, total de rodadas, público, classificação do clube). As
  consultas da copa olham `competicao = 'COPA'` do confronto. Gravar um placar filtra pela competição,
  porque as finais da copa e da continental caem na mesma rodada e podem ter os mesmos clubes.
- O sorteio dos grupos no início da temporada (`ContinentalRepositorio.criar`) só roda com
  `managerfoot.continental.ativa=true`. A chave fica desligada até as telas da continental serem
  aprovadas, para nenhum jogo parar numa etapa que a interface não mostra.
