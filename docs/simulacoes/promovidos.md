# Quem sobe da 2ª: variantes da evolução de força

Gerado por `RelatorioCicloMercadoTest` com `RELATORIO_PROMOVIDOS=1` (ferramenta de desenvolvimento; SPEC §15, Etapa 6). O simulador de `ciclo-do-mercado.md` com o padrão atual: as 6 ligas do banco padrão, 3 sementes, 6 temporadas, só máquina. No jogo completo (`jogo-completo.md`) 81% dos promovidos caem de volta.

*Fora da escalação cresce 1* (rev. 118, adotada no 1º turno): no clube que acabou de subir para a 1ª, todo jogador fora da escalação (nem titular nem no banco) ganha 1 ponto por partida, além do sorteio de sempre. Variantes só na simulação: *cresce 2×/3×*: no 1º ano na 1ª, os sorteados que ganham 1 ponto (4, 2 ou 0,5 por partida, §6.1) são o dobro/triplo. *Base móvel*: o piso do desgaste (força do clube − 5) usa a força em campo do clube no início de cada temporada, em vez da força cadastrada, que hoje nunca muda. *Teto da 2ª 35*: na 2ª os jogadores crescem até 35 em vez de 30; os clubes do banco padrão continuam com 23 a 30.

*Caem de volta*: terminam entre os últimos, tantos quantos subiram. *Força*: força em campo no início menos a média da 1ª. *Força no fim por origem*: força em campo no fim da 6ª temporada dos clubes que começaram na 1ª e na 2ª.

| Cenário | Promovidos | Posição média | Caem de volta | Força em relação à média da 1ª | Força no fim: começaram na 1ª / na 2ª | Força em campo 1ª / 2ª (fim) | Saldo 1ª / 2ª (fim, rodadas de folha) |
|---|---|---|---|---|---|---|---|
| Atual | 270 | 17,6ª | 73% | -9,8 | 38,8 / 22,3 | 38,2 / 22,7 | 107,5 / 108,0 |
| Promovido: fora da escalação cresce 1 por partida no 1º turno | 270 | 17,5ª | 73% | -9,8 | 38,8 / 22,4 | 38,3 / 22,6 | 109,8 / 104,3 |
| Promovido: fora da escalação cresce 1 por partida no 1º ano | 270 | 17,4ª | 69% | -9,7 | 38,9 / 22,6 | 38,6 / 22,7 | 106,1 / 106,1 |
| Promovido cresce 2× no 1º ano | 270 | 17,2ª | 65% | -9,7 | 38,9 / 22,6 | 38,4 / 22,8 | 106,8 / 103,8 |
| Promovido cresce 3× no 1º ano | 270 | 17,1ª | 63% | -9,6 | 38,9 / 22,8 | 38,6 / 22,8 | 104,4 / 104,0 |
| Base móvel: piso pela força em campo no início da temporada | 270 | 16,2ª | 49% | -8,8 | 34,6 / 16,4 | 34,4 / 16,5 | 154,9 / 122,3 |
| Base móvel e promovido cresce 2× | 270 | 15,9ª | 45% | -8,8 | 35,4 / 17,5 | 35,7 / 17,0 | 139,6 / 114,3 |
| Teto da 2ª 35 (em vez de 30) | 270 | 17,0ª | 64% | -8,1 | 39,2 / 22,5 | 38,3 / 23,1 | 112,2 / 105,1 |
| Teto da 2ª 35 e base móvel | 270 | 15,8ª | 44% | -7,4 | 35,5 / 16,9 | 35,2 / 17,1 | 150,0 / 113,8 |
| Ingresso R$ 30 (sem mudar a evolução) | 270 | 17,6ª | 71% | -9,5 | 38,8 / 22,6 | 38,1 / 23,0 | 23,5 / 11,0 |
