# Simulação do jogo completo

Gerado por `RelatorioJogoCompletoIT` (ferramenta de desenvolvimento; SPEC §15, Etapa 6). Cada jogo é criado do banco padrão e jogado pelo caminho do servidor, só com clubes da máquina: leilões da máquina antes de cada rodada, liga, copa, continental, Mundial, caixa, evolução de força, trocas de treinador e virada de temporada com promoção e rebaixamento. 1 semente por país (0 a 0), 5 temporadas seguidas cada; a semente da temporada é fixa, então o relatório se refaz igual.

*Força em campo*: média dos 11 mais fortes do plantel no início da temporada. *Rodadas de folha*: saldo no fim da temporada dividido pelos salários da última rodada. *Goleada*: um time com 5 gols ou mais. *Entre os 3 mais fortes*: o campeão da 1ª estava entre as 3 maiores forças em campo no início.

## Por país

| País | Gols por jogo | Mandante vence | Empate | Visitante vence | Goleadas | Expulsões por jogo | Lesões por jogo | Pontos do campeão (1ª) | Pontos do lanterna (1ª) | Campeão entre os 3 mais fortes | Mesmo campeão do ano anterior | Copa: campeão da 1ª | Continental: campeão do país | Vendas por temporada | Trocas de treinador por temporada |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| ARG | 2,9 | 48% | 22% | 30% | 7% | 0,21 | 0,06 | 80,2 (70%) | 19,4 (17%) | 60% | 0% | 100% | 20% | 30,8 | 22,2 |
| BRA | 3,0 | 47% | 22% | 31% | 8% | 0,20 | 0,06 | 80,4 (71%) | 19,6 (17%) | 80% | 0% | 100% | 40% | 28,6 | 21,6 |
| ENG | 3,0 | 48% | 22% | 30% | 7% | 0,22 | 0,06 | 85,8 (75%) | 19,2 (17%) | 40% | 25% | 100% | 20% | 32,2 | 20,6 |
| ESP | 2,9 | 48% | 23% | 30% | 7% | 0,20 | 0,06 | 81,8 (72%) | 19,6 (17%) | 100% | 25% | 100% | 0% | 27,4 | 21,6 |
| ITA | 2,9 | 47% | 23% | 30% | 7% | 0,21 | 0,06 | 84,8 (74%) | 21,8 (19%) | 60% | 25% | 100% | 40% | 27,8 | 20,6 |
| POR | 2,9 | 47% | 24% | 29% | 6% | 0,19 | 0,06 | 73,6 (72%) | 19,6 (19%) | 80% | 0% | 100% | 0% | 23,8 | 18,2 |
| Todos | 2,9 | 48% | 23% | 30% | 7% | 0,21 | 0,06 | 81,1 (72%) | 19,9 (18%) | 70% | 13% | 100% | 20% | 28,4 | 20,8 |

## Por temporada (todos os países)

| Temporada | Força em campo 1ª (média; menor a maior) | Força em campo 2ª | Saldo 1ª em rodadas de folha (mediana) | Clubes negativos 1ª | Saldo 2ª (mediana) | Clubes negativos 2ª | Estádio médio 1ª / 2ª | Gols por jogo | Vendas | Sem lance |
|---|---|---|---|---|---|---|---|---|---|---|
| 1 | 42,5 (35,1 a 49,6) | 26,6 (23,2 a 29,7) | 16,5 | 0% | 20,4 | 0% | 38.520 / 11.272 | 3,1 | 43,3 | 27,8 |
| 2 | 38,4 (26,7 a 48,0) | 24,4 (18,8 a 34,1) | 21,6 | 0% | 24,9 | 0% | 38.888 / 12.604 | 2,9 | 33,3 | 0,0 |
| 3 | 38,1 (26,5 a 48,0) | 22,5 (17,5 a 30,0) | 25,0 | 1% | 25,3 | 3% | 40.631 / 11.788 | 2,9 | 26,5 | 0,0 |
| 4 | 37,9 (24,9 a 49,1) | 22,7 (17,3 a 33,1) | 35,1 | 2% | 23,8 | 7% | 40.646 / 12.347 | 3,0 | 22,3 | 0,0 |
| 5 | 38,2 (25,8 a 49,6) | 22,6 (17,4 a 31,4) | 29,4 | 2% | 27,9 | 18% | 41.777 / 12.007 | 2,9 | 16,7 | 0,0 |

## Quem marca (liga, todos os países)

Gols da liga pela posição do autor; o pênalti fica à parte. *Artilheiro*: gols do maior artilheiro da 1ª na liga, média das temporadas. A máquina escolhe a formação a cada jogo (§7.2 dá os pesos por formação).

| País | Atacante | Meia | Defensor | Goleiro | Pênalti | Artilheiro da 1ª |
|---|---|---|---|---|---|---|
| ARG | 57% | 30% | 6% | 1 | 8% | 20,6 |
| BRA | 58% | 29% | 5% | 1 | 7% | 21,6 |
| ENG | 58% | 29% | 6% | 0 | 7% | 27,2 |
| ESP | 58% | 29% | 6% | 0 | 8% | 22,0 |
| ITA | 57% | 30% | 5% | 1 | 7% | 20,2 |
| POR | 58% | 30% | 5% | 1 | 7% | 18,8 |
| Todos | 57% | 30% | 6% | 4 | 7% | 21,7 |

## Quem sobe da 2ª (todos os países)

Clubes que subiram no fim da temporada anterior, na 1ª. *Caem de volta*: terminam entre os últimos, tantos quantos subiram (a zona de rebaixamento). *Força*: força em campo no início menos a média da 1ª.

| Temporada | Promovidos | Posição média | Caem de volta | Força em relação à média da 1ª |
|---|---|---|---|---|
| 2 | 18 | 18,6ª | 94% | -10,4 |
| 3 | 18 | 17,9ª | 78% | -9,3 |
| 4 | 18 | 18,4ª | 94% | -10,6 |
| 5 | 18 | 18,2ª | 83% | -10,2 |
| Todas | 72 | 18,3ª | 88% | -10,1 |

## Caixa por temporada, por tipo (todos os países)

Média dos clubes da divisão no fim da temporada, em rodadas da folha do próprio clube (positivo entra, negativo sai). Uma temporada de liga com 20 clubes tem 38 rodadas.

| Temporada | Divisão | BILHETERIA | COMPRA | CUSTO_DO_JOGO | DESLOCAMENTO | ESTADIO | PATROCINIO | PREMIO | SALARIOS | VENDA | Saldo da temporada |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 1 | 1ª | 61,4 | -2,9 | -9,2 | -7,5 | -2,0 | 1,2 | 1,2 | -39,8 | 5,2 | 7,4 |
| 1 | 2ª | 64,6 | -0,9 | -9,7 | -12,1 | -0,3 | 1,0 | 0,0 | -40,7 | 7,0 | 9,0 |
| 2 | 1ª | 57,7 | -1,9 | -8,7 | -8,6 | -1,7 | 1,0 | 1,2 | -38,4 | 2,8 | 3,4 |
| 2 | 2ª | 63,6 | -2,0 | -9,5 | -12,9 | 0,0 | 0,9 | 0,0 | -38,4 | 6,8 | 8,5 |
| 3 | 1ª | 54,8 | -1,6 | -8,2 | -7,7 | -0,6 | 1,0 | 1,2 | -37,0 | 2,2 | 4,0 |
| 3 | 2ª | 61,7 | -1,9 | -9,3 | -13,5 | 0,0 | 1,0 | 0,0 | -37,6 | 5,2 | 5,6 |
| 4 | 1ª | 54,4 | -1,4 | -8,2 | -7,9 | -0,5 | 1,0 | 1,4 | -37,5 | 2,8 | 4,1 |
| 4 | 2ª | 61,7 | -2,5 | -9,3 | -13,7 | -0,1 | 1,0 | 0,0 | -37,0 | 2,8 | 3,0 |
| 5 | 1ª | 54,6 | -1,4 | -8,2 | -7,8 | -0,6 | 0,9 | 1,0 | -37,8 | 2,6 | 3,5 |
| 5 | 2ª | 60,5 | -2,2 | -9,1 | -13,8 | 0,0 | 1,0 | 0,0 | -36,8 | 1,1 | 0,8 |

Tempo de simulação: 338 s para 30 temporadas.
