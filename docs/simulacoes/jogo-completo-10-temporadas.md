# Simulação do jogo completo

Gerado por `RelatorioJogoCompletoIT` (ferramenta de desenvolvimento; SPEC §15, Etapa 6). Cada jogo é criado do banco padrão e jogado pelo caminho do servidor, só com clubes da máquina: leilões da máquina antes de cada rodada, liga, copa, continental, Mundial, caixa, evolução de força, trocas de treinador e virada de temporada com promoção e rebaixamento. 1 semente por país (0 a 0), 10 temporadas seguidas cada; a semente da temporada é fixa, então o relatório se refaz igual.

*Força em campo*: média dos 11 mais fortes do plantel no início da temporada. *Rodadas de folha*: saldo no fim da temporada dividido pelos salários da última rodada. *Goleada*: um time com 5 gols ou mais. *Entre os 3 mais fortes*: o campeão da 1ª estava entre as 3 maiores forças em campo no início.

## Por país

| País | Gols por jogo | Mandante vence | Empate | Visitante vence | Goleadas | Expulsões por jogo | Lesões por jogo | Pontos do campeão (1ª) | Pontos do lanterna (1ª) | Campeão entre os 3 mais fortes | Mesmo campeão do ano anterior | Copa: campeão da 1ª | Continental: campeão do país | Vendas por temporada | Trocas de treinador por temporada |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| ARG | 2,8 | 47% | 23% | 30% | 4% | 0,21 | 0,06 | 83,3 (73%) | 22,0 (19%) | 80% | 44% | 100% | 40% | 17,3 | 22,5 |
| BRA | 2,8 | 48% | 22% | 30% | 3% | 0,21 | 0,06 | 84,5 (74%) | 19,3 (17%) | 70% | 0% | 90% | 30% | 14,9 | 22,8 |
| ENG | 2,8 | 47% | 23% | 30% | 3% | 0,21 | 0,06 | 85,0 (75%) | 20,1 (18%) | 90% | 44% | 90% | 20% | 18,5 | 23,4 |
| ESP | 2,8 | 48% | 23% | 29% | 4% | 0,20 | 0,06 | 83,0 (73%) | 20,0 (18%) | 90% | 44% | 100% | 30% | 15,4 | 22,1 |
| ITA | 2,9 | 47% | 22% | 31% | 3% | 0,21 | 0,06 | 85,7 (75%) | 22,8 (20%) | 70% | 11% | 100% | 0% | 17,9 | 21,7 |
| POR | 2,9 | 47% | 22% | 31% | 4% | 0,19 | 0,06 | 78,4 (77%) | 19,4 (19%) | 90% | 22% | 100% | 20% | 15,2 | 19,2 |
| Todos | 2,8 | 47% | 22% | 30% | 4% | 0,21 | 0,06 | 83,3 (74%) | 20,6 (18%) | 82% | 28% | 97% | 23% | 16,5 | 22,0 |

## Por temporada (todos os países)

| Temporada | Força em campo 1ª (média; menor a maior) | Força em campo 2ª | Saldo 1ª em rodadas de folha (mediana) | Clubes negativos 1ª | Saldo 2ª (mediana) | Clubes negativos 2ª | Estádio médio 1ª / 2ª | Gols por jogo | Vendas | Sem lance |
|---|---|---|---|---|---|---|---|---|---|---|
| 1 | 42,5 (35,1 a 49,6) | 26,6 (23,2 a 29,7) | 16,8 | 0% | 18,0 | 0% | 38.437 / 11.189 | 2,9 | 34,0 | 30,0 |
| 2 | 38,5 (26,2 a 49,1) | 24,7 (18,8 a 34,8) | 28,7 | 0% | 22,5 | 0% | 38.994 / 12.829 | 2,8 | 23,5 | 0,0 |
| 3 | 38,6 (28,1 a 49,9) | 22,8 (17,9 a 30,1) | 33,3 | 0% | 22,8 | 0% | 41.618 / 11.576 | 2,9 | 21,8 | 0,0 |
| 4 | 38,4 (27,2 a 49,9) | 23,0 (17,1 a 33,5) | 41,7 | 0% | 28,2 | 0% | 41.867 / 12.347 | 2,9 | 19,7 | 0,0 |
| 5 | 38,4 (26,2 a 50,0) | 23,2 (17,8 a 32,9) | 50,4 | 0% | 27,0 | 7% | 42.037 / 12.652 | 2,7 | 15,8 | 0,0 |
| 6 | 38,4 (26,6 a 50,0) | 23,0 (17,1 a 31,8) | 55,1 | 0% | 28,2 | 10% | 42.736 / 12.202 | 2,8 | 13,3 | 0,0 |
| 7 | 38,4 (26,8 a 50,0) | 23,3 (17,8 a 34,6) | 57,3 | 0% | 38,3 | 10% | 42.546 / 12.680 | 2,9 | 10,0 | 0,0 |
| 8 | 38,4 (26,7 a 50,0) | 23,0 (17,1 a 33,1) | 61,2 | 0% | 35,0 | 15% | 43.038 / 12.503 | 2,8 | 10,2 | 0,0 |
| 9 | 38,4 (26,3 a 50,0) | 23,3 (17,7 a 33,5) | 70,0 | 1% | 37,1 | 12% | 42.911 / 13.034 | 2,8 | 9,7 | 0,0 |
| 10 | 39,0 (28,5 a 50,0) | 22,7 (17,3 a 31,2) | 73,8 | 2% | 38,1 | 14% | 43.939 / 12.415 | 2,8 | 7,3 | 0,0 |

## Quem marca (liga, todos os países)

Gols da liga pela posição do autor; o pênalti fica à parte. *Artilheiro*: gols do maior artilheiro da 1ª na liga, média das temporadas. A máquina escolhe a formação a cada jogo (§7.2 dá os pesos por formação).

| País | Atacante | Meia | Defensor | Goleiro | Pênalti | Artilheiro da 1ª |
|---|---|---|---|---|---|---|
| ARG | 51% | 32% | 8% | 0 | 8% | 20,7 |
| BRA | 51% | 32% | 9% | 1 | 8% | 19,9 |
| ENG | 51% | 32% | 9% | 4 | 8% | 19,8 |
| ESP | 51% | 32% | 9% | 1 | 8% | 20,4 |
| ITA | 51% | 32% | 9% | 1 | 8% | 20,3 |
| POR | 52% | 32% | 8% | 4 | 8% | 17,7 |
| Todos | 51% | 32% | 9% | 11 | 8% | 19,8 |

## Quem sobe da 2ª (todos os países)

Clubes que subiram no fim da temporada anterior, na 1ª. *Caem de volta*: terminam entre os últimos, tantos quantos subiram (a zona de rebaixamento). *Força*: força em campo no início menos a média da 1ª.

| Temporada | Promovidos | Posição média | Caem de volta | Força em relação à média da 1ª |
|---|---|---|---|---|
| 2 | 18 | 18,6ª | 94% | -10,6 |
| 3 | 18 | 16,7ª | 50% | -9,1 |
| 4 | 18 | 17,6ª | 67% | -10,0 |
| 5 | 18 | 17,7ª | 72% | -10,0 |
| 6 | 18 | 16,4ª | 61% | -9,6 |
| 7 | 18 | 17,6ª | 67% | -9,8 |
| 8 | 18 | 16,7ª | 61% | -9,7 |
| 9 | 18 | 17,2ª | 56% | -10,3 |
| 10 | 18 | 15,8ª | 28% | -9,3 |
| Todas | 162 | 17,1ª | 62% | -9,8 |

## Caixa por temporada, por tipo (todos os países)

Média dos clubes da divisão no fim da temporada, em rodadas da folha do próprio clube (positivo entra, negativo sai). Uma temporada de liga com 20 clubes tem 38 rodadas.

| Temporada | Divisão | BILHETERIA | COMPRA | CUSTO_DO_JOGO | DESLOCAMENTO | ESTADIO | PATROCINIO | PREMIO | SALARIOS | VENDA | Saldo da temporada |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 1 | 1ª | 60,0 | -1,2 | -9,0 | -7,2 | -1,6 | 1,1 | 1,2 | -40,5 | 4,6 | 7,4 |
| 1 | 2ª | 62,9 | -3,4 | -9,4 | -8,6 | 0,0 | 1,0 | 0,0 | -39,4 | 4,3 | 7,3 |
| 2 | 1ª | 58,0 | -0,1 | -8,7 | -8,2 | -2,2 | 1,0 | 0,9 | -38,6 | 2,7 | 4,7 |
| 2 | 2ª | 61,5 | -3,5 | -9,2 | -8,9 | 0,0 | 0,8 | 0,0 | -37,7 | 3,9 | 6,8 |
| 3 | 1ª | 56,7 | -0,1 | -8,5 | -7,5 | -0,7 | 0,9 | 1,3 | -37,5 | 1,9 | 6,5 |
| 3 | 2ª | 59,4 | -3,1 | -8,9 | -9,3 | -0,3 | 0,9 | 0,0 | -37,4 | 4,2 | 5,4 |
| 4 | 1ª | 55,4 | -0,1 | -8,3 | -7,4 | -1,7 | 0,9 | 1,0 | -38,3 | 2,4 | 4,0 |
| 4 | 2ª | 60,6 | -3,3 | -9,1 | -9,5 | -0,1 | 0,9 | 0,6 | -37,0 | 2,9 | 6,0 |
| 5 | 1ª | 54,8 | -0,1 | -8,2 | -7,6 | -0,4 | 0,9 | 1,1 | -38,2 | 2,2 | 4,5 |
| 5 | 2ª | 59,3 | -2,7 | -8,9 | -9,5 | 0,0 | 0,9 | 0,0 | -36,8 | 2,0 | 4,2 |
| 6 | 1ª | 54,9 | -0,1 | -8,2 | -7,3 | -0,3 | 0,9 | 0,9 | -37,8 | 1,8 | 4,7 |
| 6 | 2ª | 60,1 | -2,7 | -9,0 | -9,6 | -0,1 | 0,9 | 0,3 | -36,9 | 1,6 | 4,4 |
| 7 | 1ª | 56,1 | -0,2 | -8,4 | -7,5 | -0,4 | 0,9 | 1,1 | -38,0 | 1,7 | 5,3 |
| 7 | 2ª | 61,4 | -1,8 | -9,2 | -9,7 | 0,0 | 0,9 | 0,0 | -37,3 | 0,8 | 5,1 |
| 8 | 1ª | 54,1 | -0,1 | -8,1 | -7,2 | -0,5 | 0,9 | 1,0 | -37,7 | 1,7 | 4,0 |
| 8 | 2ª | 59,6 | -2,0 | -8,9 | -9,7 | 0,0 | 0,9 | 0,1 | -37,0 | 0,7 | 3,7 |
| 9 | 1ª | 55,4 | 0,0 | -8,3 | -7,5 | -0,7 | 0,9 | 0,9 | -38,0 | 1,6 | 4,3 |
| 9 | 2ª | 60,5 | -1,9 | -9,1 | -9,6 | 0,0 | 0,9 | 0,0 | -37,1 | 0,8 | 4,5 |
| 10 | 1ª | 54,8 | -0,1 | -8,2 | -7,0 | -0,4 | 0,9 | 1,0 | -37,7 | 1,2 | 4,6 |
| 10 | 2ª | 60,9 | -1,6 | -9,1 | -9,8 | -0,1 | 0,9 | 0,0 | -36,8 | 0,6 | 5,0 |

Tempo de simulação: 1331 s para 60 temporadas.
