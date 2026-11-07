# Simulação do jogo completo

Gerado por `RelatorioJogoCompletoIT` (ferramenta de desenvolvimento; SPEC §15, Etapa 6). Cada jogo é criado do banco padrão e jogado pelo caminho do servidor, só com clubes da máquina: leilões da máquina antes de cada rodada, liga, copa, continental, Mundial, caixa, evolução de força, trocas de treinador e virada de temporada com promoção e rebaixamento. 2 sementes por país (0 a 1), 5 temporadas seguidas cada; a semente da temporada é fixa, então o relatório se refaz igual.

*Força em campo*: média dos 11 mais fortes do plantel no início da temporada. *Rodadas de folha*: saldo no fim da temporada dividido pelos salários da última rodada. *Goleada*: um time com 5 gols ou mais. *Entre os 3 mais fortes*: o campeão da 1ª estava entre as 3 maiores forças em campo no início.

## Por país

| País | Gols por jogo | Mandante vence | Empate | Visitante vence | Goleadas | Expulsões por jogo | Lesões por jogo | Pontos do campeão (1ª) | Pontos do lanterna (1ª) | Campeão entre os 3 mais fortes | Mesmo campeão do ano anterior | Copa: campeão da 1ª | Continental: campeão do país | Vendas por temporada | Trocas de treinador por temporada |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| ARG | 2,9 | 48% | 22% | 30% | 4% | 0,14 | 0,12 | 79,8 (70%) | 23,7 (21%) | 50% | 13% | 100% | 60% | 29,5 | 21,8 |
| BRA | 2,9 | 48% | 22% | 30% | 4% | 0,13 | 0,11 | 84,6 (74%) | 21,2 (19%) | 50% | 0% | 90% | 50% | 30,9 | 23,7 |
| ENG | 2,9 | 48% | 23% | 29% | 4% | 0,13 | 0,12 | 79,5 (70%) | 20,3 (18%) | 50% | 0% | 100% | 20% | 30,7 | 21,3 |
| ESP | 2,9 | 49% | 23% | 29% | 4% | 0,13 | 0,12 | 80,9 (71%) | 24,6 (22%) | 80% | 25% | 100% | 30% | 31,7 | 21,7 |
| ITA | 2,9 | 47% | 23% | 30% | 4% | 0,13 | 0,11 | 81,7 (72%) | 24,2 (21%) | 90% | 25% | 90% | 30% | 31,5 | 22,8 |
| POR | 2,9 | 48% | 23% | 29% | 4% | 0,14 | 0,12 | 74,5 (73%) | 19,4 (19%) | 80% | 38% | 100% | 20% | 22,7 | 17,9 |
| Todos | 2,9 | 48% | 23% | 29% | 4% | 0,13 | 0,12 | 80,2 (72%) | 22,2 (20%) | 67% | 17% | 97% | 35% | 29,5 | 21,5 |

## Por temporada (todos os países)

| Temporada | Força em campo 1ª (média; menor a maior) | Força em campo 2ª | Saldo 1ª em rodadas de folha (mediana) | Clubes negativos 1ª | Saldo 2ª (mediana) | Clubes negativos 2ª | Estádio médio 1ª / 2ª | Gols por jogo | Vendas | Sem lance |
|---|---|---|---|---|---|---|---|---|---|---|
| 1 | 42,5 (35,1 a 49,6) | 26,6 (23,1 a 29,7) | 15,2 | 0% | 17,2 | 0% | 38.696 / 11.189 | 2,8 | 40,1 | 36,8 |
| 2 | 39,8 (28,0 a 49,3) | 26,0 (20,9 a 38,3) | 19,5 | 0% | 19,0 | 0% | 41.361 / 13.347 | 2,9 | 31,6 | 0,0 |
| 3 | 40,5 (29,4 a 49,9) | 25,3 (19,5 a 34,4) | 24,0 | 0% | 18,7 | 0% | 46.566 / 12.734 | 3,0 | 29,8 | 0,0 |
| 4 | 40,9 (29,4 a 50,0) | 25,7 (19,3 a 35,1) | 26,7 | 0% | 20,0 | 0% | 48.743 / 13.652 | 2,9 | 24,0 | 0,0 |
| 5 | 41,1 (29,2 a 50,0) | 26,0 (19,5 a 34,9) | 26,7 | 1% | 19,4 | 3% | 50.324 / 14.119 | 2,9 | 22,0 | 0,1 |

## Quem marca (liga, todos os países)

Gols da liga pela posição do autor; o pênalti fica à parte. *Artilheiro*: gols do maior artilheiro da 1ª na liga, média das temporadas. A máquina escolhe a formação a cada jogo (§7.2 dá os pesos por formação).

| País | Atacante | Meia | Defensor | Goleiro | Pênalti | Artilheiro da 1ª |
|---|---|---|---|---|---|---|
| ARG | 53% | 31% | 8% | 1 | 8% | 19,1 |
| BRA | 52% | 31% | 8% | 1 | 8% | 21,7 |
| ENG | 52% | 32% | 8% | 1 | 8% | 21,3 |
| ESP | 53% | 31% | 8% | 2 | 8% | 22,3 |
| ITA | 52% | 31% | 8% | 2 | 8% | 19,9 |
| POR | 53% | 31% | 8% | 0 | 8% | 19,6 |
| Todos | 52% | 31% | 8% | 7 | 8% | 20,7 |

## Quem sobe da 2ª (todos os países)

Clubes que subiram no fim da temporada anterior, na 1ª. *Caem de volta*: terminam entre os últimos, tantos quantos subiram (a zona de rebaixamento). *Força*: força em campo no início menos a média da 1ª.

| Temporada | Promovidos | Posição média | Caem de volta | Força em relação à média da 1ª |
|---|---|---|---|---|
| 2 | 36 | 18,1ª | 81% | -11,0 |
| 3 | 36 | 16,7ª | 53% | -10,0 |
| 4 | 36 | 17,0ª | 53% | -10,9 |
| 5 | 36 | 17,1ª | 58% | -11,0 |
| Todas | 144 | 17,2ª | 61% | -10,7 |

## Caixa por temporada, por tipo (todos os países)

Média dos clubes da divisão no fim da temporada, em rodadas da folha do próprio clube (positivo entra, negativo sai). Uma temporada de liga com 20 clubes tem 38 rodadas.

| Temporada | Divisão | BILHETERIA | COMPRA | CUSTO_DO_JOGO | DESLOCAMENTO | ESTADIO | PATROCINIO | PREMIO | SALARIOS | VENDA | Saldo da temporada |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 1 | 1ª | 54,9 | -1,8 | -8,2 | -6,2 | -1,7 | 1,0 | 0,9 | -38,0 | 5,0 | 5,8 |
| 1 | 2ª | 57,8 | -3,4 | -8,7 | -7,4 | 0,0 | 0,8 | 0,0 | -36,9 | 4,7 | 7,0 |
| 2 | 1ª | 54,8 | -0,2 | -8,2 | -6,8 | -4,2 | 0,8 | 1,0 | -37,8 | 3,9 | 3,3 |
| 2 | 2ª | 58,9 | -4,8 | -8,8 | -7,6 | -1,6 | 0,7 | 0,0 | -37,0 | 4,3 | 4,0 |
| 3 | 1ª | 54,5 | -0,3 | -8,2 | -6,3 | -3,5 | 0,8 | 1,2 | -37,3 | 4,0 | 4,9 |
| 3 | 2ª | 57,2 | -4,9 | -8,6 | -7,6 | -1,7 | 0,7 | 0,2 | -36,6 | 3,7 | 2,4 |
| 4 | 1ª | 53,9 | -0,2 | -8,1 | -6,0 | -2,6 | 0,7 | 0,8 | -37,8 | 3,4 | 4,1 |
| 4 | 2ª | 58,1 | -4,4 | -8,7 | -7,7 | -1,7 | 0,7 | 0,2 | -36,5 | 2,5 | 2,5 |
| 5 | 1ª | 53,0 | -0,3 | -8,0 | -6,0 | -1,7 | 0,7 | 0,9 | -37,8 | 3,3 | 4,2 |
| 5 | 2ª | 57,6 | -3,8 | -8,6 | -7,7 | -1,7 | 0,7 | 0,1 | -36,6 | 2,0 | 2,0 |

Tempo de simulação: 881 s para 60 temporadas.
