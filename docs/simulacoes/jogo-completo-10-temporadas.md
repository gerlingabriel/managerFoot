# Simulação do jogo completo

Gerado por `RelatorioJogoCompletoIT` (ferramenta de desenvolvimento; SPEC §15, Etapa 6). Cada jogo é criado do banco padrão e jogado pelo caminho do servidor, só com clubes da máquina: leilões da máquina antes de cada rodada, liga, copa, continental, Mundial, caixa, evolução de força, trocas de treinador e virada de temporada com promoção e rebaixamento. 1 semente por país (0 a 0), 10 temporadas seguidas cada; a semente da temporada é fixa, então o relatório se refaz igual.

*Força em campo*: média dos 11 mais fortes do plantel no início da temporada. *Rodadas de folha*: saldo no fim da temporada dividido pelos salários da última rodada. *Goleada*: um time com 5 gols ou mais. *Entre os 3 mais fortes*: o campeão da 1ª estava entre as 3 maiores forças em campo no início.

## Por país

| País | Gols por jogo | Mandante vence | Empate | Visitante vence | Goleadas | Expulsões por jogo | Lesões por jogo | Pontos do campeão (1ª) | Pontos do lanterna (1ª) | Campeão entre os 3 mais fortes | Mesmo campeão do ano anterior | Copa: campeão da 1ª | Continental: campeão do país | Vendas por temporada | Trocas de treinador por temporada |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| ARG | 2,8 | 47% | 23% | 30% | 3% | 0,21 | 0,06 | 82,6 (72%) | 23,0 (20%) | 80% | 44% | 100% | 30% | 24,0 | 22,2 |
| BRA | 2,8 | 47% | 23% | 30% | 3% | 0,20 | 0,06 | 83,2 (73%) | 19,2 (17%) | 90% | 56% | 100% | 30% | 16,8 | 24,6 |
| ENG | 2,8 | 47% | 23% | 30% | 3% | 0,21 | 0,06 | 85,5 (75%) | 16,9 (15%) | 100% | 56% | 100% | 40% | 20,7 | 22,1 |
| ESP | 2,8 | 47% | 23% | 30% | 3% | 0,20 | 0,06 | 81,6 (72%) | 20,0 (18%) | 90% | 0% | 90% | 20% | 17,0 | 23,2 |
| ITA | 2,8 | 47% | 23% | 30% | 3% | 0,21 | 0,06 | 84,3 (74%) | 21,5 (19%) | 80% | 44% | 100% | 10% | 18,7 | 23,1 |
| POR | 2,8 | 47% | 22% | 31% | 3% | 0,20 | 0,06 | 74,4 (73%) | 15,5 (15%) | 100% | 56% | 100% | 10% | 14,9 | 20,0 |
| Todos | 2,8 | 47% | 23% | 30% | 3% | 0,21 | 0,06 | 81,9 (73%) | 19,4 (17%) | 90% | 43% | 98% | 23% | 18,7 | 22,5 |

## Por temporada (todos os países)

| Temporada | Força em campo 1ª (média; menor a maior) | Força em campo 2ª | Saldo 1ª em rodadas de folha (mediana) | Clubes negativos 1ª | Saldo 2ª (mediana) | Clubes negativos 2ª | Estádio médio 1ª / 2ª | Gols por jogo | Vendas | Sem lance |
|---|---|---|---|---|---|---|---|---|---|---|
| 1 | 42,5 (35,1 a 49,6) | 26,6 (23,2 a 29,7) | 16,1 | 0% | 23,1 | 0% | 38.224 / 11.231 | 2,9 | 34,5 | 30,3 |
| 2 | 38,5 (26,2 a 48,5) | 24,5 (19,4 a 34,1) | 25,4 | 0% | 31,4 | 0% | 38.729 / 12.610 | 2,7 | 21,3 | 0,0 |
| 3 | 38,2 (27,6 a 48,4) | 22,6 (17,7 a 30,3) | 31,2 | 0% | 36,3 | 0% | 40.444 / 11.969 | 2,8 | 22,5 | 0,0 |
| 4 | 38,1 (26,8 a 48,9) | 22,7 (17,2 a 32,2) | 35,5 | 1% | 42,1 | 2% | 40.996 / 12.081 | 2,8 | 20,2 | 0,0 |
| 5 | 38,3 (25,6 a 49,8) | 22,6 (17,6 a 31,8) | 35,6 | 0% | 50,4 | 6% | 41.462 / 12.079 | 2,7 | 15,7 | 0,0 |
| 6 | 38,5 (25,7 a 49,9) | 22,6 (17,2 a 31,0) | 35,6 | 0% | 49,8 | 8% | 41.841 / 12.059 | 2,7 | 15,3 | 0,0 |
| 7 | 38,4 (25,9 a 50,0) | 22,7 (17,5 a 35,0) | 37,1 | 0% | 57,9 | 13% | 41.726 / 12.389 | 2,8 | 14,7 | 0,0 |
| 8 | 38,5 (26,5 a 50,0) | 22,6 (17,2 a 31,7) | 30,6 | 0% | 56,0 | 15% | 42.271 / 12.094 | 2,8 | 15,3 | 0,0 |
| 9 | 38,5 (25,2 a 50,0) | 23,1 (17,5 a 33,0) | 35,3 | 0% | 54,9 | 14% | 42.170 / 12.677 | 2,8 | 13,7 | 0,0 |
| 10 | 38,8 (27,4 a 50,0) | 22,6 (17,2 a 31,9) | 35,6 | 1% | 55,8 | 15% | 43.132 / 12.148 | 2,8 | 13,7 | 0,0 |

## Quem marca (liga, todos os países)

Gols da liga pela posição do autor; o pênalti fica à parte. *Artilheiro*: gols do maior artilheiro da 1ª na liga, média das temporadas. A máquina escolhe a formação a cada jogo (§7.2 dá os pesos por formação).

| País | Atacante | Meia | Defensor | Goleiro | Pênalti | Artilheiro da 1ª |
|---|---|---|---|---|---|---|
| ARG | 57% | 29% | 5% | 2 | 8% | 19,9 |
| BRA | 57% | 29% | 5% | 3 | 8% | 22,5 |
| ENG | 56% | 30% | 5% | 0 | 8% | 24,7 |
| ESP | 57% | 30% | 5% | 5 | 8% | 20,8 |
| ITA | 56% | 30% | 6% | 0 | 8% | 21,1 |
| POR | 57% | 29% | 5% | 1 | 8% | 19,9 |
| Todos | 57% | 30% | 5% | 11 | 8% | 21,5 |

## Quem sobe da 2ª (todos os países)

Clubes que subiram no fim da temporada anterior, na 1ª. *Caem de volta*: terminam entre os últimos, tantos quantos subiram (a zona de rebaixamento). *Força*: força em campo no início menos a média da 1ª.

| Temporada | Promovidos | Posição média | Caem de volta | Força em relação à média da 1ª |
|---|---|---|---|---|
| 2 | 18 | 18,6ª | 94% | -10,6 |
| 3 | 18 | 16,9ª | 61% | -9,2 |
| 4 | 18 | 17,4ª | 72% | -9,9 |
| 5 | 18 | 17,9ª | 67% | -10,0 |
| 6 | 18 | 17,6ª | 78% | -10,5 |
| 7 | 18 | 17,7ª | 83% | -10,3 |
| 8 | 18 | 16,7ª | 44% | -10,3 |
| 9 | 18 | 17,3ª | 61% | -10,4 |
| 10 | 18 | 15,7ª | 39% | -9,7 |
| Todas | 162 | 17,3ª | 67% | -10,1 |

## Caixa por temporada, por tipo (todos os países)

Média dos clubes da divisão no fim da temporada, em rodadas da folha do próprio clube (positivo entra, negativo sai). Uma temporada de liga com 20 clubes tem 38 rodadas.

| Temporada | Divisão | BILHETERIA | COMPRA | CUSTO_DO_JOGO | DESLOCAMENTO | ESTADIO | PATROCINIO | PREMIO | SALARIOS | VENDA | Saldo da temporada |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 1 | 1ª | 58,8 | -2,3 | -8,8 | -7,2 | -1,6 | 1,1 | 1,3 | -40,1 | 4,5 | 5,8 |
| 1 | 2ª | 66,3 | -0,2 | -10,0 | -8,9 | -0,5 | 1,0 | 0,0 | -40,6 | 4,5 | 11,7 |
| 2 | 1ª | 55,5 | -1,2 | -8,3 | -8,3 | -1,5 | 1,0 | 0,9 | -38,4 | 1,8 | 1,5 |
| 2 | 2ª | 65,4 | -0,9 | -9,8 | -9,3 | 0,0 | 0,9 | 0,0 | -38,3 | 3,9 | 12,0 |
| 3 | 1ª | 53,3 | -1,7 | -8,0 | -7,6 | -0,8 | 0,9 | 1,2 | -37,2 | 2,4 | 2,4 |
| 3 | 2ª | 64,5 | -0,9 | -9,7 | -9,8 | 0,0 | 1,0 | 0,0 | -37,8 | 3,5 | 10,9 |
| 4 | 1ª | 51,8 | -1,2 | -7,8 | -7,5 | -0,4 | 0,9 | 1,7 | -37,6 | 2,2 | 2,2 |
| 4 | 2ª | 64,2 | -1,6 | -9,6 | -9,9 | -0,2 | 0,9 | 0,4 | -37,0 | 2,7 | 10,0 |
| 5 | 1ª | 51,1 | -1,1 | -7,7 | -7,4 | -0,3 | 0,9 | 1,0 | -37,6 | 1,8 | 0,7 |
| 5 | 2ª | 63,9 | -1,1 | -9,6 | -10,1 | -0,1 | 1,0 | 0,4 | -37,1 | 2,0 | 9,3 |
| 6 | 1ª | 52,0 | -1,5 | -7,8 | -7,1 | -0,3 | 0,9 | 0,8 | -37,5 | 2,1 | 1,5 |
| 6 | 2ª | 63,7 | -1,5 | -9,6 | -10,0 | -0,1 | 1,0 | 0,0 | -37,1 | 1,3 | 7,6 |
| 7 | 1ª | 52,5 | -1,0 | -7,9 | -7,2 | -0,1 | 0,9 | 0,9 | -37,9 | 2,6 | 2,8 |
| 7 | 2ª | 64,9 | -2,1 | -9,7 | -10,3 | 0,0 | 1,0 | 0,0 | -37,0 | 0,6 | 7,4 |
| 8 | 1ª | 52,3 | -1,4 | -7,8 | -7,2 | -0,2 | 0,9 | 1,1 | -37,8 | 2,9 | 2,6 |
| 8 | 2ª | 61,9 | -2,3 | -9,3 | -10,1 | -0,1 | 0,9 | 0,0 | -36,5 | 0,2 | 4,8 |
| 9 | 1ª | 52,6 | -1,1 | -7,9 | -7,5 | -0,5 | 0,9 | 1,2 | -38,0 | 2,5 | 2,2 |
| 9 | 2ª | 63,8 | -1,3 | -9,6 | -10,0 | 0,0 | 0,9 | 0,0 | -37,3 | 0,6 | 7,2 |
| 10 | 1ª | 51,8 | -1,5 | -7,8 | -6,9 | -0,5 | 0,8 | 1,2 | -37,8 | 2,5 | 1,7 |
| 10 | 2ª | 63,8 | -1,0 | -9,6 | -10,1 | 0,0 | 1,0 | 0,0 | -36,6 | 0,3 | 7,7 |

Tempo de simulação: 979 s para 60 temporadas.
