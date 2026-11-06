# Simulação do jogo completo

Gerado por `RelatorioJogoCompletoIT` (ferramenta de desenvolvimento; SPEC §15, Etapa 6). Cada jogo é criado do banco padrão e jogado pelo caminho do servidor, só com clubes da máquina: leilões da máquina antes de cada rodada, liga, copa, continental, Mundial, caixa, evolução de força, trocas de treinador e virada de temporada com promoção e rebaixamento. 1 semente por país (0 a 0), 10 temporadas seguidas cada; a semente da temporada é fixa, então o relatório se refaz igual.

*Força em campo*: média dos 11 mais fortes do plantel no início da temporada. *Rodadas de folha*: saldo no fim da temporada dividido pelos salários da última rodada. *Goleada*: um time com 5 gols ou mais. *Entre os 3 mais fortes*: o campeão da 1ª estava entre as 3 maiores forças em campo no início.

## Por país

| País | Gols por jogo | Mandante vence | Empate | Visitante vence | Goleadas | Expulsões por jogo | Lesões por jogo | Pontos do campeão (1ª) | Pontos do lanterna (1ª) | Campeão entre os 3 mais fortes | Mesmo campeão do ano anterior | Copa: campeão da 1ª | Continental: campeão do país | Vendas por temporada | Trocas de treinador por temporada |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| ARG | 2,8 | 48% | 23% | 30% | 3% | 0,21 | 0,06 | 82,6 (72%) | 22,3 (20%) | 80% | 44% | 100% | 60% | 21,2 | 22,4 |
| BRA | 2,8 | 48% | 22% | 30% | 3% | 0,21 | 0,06 | 84,0 (74%) | 19,5 (17%) | 80% | 22% | 90% | 30% | 19,8 | 22,9 |
| ENG | 2,8 | 47% | 23% | 30% | 3% | 0,21 | 0,06 | 83,9 (74%) | 18,2 (16%) | 100% | 33% | 100% | 10% | 26,1 | 23,3 |
| ESP | 2,8 | 47% | 23% | 30% | 3% | 0,20 | 0,06 | 81,0 (71%) | 20,1 (18%) | 70% | 33% | 100% | 0% | 23,5 | 23,0 |
| ITA | 2,8 | 48% | 22% | 30% | 3% | 0,20 | 0,06 | 87,6 (77%) | 21,1 (19%) | 100% | 44% | 100% | 0% | 25,0 | 21,5 |
| POR | 2,8 | 47% | 23% | 30% | 3% | 0,20 | 0,06 | 78,5 (77%) | 19,2 (19%) | 90% | 33% | 100% | 10% | 19,6 | 17,3 |
| Todos | 2,8 | 47% | 23% | 30% | 3% | 0,20 | 0,06 | 82,9 (74%) | 20,1 (18%) | 87% | 35% | 98% | 18% | 22,5 | 21,7 |

## Por temporada (todos os países)

| Temporada | Força em campo 1ª (média; menor a maior) | Força em campo 2ª | Saldo 1ª em rodadas de folha (mediana) | Clubes negativos 1ª | Saldo 2ª (mediana) | Clubes negativos 2ª | Estádio médio 1ª / 2ª | Gols por jogo | Vendas | Sem lance |
|---|---|---|---|---|---|---|---|---|---|---|
| 1 | 42,5 (35,1 a 49,6) | 26,6 (23,2 a 29,7) | 16,5 | 0% | 19,4 | 0% | 38.395 / 11.189 | 2,9 | 43,3 | 28,0 |
| 2 | 38,4 (27,0 a 48,6) | 24,4 (18,8 a 34,0) | 24,9 | 0% | 25,5 | 0% | 38.904 / 12.566 | 2,7 | 31,0 | 0,0 |
| 3 | 38,2 (26,5 a 49,3) | 22,6 (18,0 a 29,5) | 29,4 | 0% | 24,6 | 3% | 40.523 / 11.928 | 2,8 | 28,8 | 0,0 |
| 4 | 38,0 (26,1 a 49,6) | 22,8 (17,2 a 32,1) | 33,7 | 2% | 23,5 | 9% | 40.854 / 12.282 | 2,8 | 22,8 | 0,0 |
| 5 | 38,4 (26,2 a 49,8) | 22,6 (17,7 a 31,9) | 30,7 | 3% | 27,6 | 20% | 41.568 / 12.051 | 2,7 | 20,0 | 0,0 |
| 6 | 38,3 (26,5 a 49,9) | 23,0 (17,2 a 33,7) | 32,7 | 3% | 28,8 | 20% | 41.631 / 12.453 | 2,7 | 16,5 | 0,0 |
| 7 | 38,4 (26,3 a 50,0) | 22,6 (17,5 a 31,3) | 32,5 | 1% | 27,0 | 22% | 42.354 / 12.081 | 2,8 | 17,8 | 0,0 |
| 8 | 38,2 (24,6 a 50,0) | 23,0 (17,2 a 32,6) | 40,7 | 0% | 30,1 | 24% | 42.119 / 12.457 | 2,8 | 13,2 | 0,0 |
| 9 | 38,3 (25,0 a 50,0) | 23,0 (17,4 a 32,3) | 37,6 | 1% | 36,3 | 24% | 42.361 / 12.525 | 2,8 | 16,3 | 0,0 |
| 10 | 38,6 (26,2 a 50,0) | 22,8 (17,2 a 32,0) | 41,6 | 1% | 31,2 | 26% | 43.058 / 12.344 | 2,8 | 15,5 | 0,0 |

## Quem marca (liga, todos os países)

Gols da liga pela posição do autor; o pênalti fica à parte. *Artilheiro*: gols do maior artilheiro da 1ª na liga, média das temporadas. A máquina escolhe a formação a cada jogo (§7.2 dá os pesos por formação).

| País | Atacante | Meia | Defensor | Goleiro | Pênalti | Artilheiro da 1ª |
|---|---|---|---|---|---|---|
| ARG | 57% | 29% | 6% | 4 | 8% | 20,2 |
| BRA | 57% | 30% | 6% | 3 | 8% | 20,1 |
| ENG | 57% | 30% | 6% | 3 | 8% | 21,6 |
| ESP | 57% | 29% | 5% | 8 | 8% | 19,8 |
| ITA | 57% | 30% | 5% | 3 | 8% | 21,5 |
| POR | 58% | 29% | 5% | 2 | 8% | 19,3 |
| Todos | 57% | 30% | 5% | 23 | 8% | 20,4 |

## Quem sobe da 2ª (todos os países)

Clubes que subiram no fim da temporada anterior, na 1ª. *Caem de volta*: terminam entre os últimos, tantos quantos subiram (a zona de rebaixamento). *Força*: força em campo no início menos a média da 1ª.

| Temporada | Promovidos | Posição média | Caem de volta | Força em relação à média da 1ª |
|---|---|---|---|---|
| 2 | 18 | 18,6ª | 94% | -10,2 |
| 3 | 18 | 17,7ª | 72% | -9,3 |
| 4 | 18 | 17,8ª | 83% | -10,7 |
| 5 | 18 | 17,9ª | 72% | -9,4 |
| 6 | 18 | 17,3ª | 78% | -9,9 |
| 7 | 18 | 17,6ª | 67% | -9,7 |
| 8 | 18 | 17,7ª | 67% | -10,8 |
| 9 | 18 | 17,3ª | 61% | -10,6 |
| 10 | 18 | 16,3ª | 56% | -9,8 |
| Todas | 162 | 17,6ª | 72% | -10,0 |

## Caixa por temporada, por tipo (todos os países)

Média dos clubes da divisão no fim da temporada, em rodadas da folha do próprio clube (positivo entra, negativo sai). Uma temporada de liga com 20 clubes tem 38 rodadas.

| Temporada | Divisão | BILHETERIA | COMPRA | CUSTO_DO_JOGO | DESLOCAMENTO | ESTADIO | PATROCINIO | PREMIO | SALARIOS | VENDA | Saldo da temporada |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 1 | 1ª | 61,5 | -2,9 | -9,2 | -7,6 | -1,7 | 1,2 | 1,4 | -39,9 | 5,1 | 7,8 |
| 1 | 2ª | 64,0 | -0,8 | -9,6 | -12,0 | -0,6 | 1,0 | 0,0 | -40,4 | 7,1 | 8,6 |
| 2 | 1ª | 57,4 | -1,4 | -8,6 | -8,5 | -1,7 | 1,0 | 1,0 | -38,1 | 2,3 | 3,4 |
| 2 | 2ª | 63,8 | -2,1 | -9,6 | -12,8 | 0,0 | 0,9 | 0,0 | -38,4 | 6,5 | 8,2 |
| 3 | 1ª | 54,8 | -2,0 | -8,2 | -7,7 | -0,6 | 0,9 | 1,4 | -37,0 | 2,9 | 4,5 |
| 3 | 2ª | 61,9 | -1,9 | -9,3 | -13,4 | -0,1 | 1,0 | 0,0 | -37,2 | 4,9 | 5,7 |
| 4 | 1ª | 54,7 | -1,3 | -8,2 | -8,1 | -0,6 | 1,0 | 1,5 | -37,7 | 3,0 | 4,3 |
| 4 | 2ª | 60,4 | -2,7 | -9,1 | -13,5 | 0,0 | 0,9 | 0,3 | -36,5 | 2,7 | 2,5 |
| 5 | 1ª | 54,0 | -1,1 | -8,1 | -7,7 | -0,4 | 0,9 | 1,2 | -38,0 | 3,3 | 4,1 |
| 5 | 2ª | 60,4 | -2,7 | -9,1 | -13,4 | -0,3 | 1,0 | 0,0 | -36,6 | 1,1 | 0,4 |
| 6 | 1ª | 54,4 | -1,1 | -8,2 | -7,7 | -0,3 | 1,0 | 1,1 | -37,8 | 2,7 | 4,0 |
| 6 | 2ª | 62,6 | -2,1 | -9,4 | -13,6 | 0,0 | 0,9 | 0,1 | -37,0 | 1,0 | 2,5 |
| 7 | 1ª | 54,9 | -1,2 | -8,2 | -7,6 | -0,3 | 0,9 | 1,4 | -37,9 | 3,1 | 5,0 |
| 7 | 2ª | 61,7 | -2,6 | -9,2 | -13,6 | -0,2 | 1,0 | 0,0 | -36,5 | 0,9 | 1,4 |
| 8 | 1ª | 54,1 | -1,0 | -8,1 | -7,9 | -0,0 | 0,9 | 1,1 | -37,8 | 2,4 | 3,7 |
| 8 | 2ª | 61,5 | -2,0 | -9,2 | -13,6 | 0,0 | 1,0 | 0,2 | -36,8 | 0,5 | 1,7 |
| 9 | 1ª | 54,5 | -1,4 | -8,2 | -7,8 | -0,3 | 0,9 | 0,9 | -37,7 | 3,1 | 4,0 |
| 9 | 2ª | 61,3 | -1,4 | -9,2 | -13,6 | -0,2 | 1,0 | 0,0 | -36,8 | 0,6 | 1,7 |
| 10 | 1ª | 54,1 | -1,3 | -8,1 | -7,6 | -0,4 | 0,9 | 1,1 | -37,6 | 2,5 | 3,6 |
| 10 | 2ª | 62,0 | -1,7 | -9,3 | -13,6 | 0,0 | 1,0 | 0,0 | -36,7 | 1,1 | 2,8 |

Tempo de simulação: 843 s para 60 temporadas.
