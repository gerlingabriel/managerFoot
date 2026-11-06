# Simulação do jogo completo

Gerado por `RelatorioJogoCompletoIT` (ferramenta de desenvolvimento; SPEC §15, Etapa 6). Cada jogo é criado do banco padrão e jogado pelo caminho do servidor, só com clubes da máquina: leilões da máquina antes de cada rodada, liga, copa, continental, Mundial, caixa, evolução de força, trocas de treinador e virada de temporada com promoção e rebaixamento. 1 semente por país (0 a 0), 5 temporadas seguidas cada; a semente da temporada é fixa, então o relatório se refaz igual.

*Força em campo*: média dos 11 mais fortes do plantel no início da temporada. *Rodadas de folha*: saldo no fim da temporada dividido pelos salários da última rodada. *Goleada*: um time com 5 gols ou mais. *Entre os 3 mais fortes*: o campeão da 1ª estava entre as 3 maiores forças em campo no início.

## Por país

| País | Gols por jogo | Mandante vence | Empate | Visitante vence | Goleadas | Expulsões por jogo | Lesões por jogo | Pontos do campeão (1ª) | Pontos do lanterna (1ª) | Campeão entre os 3 mais fortes | Mesmo campeão do ano anterior | Copa: campeão da 1ª | Continental: campeão do país | Vendas por temporada | Trocas de treinador por temporada |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| ARG | 2,8 | 48% | 22% | 30% | 3% | 0,21 | 0,06 | 81,6 (72%) | 22,0 (19%) | 80% | 50% | 100% | 80% | 29,0 | 21,4 |
| BRA | 2,8 | 48% | 22% | 30% | 3% | 0,20 | 0,06 | 85,6 (75%) | 17,0 (15%) | 100% | 25% | 100% | 20% | 27,0 | 23,6 |
| ENG | 2,8 | 48% | 22% | 30% | 4% | 0,21 | 0,06 | 84,2 (74%) | 22,0 (19%) | 100% | 25% | 100% | 40% | 27,8 | 22,6 |
| ESP | 2,7 | 47% | 24% | 29% | 3% | 0,20 | 0,06 | 78,0 (68%) | 23,0 (20%) | 60% | 0% | 100% | 0% | 28,8 | 21,2 |
| ITA | 2,8 | 48% | 22% | 30% | 3% | 0,21 | 0,06 | 84,4 (74%) | 19,4 (17%) | 80% | 25% | 100% | 20% | 28,6 | 19,8 |
| POR | 2,8 | 47% | 22% | 30% | 3% | 0,19 | 0,06 | 76,8 (75%) | 17,0 (17%) | 100% | 50% | 100% | 40% | 25,2 | 18,2 |
| Todos | 2,8 | 48% | 22% | 30% | 3% | 0,20 | 0,06 | 81,8 (73%) | 20,1 (18%) | 87% | 29% | 100% | 33% | 27,7 | 21,1 |

## Por temporada (todos os países)

| Temporada | Força em campo 1ª (média; menor a maior) | Força em campo 2ª | Saldo 1ª em rodadas de folha (mediana) | Clubes negativos 1ª | Saldo 2ª (mediana) | Clubes negativos 2ª | Estádio médio 1ª / 2ª | Gols por jogo | Vendas | Sem lance |
|---|---|---|---|---|---|---|---|---|---|---|
| 1 | 42,5 (35,1 a 49,6) | 26,6 (23,2 a 29,7) | 16,3 | 0% | 19,4 | 0% | 38.349 / 11.189 | 2,9 | 43,2 | 28,2 |
| 2 | 38,3 (26,7 a 48,3) | 24,4 (18,9 a 34,5) | 26,6 | 0% | 24,2 | 0% | 39.018 / 12.710 | 2,7 | 31,3 | 0,0 |
| 3 | 38,0 (26,0 a 48,2) | 22,7 (17,7 a 29,8) | 28,8 | 2% | 25,8 | 4% | 40.673 / 12.131 | 2,8 | 27,0 | 0,0 |
| 4 | 38,1 (25,6 a 49,4) | 22,6 (17,3 a 31,2) | 33,2 | 3% | 26,1 | 5% | 41.076 / 12.139 | 2,8 | 22,3 | 0,0 |
| 5 | 38,3 (25,1 a 49,6) | 22,8 (17,5 a 32,2) | 36,1 | 3% | 25,5 | 15% | 41.308 / 12.287 | 2,7 | 14,8 | 0,0 |

## Quem marca (liga, todos os países)

Gols da liga pela posição do autor; o pênalti fica à parte. *Artilheiro*: gols do maior artilheiro da 1ª na liga, média das temporadas. A máquina escolhe a formação a cada jogo (§7.2 dá os pesos por formação).

| País | Atacante | Meia | Defensor | Goleiro | Pênalti | Artilheiro da 1ª |
|---|---|---|---|---|---|---|
| ARG | 57% | 29% | 6% | 3 | 8% | 18,6 |
| BRA | 57% | 30% | 6% | 0 | 8% | 20,0 |
| ENG | 57% | 30% | 5% | 1 | 8% | 25,0 |
| ESP | 57% | 30% | 6% | 0 | 8% | 17,2 |
| ITA | 57% | 30% | 6% | 0 | 8% | 20,6 |
| POR | 58% | 30% | 5% | 2 | 8% | 18,4 |
| Todos | 57% | 30% | 6% | 6 | 8% | 20,0 |

## Quem sobe da 2ª (todos os países)

Clubes que subiram no fim da temporada anterior, na 1ª. *Caem de volta*: terminam entre os últimos, tantos quantos subiram (a zona de rebaixamento). *Força*: força em campo no início menos a média da 1ª.

| Temporada | Promovidos | Posição média | Caem de volta | Força em relação à média da 1ª |
|---|---|---|---|---|
| 2 | 18 | 18,4ª | 89% | -10,2 |
| 3 | 18 | 17,0ª | 67% | -9,2 |
| 4 | 18 | 18,3ª | 89% | -10,5 |
| 5 | 18 | 18,3ª | 89% | -10,8 |
| Todas | 72 | 18,0ª | 83% | -10,2 |

## Caixa por temporada, por tipo (todos os países)

Média dos clubes da divisão no fim da temporada, em rodadas da folha do próprio clube (positivo entra, negativo sai). Uma temporada de liga com 20 clubes tem 38 rodadas.

| Temporada | Divisão | BILHETERIA | COMPRA | CUSTO_DO_JOGO | DESLOCAMENTO | ESTADIO | PATROCINIO | PREMIO | SALARIOS | VENDA | Saldo da temporada |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 1 | 1ª | 61,5 | -3,0 | -9,2 | -7,6 | -1,8 | 1,2 | 1,3 | -39,9 | 5,3 | 7,7 |
| 1 | 2ª | 64,3 | -0,6 | -9,7 | -12,1 | -0,5 | 1,0 | 0,0 | -40,6 | 6,9 | 8,9 |
| 2 | 1ª | 58,0 | -1,2 | -8,7 | -8,7 | -2,4 | 1,0 | 1,4 | -38,4 | 2,5 | 3,6 |
| 2 | 2ª | 62,9 | -2,5 | -9,4 | -12,8 | -0,1 | 0,9 | 0,0 | -38,3 | 6,6 | 7,3 |
| 3 | 1ª | 55,1 | -1,4 | -8,3 | -8,1 | -0,7 | 1,0 | 1,6 | -37,1 | 2,5 | 4,6 |
| 3 | 2ª | 61,4 | -2,2 | -9,2 | -13,4 | 0,0 | 1,0 | 0,0 | -37,3 | 4,9 | 5,2 |
| 4 | 1ª | 53,7 | -1,2 | -8,1 | -7,7 | -0,4 | 0,9 | 1,2 | -37,4 | 2,3 | 3,4 |
| 4 | 2ª | 62,6 | -2,2 | -9,4 | -13,7 | 0,0 | 1,0 | 0,1 | -36,9 | 3,4 | 4,8 |
| 5 | 1ª | 54,0 | -1,2 | -8,1 | -7,9 | -0,3 | 0,9 | 1,4 | -37,5 | 2,0 | 3,5 |
| 5 | 2ª | 61,0 | -1,8 | -9,2 | -13,6 | -0,1 | 1,0 | 0,0 | -36,9 | 1,2 | 1,6 |

Tempo de simulação: 360 s para 30 temporadas.
