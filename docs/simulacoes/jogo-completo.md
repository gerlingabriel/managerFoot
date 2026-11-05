# Simulação do jogo completo

Gerado por `RelatorioJogoCompletoIT` (ferramenta de desenvolvimento; SPEC §15, Etapa 6). Cada jogo é criado do banco padrão e jogado pelo caminho do servidor, só com clubes da máquina: leilões da máquina antes de cada rodada, liga, copa, continental, Mundial, caixa, evolução de força, trocas de treinador e virada de temporada com promoção e rebaixamento. 1 semente por país (0 a 0), 5 temporadas seguidas cada; a semente da temporada é fixa, então o relatório se refaz igual.

*Força em campo*: média dos 11 mais fortes do plantel no início da temporada. *Rodadas de folha*: saldo no fim da temporada dividido pelos salários da última rodada. *Goleada*: um time com 5 gols ou mais. *Entre os 3 mais fortes*: o campeão da 1ª estava entre as 3 maiores forças em campo no início.

## Por país

| País | Gols por jogo | Mandante vence | Empate | Visitante vence | Goleadas | Expulsões por jogo | Lesões por jogo | Pontos do campeão (1ª) | Pontos do lanterna (1ª) | Campeão entre os 3 mais fortes | Mesmo campeão do ano anterior | Copa: campeão da 1ª | Continental: campeão do país | Vendas por temporada | Trocas de treinador por temporada |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| ARG | 3,0 | 48% | 23% | 29% | 7% | 0,21 | 0,06 | 81,6 (72%) | 20,2 (18%) | 100% | 25% | 100% | 20% | 26,6 | 20,6 |
| BRA | 2,9 | 48% | 23% | 30% | 7% | 0,21 | 0,06 | 80,6 (71%) | 24,0 (21%) | 80% | 25% | 100% | 60% | 26,4 | 19,8 |
| ENG | 2,9 | 48% | 23% | 29% | 7% | 0,20 | 0,06 | 83,6 (73%) | 21,2 (19%) | 80% | 25% | 100% | 0% | 28,2 | 22,2 |
| ESP | 2,9 | 48% | 22% | 30% | 6% | 0,20 | 0,06 | 78,6 (69%) | 21,0 (18%) | 20% | 0% | 100% | 0% | 27,8 | 20,2 |
| ITA | 2,9 | 48% | 22% | 30% | 7% | 0,20 | 0,06 | 79,8 (70%) | 20,2 (18%) | 80% | 0% | 100% | 20% | 27,6 | 22,0 |
| POR | 3,0 | 49% | 22% | 30% | 7% | 0,18 | 0,06 | 73,8 (72%) | 19,4 (19%) | 100% | 25% | 100% | 0% | 22,0 | 17,2 |
| Todos | 2,9 | 48% | 22% | 30% | 7% | 0,20 | 0,06 | 79,7 (71%) | 21,0 (19%) | 77% | 17% | 100% | 17% | 26,4 | 20,3 |

## Por temporada (todos os países)

| Temporada | Força em campo 1ª (média; menor a maior) | Força em campo 2ª | Saldo 1ª em rodadas de folha (mediana) | Clubes negativos 1ª | Saldo 2ª (mediana) | Clubes negativos 2ª | Estádio médio 1ª / 2ª | Gols por jogo | Vendas | Sem lance |
|---|---|---|---|---|---|---|---|---|---|---|
| 1 | 42,5 (35,1 a 49,6) | 26,6 (23,2 a 29,7) | 16,2 | 0% | 19,8 | 0% | 38.044 / 11.231 | 3,1 | 42,8 | 29,8 |
| 2 | 38,5 (26,9 a 48,5) | 24,5 (19,4 a 33,7) | 25,1 | 0% | 28,0 | 0% | 38.514 / 12.662 | 2,8 | 28,5 | 0,0 |
| 3 | 38,2 (27,0 a 48,5) | 22,3 (17,5 a 29,4) | 29,4 | 2% | 29,0 | 6% | 40.627 / 11.625 | 2,9 | 26,5 | 0,0 |
| 4 | 38,0 (25,6 a 48,8) | 22,8 (17,3 a 34,0) | 36,0 | 3% | 26,7 | 9% | 40.359 / 12.553 | 2,9 | 18,0 | 0,0 |
| 5 | 38,3 (28,0 a 49,4) | 22,5 (17,3 a 30,6) | 33,2 | 1% | 28,7 | 19% | 41.578 / 12.023 | 2,9 | 16,3 | 0,0 |

## Quem marca (liga, todos os países)

Gols da liga pela posição do autor; o pênalti fica à parte. *Artilheiro*: gols do maior artilheiro da 1ª na liga, média das temporadas. A máquina escolhe a formação a cada jogo (§7.2 dá os pesos por formação).

| País | Atacante | Meia | Defensor | Goleiro | Pênalti | Artilheiro da 1ª |
|---|---|---|---|---|---|---|
| ARG | 58% | 29% | 5% | 0 | 7% | 21,6 |
| BRA | 57% | 29% | 6% | 0 | 8% | 24,6 |
| ENG | 57% | 30% | 5% | 0 | 8% | 25,8 |
| ESP | 57% | 29% | 6% | 0 | 8% | 22,4 |
| ITA | 57% | 30% | 6% | 0 | 8% | 18,8 |
| POR | 58% | 29% | 5% | 2 | 8% | 20,8 |
| Todos | 57% | 29% | 6% | 2 | 8% | 22,3 |

## Quem sobe da 2ª (todos os países)

Clubes que subiram no fim da temporada anterior, na 1ª. *Caem de volta*: terminam entre os últimos, tantos quantos subiram (a zona de rebaixamento). *Força*: força em campo no início menos a média da 1ª.

| Temporada | Promovidos | Posição média | Caem de volta | Força em relação à média da 1ª |
|---|---|---|---|---|
| 2 | 18 | 18,6ª | 100% | -10,4 |
| 3 | 18 | 16,8ª | 56% | -9,2 |
| 4 | 18 | 18,3ª | 89% | -10,4 |
| 5 | 18 | 18,2ª | 89% | -9,4 |
| Todas | 72 | 18,0ª | 83% | -9,9 |

## Caixa por temporada, por tipo (todos os países)

Média dos clubes da divisão no fim da temporada, em rodadas da folha do próprio clube (positivo entra, negativo sai). Uma temporada de liga com 20 clubes tem 38 rodadas.

| Temporada | Divisão | BILHETERIA | COMPRA | CUSTO_DO_JOGO | DESLOCAMENTO | ESTADIO | PATROCINIO | PREMIO | SALARIOS | VENDA | Saldo da temporada |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 1 | 1ª | 61,5 | -2,7 | -9,2 | -7,6 | -1,5 | 1,2 | 1,2 | -39,8 | 4,9 | 8,0 |
| 1 | 2ª | 64,1 | -1,1 | -9,6 | -12,1 | -0,1 | 1,0 | 0,0 | -40,5 | 7,2 | 8,9 |
| 2 | 1ª | 57,6 | -1,8 | -8,6 | -8,6 | -2,0 | 1,0 | 1,2 | -38,1 | 2,1 | 2,9 |
| 2 | 2ª | 64,2 | -1,3 | -9,6 | -12,7 | 0,0 | 0,9 | 0,0 | -38,6 | 6,1 | 9,0 |
| 3 | 1ª | 54,9 | -1,9 | -8,2 | -7,5 | -0,7 | 0,9 | 1,4 | -36,9 | 2,4 | 4,5 |
| 3 | 2ª | 62,1 | -1,2 | -9,3 | -13,7 | -0,1 | 1,0 | 0,0 | -37,5 | 4,7 | 5,9 |
| 4 | 1ª | 53,8 | -1,0 | -8,1 | -7,8 | -0,5 | 0,9 | 1,4 | -37,3 | 1,8 | 3,3 |
| 4 | 2ª | 62,3 | -1,6 | -9,3 | -13,4 | -0,3 | 0,9 | 0,0 | -37,1 | 2,8 | 4,3 |
| 5 | 1ª | 53,5 | -1,4 | -8,0 | -7,3 | -0,8 | 0,9 | 1,2 | -37,6 | 2,6 | 3,2 |
| 5 | 2ª | 61,2 | -2,0 | -9,2 | -13,8 | -0,3 | 1,0 | 0,0 | -36,7 | 1,0 | 1,2 |

Tempo de simulação: 376 s para 30 temporadas.
