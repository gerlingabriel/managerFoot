# Quem sobe da 2ª: variantes da evolução de força

Gerado por `RelatorioCicloMercadoTest` com `RELATORIO_PROMOVIDOS=1` (ferramenta de desenvolvimento; SPEC §15, Etapa 6). O simulador de `ciclo-do-mercado.md` com o padrão atual: as 6 ligas do banco padrão, 3 sementes, 6 temporadas, só máquina. No jogo completo (`jogo-completo.md`) 81% dos promovidos caem de volta.

Variantes só na simulação: *cresce 2×/3×*: no 1º ano na 1ª, os sorteados que ganham 1 ponto (4, 2 ou 0,5 por partida, §6.1) são o dobro/triplo. *Base móvel*: o piso do desgaste (força do clube − 5) usa a força em campo do clube no início de cada temporada, em vez da força cadastrada, que hoje nunca muda. *Teto da 2ª 35*: na 2ª os jogadores crescem até 35 em vez de 30; os clubes do banco padrão continuam com 23 a 30.

*Caem de volta*: terminam entre os últimos, tantos quantos subiram. *Força*: força em campo no início menos a média da 1ª. *Força no fim por origem*: força em campo no fim da 6ª temporada dos clubes que começaram na 1ª e na 2ª.

| Cenário | Promovidos | Posição média | Caem de volta | Força em relação à média da 1ª | Força no fim: começaram na 1ª / na 2ª | Força em campo 1ª / 2ª (fim) | Saldo 1ª / 2ª (fim, rodadas de folha) |
|---|---|---|---|---|---|---|---|
| Atual | 270 | 17,6ª | 70% | -9,8 | 38,6 / 22,3 | 38,1 / 22,7 | 109,0 / 107,6 |
| Promovido cresce 2× no 1º ano | 270 | 17,3ª | 69% | -9,6 | 38,7 / 22,6 | 38,2 / 22,8 | 108,8 / 106,8 |
| Promovido cresce 3× no 1º ano | 270 | 17,2ª | 64% | -9,5 | 38,8 / 22,7 | 38,6 / 22,7 | 104,7 / 107,6 |
| Base móvel: piso pela força em campo no início da temporada | 270 | 16,4ª | 47% | -8,8 | 34,5 / 16,4 | 34,2 / 16,6 | 162,7 / 124,6 |
| Base móvel e promovido cresce 2× | 270 | 15,9ª | 47% | -8,8 | 35,2 / 17,5 | 35,4 / 17,3 | 140,9 / 124,4 |
| Teto da 2ª 35 (em vez de 30) | 270 | 17,2ª | 65% | -8,1 | 39,1 / 22,4 | 38,1 / 23,1 | 113,7 / 104,6 |
| Teto da 2ª 35 e base móvel | 270 | 15,9ª | 47% | -7,5 | 35,3 / 17,1 | 34,9 / 17,4 | 153,9 / 120,1 |
| Ingresso R$ 30 (sem mudar a evolução) | 270 | 17,5ª | 67% | -9,4 | 38,7 / 22,6 | 38,2 / 22,9 | 24,6 / 11,4 |
