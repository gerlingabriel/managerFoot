# Simulação do jogo completo

Gerado por `RelatorioJogoCompletoIT` (ferramenta de desenvolvimento; SPEC §15, Etapa 6). Cada jogo é criado do banco padrão e jogado pelo caminho do servidor, só com clubes da máquina: leilões da máquina antes de cada rodada, liga, copa, continental, Mundial, caixa, evolução de força, trocas de treinador e virada de temporada com promoção e rebaixamento. 1 semente por país (0 a 0), 5 temporadas seguidas cada; a semente da temporada é fixa, então o relatório se refaz igual.

*Força em campo*: média dos 11 mais fortes do plantel no início da temporada. *Rodadas de folha*: saldo no fim da temporada dividido pelos salários da última rodada. *Goleada*: um time com 5 gols ou mais. *Entre os 3 mais fortes*: o campeão da 1ª estava entre as 3 maiores forças em campo no início.

## Por país

| País | Gols por jogo | Mandante vence | Empate | Visitante vence | Goleadas | Expulsões por jogo | Lesões por jogo | Pontos do campeão (1ª) | Pontos do lanterna (1ª) | Campeão entre os 3 mais fortes | Mesmo campeão do ano anterior | Copa: campeão da 1ª | Continental: campeão do país | Vendas por temporada | Trocas de treinador por temporada |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| ARG | 2,7 | 47% | 24% | 30% | 5% | 0,21 | 0,06 | 81,4 (71%) | 20,8 (18%) | 60% | 0% | 100% | 60% | 29,4 | 23,4 |
| BRA | 2,7 | 47% | 23% | 30% | 5% | 0,21 | 0,06 | 79,2 (69%) | 21,6 (19%) | 60% | 25% | 100% | 40% | 24,4 | 22,2 |
| ENG | 2,7 | 47% | 24% | 30% | 5% | 0,21 | 0,06 | 77,6 (68%) | 21,4 (19%) | 40% | 0% | 100% | 0% | 25,6 | 21,2 |
| ESP | 2,6 | 46% | 24% | 30% | 5% | 0,20 | 0,05 | 79,2 (69%) | 19,4 (17%) | 100% | 0% | 80% | 40% | 27,0 | 20,0 |
| ITA | 2,6 | 47% | 24% | 29% | 5% | 0,19 | 0,06 | 77,8 (68%) | 19,0 (17%) | 60% | 0% | 100% | 0% | 30,0 | 20,4 |
| POR | 2,6 | 47% | 24% | 29% | 4% | 0,19 | 0,06 | 70,4 (69%) | 21,0 (21%) | 40% | 0% | 100% | 0% | 24,0 | 20,0 |
| Todos | 2,7 | 47% | 24% | 30% | 5% | 0,20 | 0,06 | 77,6 (69%) | 20,5 (18%) | 60% | 4% | 97% | 23% | 26,7 | 21,2 |

## Por temporada (todos os países)

| Temporada | Força em campo 1ª (média; menor a maior) | Força em campo 2ª | Saldo 1ª em rodadas de folha (mediana) | Clubes negativos 1ª | Saldo 2ª (mediana) | Clubes negativos 2ª | Estádio médio 1ª / 2ª | Gols por jogo | Vendas | Sem lance |
|---|---|---|---|---|---|---|---|---|---|---|
| 1 | 42,5 (35,1 a 49,6) | 26,6 (23,2 a 29,7) | 18,0 | 0% | 21,7 | 0% | 38.020 / 11.189 | 2,7 | 42,5 | 29,3 |
| 2 | 38,5 (27,5 a 48,2) | 24,4 (19,6 a 33,1) | 27,1 | 0% | 27,6 | 0% | 38.771 / 12.585 | 2,6 | 28,0 | 0,0 |
| 3 | 38,0 (26,1 a 48,5) | 22,5 (18,1 a 30,5) | 30,7 | 3% | 29,8 | 4% | 40.144 / 11.772 | 2,7 | 24,0 | 0,0 |
| 4 | 37,9 (25,5 a 49,1) | 22,7 (17,3 a 32,1) | 35,2 | 1% | 28,9 | 13% | 40.363 / 12.158 | 2,7 | 20,7 | 0,0 |
| 5 | 38,1 (26,0 a 48,7) | 22,8 (17,5 a 31,5) | 38,1 | 1% | 31,1 | 13% | 40.852 / 12.202 | 2,6 | 18,5 | 0,0 |

## Quem marca (liga, todos os países)

Gols da liga pela posição do autor; o pênalti fica à parte. *Artilheiro*: gols do maior artilheiro da 1ª na liga, média das temporadas. A máquina escolhe a formação a cada jogo (§7.2 dá os pesos por formação).

| País | Atacante | Meia | Defensor | Goleiro | Pênalti | Artilheiro da 1ª |
|---|---|---|---|---|---|---|
| ARG | 56% | 30% | 6% | 0 | 8% | 21,6 |
| BRA | 56% | 30% | 6% | 1 | 8% | 19,2 |
| ENG | 56% | 30% | 5% | 0 | 8% | 20,2 |
| ESP | 56% | 30% | 6% | 1 | 9% | 19,6 |
| ITA | 56% | 30% | 6% | 0 | 8% | 19,8 |
| POR | 57% | 30% | 5% | 4 | 8% | 18,6 |
| Todos | 56% | 30% | 6% | 6 | 8% | 19,8 |

## Quem sobe da 2ª (todos os países)

Clubes que subiram no fim da temporada anterior, na 1ª. *Caem de volta*: terminam entre os últimos, tantos quantos subiram (a zona de rebaixamento). *Força*: força em campo no início menos a média da 1ª.

| Temporada | Promovidos | Posição média | Caem de volta | Força em relação à média da 1ª |
|---|---|---|---|---|
| 2 | 18 | 18,5ª | 89% | -10,4 |
| 3 | 18 | 17,7ª | 67% | -9,1 |
| 4 | 18 | 17,9ª | 89% | -10,5 |
| 5 | 18 | 17,9ª | 78% | -10,2 |
| Todas | 72 | 18,0ª | 81% | -10,0 |

## Caixa por temporada, por tipo (todos os países)

Média dos clubes da divisão no fim da temporada, em rodadas da folha do próprio clube (positivo entra, negativo sai). Uma temporada de liga com 20 clubes tem 38 rodadas.

| Temporada | Divisão | BILHETERIA | COMPRA | CUSTO_DO_JOGO | DESLOCAMENTO | ESTADIO | PATROCINIO | PREMIO | SALARIOS | VENDA | Saldo da temporada |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 1 | 1ª | 61,5 | -3,0 | -9,2 | -7,6 | -1,5 | 1,2 | 1,5 | -39,8 | 4,9 | 8,1 |
| 1 | 2ª | 65,1 | -0,4 | -9,8 | -12,2 | 0,0 | 1,0 | 0,0 | -41,0 | 7,2 | 10,0 |
| 2 | 1ª | 57,7 | -1,6 | -8,7 | -8,6 | -2,2 | 1,0 | 1,2 | -38,1 | 2,0 | 2,8 |
| 2 | 2ª | 64,2 | -1,4 | -9,6 | -12,7 | 0,0 | 0,9 | 0,0 | -38,6 | 6,0 | 8,8 |
| 3 | 1ª | 54,3 | -1,5 | -8,1 | -7,7 | -0,5 | 0,9 | 1,4 | -37,0 | 2,1 | 3,8 |
| 3 | 2ª | 62,5 | -1,4 | -9,4 | -13,5 | 0,0 | 1,0 | 0,0 | -37,2 | 4,4 | 6,4 |
| 4 | 1ª | 53,8 | -1,1 | -8,1 | -7,8 | -0,5 | 1,0 | 1,1 | -37,5 | 2,5 | 3,3 |
| 4 | 2ª | 62,0 | -2,4 | -9,3 | -13,6 | -0,1 | 1,0 | 0,4 | -36,7 | 2,5 | 3,7 |
| 5 | 1ª | 53,9 | -1,5 | -8,1 | -7,8 | -0,6 | 1,0 | 1,5 | -37,8 | 2,4 | 2,9 |
| 5 | 2ª | 61,4 | -1,7 | -9,2 | -13,6 | 0,0 | 1,0 | 0,0 | -37,1 | 1,9 | 2,7 |

Tempo de simulação: 260 s para 30 temporadas.
