# Simulação do jogo completo

Gerado por `RelatorioJogoCompletoIT` (ferramenta de desenvolvimento; SPEC §15, Etapa 6). Cada jogo é criado do banco padrão e jogado pelo caminho do servidor, só com clubes da máquina: leilões da máquina antes de cada rodada, liga, copa, continental, Mundial, caixa, evolução de força, trocas de treinador e virada de temporada com promoção e rebaixamento. 1 semente por país (0 a 0), 5 temporadas seguidas cada; a semente da temporada é fixa, então o relatório se refaz igual.

*Força em campo*: média dos 11 mais fortes do plantel no início da temporada. *Rodadas de folha*: saldo no fim da temporada dividido pelos salários da última rodada. *Goleada*: um time com 5 gols ou mais. *Entre os 3 mais fortes*: o campeão da 1ª estava entre as 3 maiores forças em campo no início.

## Por país

| País | Gols por jogo | Mandante vence | Empate | Visitante vence | Goleadas | Expulsões por jogo | Lesões por jogo | Pontos do campeão (1ª) | Pontos do lanterna (1ª) | Campeão entre os 3 mais fortes | Mesmo campeão do ano anterior | Copa: campeão da 1ª | Continental: campeão do país | Vendas por temporada | Trocas de treinador por temporada |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| ARG | 2,8 | 48% | 23% | 29% | 3% | 0,21 | 0,06 | 82,0 (72%) | 25,0 (22%) | 80% | 50% | 100% | 40% | 27,8 | 21,2 |
| BRA | 2,8 | 48% | 22% | 30% | 3% | 0,20 | 0,06 | 85,0 (75%) | 20,8 (18%) | 100% | 25% | 100% | 40% | 21,2 | 22,4 |
| ENG | 2,8 | 48% | 23% | 29% | 3% | 0,21 | 0,06 | 80,6 (71%) | 19,8 (17%) | 100% | 75% | 100% | 0% | 25,4 | 21,4 |
| ESP | 2,8 | 48% | 22% | 29% | 3% | 0,21 | 0,05 | 79,2 (69%) | 21,8 (19%) | 80% | 50% | 100% | 0% | 23,8 | 19,2 |
| ITA | 2,8 | 48% | 23% | 29% | 3% | 0,20 | 0,06 | 82,4 (72%) | 22,6 (20%) | 80% | 25% | 100% | 0% | 23,4 | 21,4 |
| POR | 2,8 | 48% | 23% | 29% | 3% | 0,20 | 0,06 | 74,4 (73%) | 19,8 (19%) | 80% | 25% | 100% | 20% | 17,8 | 19,2 |
| Todos | 2,8 | 48% | 23% | 29% | 3% | 0,20 | 0,06 | 80,6 (72%) | 21,6 (19%) | 87% | 42% | 100% | 17% | 23,2 | 20,8 |

## Por temporada (todos os países)

| Temporada | Força em campo 1ª (média; menor a maior) | Força em campo 2ª | Saldo 1ª em rodadas de folha (mediana) | Clubes negativos 1ª | Saldo 2ª (mediana) | Clubes negativos 2ª | Estádio médio 1ª / 2ª | Gols por jogo | Vendas | Sem lance |
|---|---|---|---|---|---|---|---|---|---|---|
| 1 | 42,5 (35,1 a 49,6) | 26,6 (23,2 a 29,7) | 15,8 | 0% | 23,1 | 0% | 38.266 / 11.231 | 2,9 | 34,5 | 30,5 |
| 2 | 38,5 (26,1 a 48,6) | 24,5 (19,4 a 34,3) | 25,8 | 0% | 36,3 | 0% | 38.562 / 12.614 | 2,7 | 21,0 | 0,0 |
| 3 | 38,3 (27,1 a 48,6) | 22,6 (17,9 a 29,4) | 27,8 | 0% | 38,1 | 0% | 40.505 / 11.845 | 2,8 | 21,3 | 0,0 |
| 4 | 38,1 (26,4 a 49,4) | 22,7 (17,2 a 33,0) | 36,8 | 0% | 45,3 | 2% | 40.798 / 12.175 | 2,8 | 20,5 | 0,0 |
| 5 | 38,3 (26,2 a 49,7) | 22,6 (17,8 a 31,6) | 29,5 | 0% | 48,4 | 5% | 41.401 / 11.936 | 2,7 | 18,8 | 0,0 |

## Quem marca (liga, todos os países)

Gols da liga pela posição do autor; o pênalti fica à parte. *Artilheiro*: gols do maior artilheiro da 1ª na liga, média das temporadas. A máquina escolhe a formação a cada jogo (§7.2 dá os pesos por formação).

| País | Atacante | Meia | Defensor | Goleiro | Pênalti | Artilheiro da 1ª |
|---|---|---|---|---|---|---|
| ARG | 57% | 29% | 6% | 0 | 8% | 20,4 |
| BRA | 57% | 30% | 6% | 0 | 8% | 22,8 |
| ENG | 57% | 30% | 5% | 3 | 8% | 22,4 |
| ESP | 57% | 29% | 6% | 0 | 8% | 18,8 |
| ITA | 57% | 30% | 5% | 0 | 8% | 18,2 |
| POR | 58% | 30% | 5% | 1 | 8% | 18,6 |
| Todos | 57% | 29% | 5% | 4 | 8% | 20,2 |

## Quem sobe da 2ª (todos os países)

Clubes que subiram no fim da temporada anterior, na 1ª. *Caem de volta*: terminam entre os últimos, tantos quantos subiram (a zona de rebaixamento). *Força*: força em campo no início menos a média da 1ª.

| Temporada | Promovidos | Posição média | Caem de volta | Força em relação à média da 1ª |
|---|---|---|---|---|
| 2 | 18 | 18,6ª | 100% | -10,7 |
| 3 | 18 | 17,7ª | 67% | -9,4 |
| 4 | 18 | 18,3ª | 83% | -9,9 |
| 5 | 18 | 17,7ª | 78% | -9,6 |
| Todas | 72 | 18,1ª | 82% | -9,9 |

## Caixa por temporada, por tipo (todos os países)

Média dos clubes da divisão no fim da temporada, em rodadas da folha do próprio clube (positivo entra, negativo sai). Uma temporada de liga com 20 clubes tem 38 rodadas.

| Temporada | Divisão | BILHETERIA | COMPRA | CUSTO_DO_JOGO | DESLOCAMENTO | ESTADIO | PATROCINIO | PREMIO | SALARIOS | VENDA | Saldo da temporada |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 1 | 1ª | 58,7 | -2,3 | -8,8 | -7,2 | -1,5 | 1,1 | 1,3 | -40,1 | 4,6 | 5,9 |
| 1 | 2ª | 66,2 | -0,3 | -9,9 | -8,9 | -0,3 | 1,0 | 0,0 | -40,5 | 4,5 | 11,8 |
| 2 | 1ª | 55,7 | -1,2 | -8,4 | -8,3 | -1,7 | 1,0 | 0,9 | -38,3 | 1,7 | 1,6 |
| 2 | 2ª | 66,0 | -0,7 | -9,9 | -9,3 | 0,0 | 0,9 | 0,0 | -38,5 | 3,9 | 12,5 |
| 3 | 1ª | 53,2 | -1,5 | -8,0 | -7,5 | -0,5 | 0,9 | 1,1 | -37,1 | 2,1 | 2,7 |
| 3 | 2ª | 64,8 | -0,7 | -9,7 | -9,9 | 0,0 | 1,0 | 0,0 | -37,7 | 3,6 | 11,3 |
| 4 | 1ª | 52,4 | -1,4 | -7,9 | -7,5 | -0,5 | 0,9 | 1,6 | -37,5 | 2,1 | 2,1 |
| 4 | 2ª | 63,3 | -1,4 | -9,5 | -9,9 | 0,0 | 0,9 | 0,0 | -37,1 | 3,0 | 9,4 |
| 5 | 1ª | 51,1 | -1,1 | -7,7 | -7,2 | -0,2 | 0,9 | 1,1 | -37,9 | 2,6 | 1,6 |
| 5 | 2ª | 63,3 | -2,1 | -9,5 | -9,9 | -0,3 | 0,9 | 0,0 | -36,8 | 1,7 | 7,3 |

Tempo de simulação: 518 s para 30 temporadas.
