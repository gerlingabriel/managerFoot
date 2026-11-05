# Simulação do jogo completo

Gerado por `RelatorioJogoCompletoIT` (ferramenta de desenvolvimento; SPEC §15, Etapa 6). Cada jogo é criado do banco padrão e jogado pelo caminho do servidor, só com clubes da máquina: leilões da máquina antes de cada rodada, liga, copa, continental, Mundial, caixa, evolução de força, trocas de treinador e virada de temporada com promoção e rebaixamento. 1 semente por país (0 a 0), 10 temporadas seguidas cada; a semente da temporada é fixa, então o relatório se refaz igual.

*Força em campo*: média dos 11 mais fortes do plantel no início da temporada. *Rodadas de folha*: saldo no fim da temporada dividido pelos salários da última rodada. *Goleada*: um time com 5 gols ou mais. *Entre os 3 mais fortes*: o campeão da 1ª estava entre as 3 maiores forças em campo no início.

## Por país

| País | Gols por jogo | Mandante vence | Empate | Visitante vence | Goleadas | Expulsões por jogo | Lesões por jogo | Pontos do campeão (1ª) | Pontos do lanterna (1ª) | Campeão entre os 3 mais fortes | Mesmo campeão do ano anterior | Copa: campeão da 1ª | Continental: campeão do país | Vendas por temporada | Trocas de treinador por temporada |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| ARG | 2,7 | 46% | 24% | 29% | 5% | 0,21 | 0,06 | 81,0 (71%) | 22,8 (20%) | 90% | 33% | 100% | 30% | 5,5 | 22,7 |
| BRA | 2,7 | 47% | 23% | 30% | 5% | 0,21 | 0,06 | 81,6 (72%) | 22,9 (20%) | 90% | 11% | 100% | 50% | 5,9 | 22,2 |
| ENG | 2,7 | 47% | 24% | 30% | 5% | 0,21 | 0,06 | 83,0 (73%) | 19,1 (17%) | 60% | 22% | 100% | 0% | 6,5 | 21,6 |
| ESP | 2,7 | 46% | 24% | 30% | 5% | 0,21 | 0,06 | 78,2 (69%) | 19,6 (17%) | 90% | 44% | 100% | 20% | 6,2 | 22,1 |
| ITA | 2,7 | 46% | 24% | 30% | 5% | 0,21 | 0,06 | 82,7 (73%) | 19,0 (17%) | 80% | 22% | 100% | 20% | 7,8 | 23,1 |
| POR | 2,7 | 46% | 24% | 29% | 5% | 0,19 | 0,06 | 74,9 (73%) | 17,8 (17%) | 90% | 33% | 100% | 10% | 4,7 | 19,8 |
| Todos | 2,7 | 47% | 24% | 30% | 5% | 0,21 | 0,06 | 80,2 (72%) | 20,2 (18%) | 83% | 28% | 100% | 22% | 6,1 | 21,9 |

## Por temporada (todos os países)

| Temporada | Força em campo 1ª (média; menor a maior) | Força em campo 2ª | Saldo 1ª em rodadas de folha (mediana) | Clubes negativos 1ª | Saldo 2ª (mediana) | Clubes negativos 2ª | Estádio médio 1ª / 2ª | Gols por jogo | Vendas | Sem lance |
|---|---|---|---|---|---|---|---|---|---|---|
| 1 | 42,5 (35,1 a 49,6) | 26,6 (23,2 a 29,7) | 34,1 | 0% | 39,6 | 0% | 40.604 / 11.231 | 2,7 | 21,3 | 19,0 |
| 2 | 38,5 (25,8 a 48,3) | 24,5 (19,6 a 33,5) | 67,7 | 0% | 60,4 | 0% | 40.550 / 12.614 | 2,6 | 9,2 | 0,0 |
| 3 | 38,0 (24,1 a 48,1) | 22,7 (18,1 a 30,9) | 86,9 | 0% | 91,1 | 0% | 41.701 / 11.948 | 2,7 | 10,0 | 0,0 |
| 4 | 37,9 (25,7 a 48,7) | 22,6 (17,2 a 32,8) | 120,7 | 0% | 92,5 | 1% | 41.793 / 12.068 | 2,7 | 6,7 | 0,0 |
| 5 | 37,9 (25,3 a 49,3) | 22,8 (17,9 a 32,0) | 145,5 | 0% | 120,4 | 1% | 41.868 / 12.407 | 2,6 | 5,2 | 0,0 |
| 6 | 38,1 (26,1 a 49,7) | 22,4 (17,1 a 33,1) | 162,6 | 0% | 133,0 | 6% | 42.506 / 12.001 | 2,6 | 3,0 | 0,0 |
| 7 | 37,7 (22,5 a 49,6) | 23,0 (17,7 a 34,1) | 192,1 | 0% | 155,2 | 4% | 42.170 / 12.737 | 2,7 | 1,5 | 0,0 |
| 8 | 38,2 (26,0 a 49,8) | 22,3 (17,2 a 31,0) | 216,4 | 0% | 168,9 | 6% | 43.310 / 11.801 | 2,7 | 1,0 | 0,0 |
| 9 | 38,1 (24,1 a 50,0) | 22,9 (17,8 a 33,0) | 226,5 | 0% | 198,3 | 8% | 42.913 / 12.586 | 2,6 | 2,5 | 0,0 |
| 10 | 38,7 (27,7 a 50,0) | 22,3 (17,3 a 30,4) | 254,2 | 0% | 201,1 | 7% | 44.252 / 11.573 | 2,6 | 0,7 | 0,0 |

## Quem sobe da 2ª (todos os países)

Clubes que subiram no fim da temporada anterior, na 1ª. *Caem de volta*: terminam entre os últimos, tantos quantos subiram (a zona de rebaixamento). *Força*: força em campo no início menos a média da 1ª.

| Temporada | Promovidos | Posição média | Caem de volta | Força em relação à média da 1ª |
|---|---|---|---|---|
| 2 | 18 | 18,6ª | 100% | -11,0 |
| 3 | 18 | 17,2ª | 50% | -10,1 |
| 4 | 18 | 18,1ª | 89% | -10,0 |
| 5 | 18 | 18,1ª | 89% | -10,6 |
| 6 | 18 | 16,5ª | 67% | -9,5 |
| 7 | 18 | 18,2ª | 83% | -11,6 |
| 8 | 18 | 17,1ª | 56% | -10,2 |
| 9 | 18 | 16,8ª | 56% | -10,4 |
| 10 | 18 | 16,0ª | 44% | -9,7 |
| Todas | 162 | 17,4ª | 70% | -10,4 |

## Caixa por temporada, por tipo (todos os países)

Média dos clubes da divisão no fim da temporada, em rodadas da folha do próprio clube (positivo entra, negativo sai). Uma temporada de liga com 20 clubes tem 38 rodadas.

| Temporada | Divisão | BILHETERIA | COMPRA | CUSTO_DO_JOGO | DESLOCAMENTO | ESTADIO | PATROCINIO | PREMIO | SALARIOS | VENDA | Saldo da temporada |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 1 | 1ª | 82,9 | -1,1 | -12,4 | -7,4 | -4,6 | 1,1 | 1,3 | -39,5 | 2,0 | 22,3 |
| 1 | 2ª | 86,1 | 0,0 | -12,9 | -11,8 | -0,3 | 1,0 | 0,0 | -40,4 | 3,9 | 25,5 |
| 2 | 1ª | 78,2 | -0,5 | -11,7 | -8,7 | -1,2 | 1,1 | 1,1 | -37,9 | 0,1 | 20,5 |
| 2 | 2ª | 84,3 | 0,0 | -12,6 | -12,1 | 0,0 | 0,9 | 0,0 | -38,2 | 2,8 | 25,0 |
| 3 | 1ª | 76,1 | -0,6 | -11,4 | -8,2 | -0,3 | 1,0 | 1,6 | -37,0 | 0,5 | 21,5 |
| 3 | 2ª | 83,0 | 0,0 | -12,5 | -12,7 | 0,0 | 1,0 | 0,0 | -37,8 | 2,5 | 23,5 |
| 4 | 1ª | 73,0 | -0,3 | -10,9 | -7,8 | -0,3 | 1,0 | 1,3 | -36,8 | 0,0 | 19,0 |
| 4 | 2ª | 83,4 | -0,1 | -12,5 | -13,0 | 0,0 | 1,0 | 0,2 | -37,6 | 2,1 | 23,5 |
| 5 | 1ª | 72,5 | -0,3 | -10,9 | -8,0 | -0,2 | 1,0 | 1,1 | -37,2 | 0,0 | 18,0 |
| 5 | 2ª | 83,1 | -0,1 | -12,5 | -12,8 | -0,2 | 0,9 | 0,0 | -37,4 | 1,6 | 22,6 |
| 6 | 1ª | 73,3 | -0,2 | -11,0 | -7,5 | -0,3 | 0,9 | 1,2 | -37,1 | 0,0 | 19,3 |
| 6 | 2ª | 83,9 | 0,0 | -12,6 | -13,7 | 0,0 | 1,0 | 0,0 | -37,5 | 1,0 | 22,1 |
| 7 | 1ª | 73,7 | -0,1 | -11,1 | -8,4 | -0,3 | 1,0 | 1,0 | -37,1 | 0,0 | 18,9 |
| 7 | 2ª | 82,9 | -0,1 | -12,4 | -12,8 | 0,0 | 0,9 | 0,0 | -37,4 | 0,4 | 21,6 |
| 8 | 1ª | 71,8 | -0,1 | -10,8 | -7,4 | -0,2 | 0,9 | 0,9 | -37,1 | 0,0 | 18,1 |
| 8 | 2ª | 82,8 | 0,0 | -12,4 | -13,7 | 0,0 | 1,0 | 0,1 | -37,2 | 0,3 | 20,9 |
| 9 | 1ª | 72,6 | -0,1 | -10,9 | -7,6 | -0,3 | 0,9 | 1,2 | -37,1 | 0,0 | 18,8 |
| 9 | 2ª | 83,9 | -0,1 | -12,6 | -13,3 | 0,0 | 1,0 | 0,0 | -37,5 | 0,8 | 22,2 |
| 10 | 1ª | 71,5 | -0,1 | -10,7 | -7,1 | -0,3 | 0,9 | 1,0 | -37,2 | 0,1 | 18,1 |
| 10 | 2ª | 84,7 | 0,0 | -12,7 | -13,6 | 0,0 | 1,0 | 0,0 | -37,0 | 0,1 | 22,4 |

Tempo de simulação: 609 s para 60 temporadas.
