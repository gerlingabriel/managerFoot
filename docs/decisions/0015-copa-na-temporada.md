# 0015 — Copa nacional entre as rodadas da liga

- **Status:** Aceita (SPEC §8.2, rev. 69 e 70)
- **Data:** 2026-10-29

## Contexto

- A copa tem um quadro fixo pelas rodadas da liga: cada fase de ida e volta é jogada depois de duas
  rodadas da liga (16 avos depois da 3ª e da 5ª, …), e a final única depois da última.
- Tudo na temporada conta "rodadas": `rodadas_processadas`, caixa, patrocínio, moral, demissões, rodada
  ao vivo. Gols, cartões e lesões valem para todas as competições (SPEC §5.2, §7.4, §7.5).
- A partida da copa tem os efeitos da liga, menos o salário, e usa a mesma tela da rodada (rev. 70).

## Decisão

- **Domínio puro:** `temporada.Copa` (inscritos, chave sorteada e fixa, quadro de etapas, avanço de
  fase), `ConfrontoCopa` (placar somado, prorrogação e pênaltis da volta) e, no motor,
  `JogoDecisivo` e `DisputaPenaltis` na `Partida`. A volta e a final passam ao motor quanto falta para
  o vencedor; o motor joga a prorrogação e os pênaltis sem saber de copa.
- **Partidas no mesmo calendário (V30):** a partida da copa fica em `jogo_partida` com `confronto_id`
  e `perna` em vez de `divisao_id`, e a rodada é a da liga depois da qual ela é jogada. Eventos,
  artilharia, cartões e o limite de lesões seguem funcionando sem mudança; as consultas só da liga
  filtram `confronto_id IS NULL` (classificação, calendário do clube, público por rodada).
- **Etapas contadas à parte:** `jogo_temporada.etapas_copa_processadas` avança uma etapa por vez, como
  `rodadas_processadas`. `RodadaRepositorio.proximo` diz o que vem: a etapa cuja rodada já passou vem
  antes da rodada seguinte da liga; a final, depois da última. `avancar` (liga) recusa enquanto houver
  etapa pendente; `avancarCopa` só aceita a etapa esperada, com a temporada travada. O fim de
  temporada espera a final.
- **Efeitos da etapa:** os mesmos métodos da rodada (disciplina, moral, evolução com o teto da divisão
  de cada clube, demissões), mais `CaixaRepositorio.lancarCopa` sem salário. `jogo_lancamento` ganhou
  `competicao` (V31) na chave, para o lançamento da copa não somar com o da rodada de mesmo número. O
  prêmio e o campeão (`jogo_campeao_copa`) são gravados ao fim da final.
- **Ao vivo:** `jogo_rodada_ao_vivo.etapa_copa` marca a etapa da copa. As partidas da copa têm a
  prioridade 0 (`ProcessamentoRodada.PRIORIDADE_COPA`) e formam um grupo só; o relógio desse grupo tem
  90 minutos ou 120, se uma partida está na prorrogação (calculado ao refazer as partidas).
- **Temporadas antigas** (antes da V30) não têm copa e seguem só com a liga.

## Consequências

- Uma liga cujo calendário termina antes da volta da semifinal (menos de 28 rodadas) fica sem copa.
- Continentais e Mundial podem reusar `Copa.Etapa`, `confronto_id` e `competicao`, com outra tabela de
  confrontos ou uma coluna de competição nela.
