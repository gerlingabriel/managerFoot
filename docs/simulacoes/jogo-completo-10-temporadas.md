# Simulação do jogo completo

Gerado por `RelatorioJogoCompletoIT` (ferramenta de desenvolvimento; SPEC §15, Etapa 6). Cada jogo é criado do banco padrão e jogado pelo caminho do servidor, só com clubes da máquina: leilões da máquina antes de cada rodada, liga, copa, continental, Mundial, caixa, evolução de força, trocas de treinador e virada de temporada com promoção e rebaixamento. 1 semente por país (0 a 0), 10 temporadas seguidas cada; a semente da temporada é fixa, então o relatório se refaz igual.

*Força em campo*: média dos 11 mais fortes do plantel no início da temporada. *Rodadas de folha*: saldo no fim da temporada dividido pelos salários da última rodada. *Goleada*: um time com 5 gols ou mais. *Entre os 3 mais fortes*: o campeão da 1ª estava entre as 3 maiores forças em campo no início.

## Por país

| País | Gols por jogo | Mandante vence | Empate | Visitante vence | Goleadas | Expulsões por jogo | Lesões por jogo | Pontos do campeão (1ª) | Pontos do lanterna (1ª) | Campeão entre os 3 mais fortes | Mesmo campeão do ano anterior | Copa: campeão da 1ª | Continental: campeão do país | Vendas por temporada | Trocas de treinador por temporada |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| ARG | 2,9 | 49% | 21% | 30% | 3% | 0,21 | 0,06 | 82,9 (73%) | 26,3 (23%) | 50% | 0% | 90% | 60% | 21,9 | 22,8 |
| BRA | 2,9 | 48% | 21% | 30% | 4% | 0,21 | 0,06 | 81,2 (71%) | 21,0 (18%) | 50% | 22% | 100% | 60% | 26,4 | 23,4 |
| ENG | 2,9 | 48% | 22% | 30% | 4% | 0,21 | 0,06 | 80,4 (71%) | 22,9 (20%) | 40% | 0% | 100% | 0% | 23,4 | 22,7 |
| ESP | 2,9 | 48% | 22% | 30% | 4% | 0,21 | 0,06 | 81,2 (71%) | 23,0 (20%) | 60% | 11% | 100% | 20% | 24,1 | 23,7 |
| ITA | 2,9 | 47% | 22% | 31% | 4% | 0,21 | 0,06 | 84,1 (74%) | 23,8 (21%) | 50% | 33% | 100% | 20% | 23,5 | 22,9 |
| POR | 2,9 | 47% | 22% | 31% | 4% | 0,19 | 0,06 | 72,6 (71%) | 21,4 (21%) | 60% | 11% | 100% | 10% | 19,0 | 18,8 |
| Todos | 2,9 | 48% | 22% | 30% | 4% | 0,21 | 0,06 | 80,4 (72%) | 23,1 (21%) | 52% | 13% | 98% | 28% | 23,1 | 22,4 |

## Por temporada (todos os países)

| Temporada | Força em campo 1ª (média; menor a maior) | Força em campo 2ª | Saldo 1ª em rodadas de folha (mediana) | Clubes negativos 1ª | Saldo 2ª (mediana) | Clubes negativos 2ª | Estádio médio 1ª / 2ª | Gols por jogo | Vendas | Sem lance |
|---|---|---|---|---|---|---|---|---|---|---|
| 1 | 42,5 (35,1 a 49,6) | 26,6 (23,2 a 29,7) | 15,0 | 0% | 16,7 | 0% | 38.895 / 11.189 | 2,8 | 41,5 | 36,0 |
| 2 | 40,1 (27,8 a 49,3) | 26,1 (21,3 a 36,3) | 20,4 | 0% | 19,4 | 0% | 42.622 / 13.057 | 2,9 | 31,2 | 0,0 |
| 3 | 40,8 (29,6 a 49,8) | 25,6 (19,8 a 33,2) | 24,6 | 0% | 19,7 | 0% | 46.883 / 12.950 | 3,0 | 22,8 | 0,0 |
| 4 | 41,1 (29,8 a 50,0) | 25,8 (19,2 a 35,3) | 25,6 | 1% | 18,7 | 1% | 49.770 / 13.370 | 3,0 | 23,5 | 0,0 |
| 5 | 41,4 (29,8 a 50,0) | 26,2 (19,5 a 36,0) | 27,4 | 3% | 19,7 | 3% | 50.551 / 15.016 | 2,8 | 22,5 | 0,0 |
| 6 | 41,8 (29,9 a 50,0) | 26,2 (19,1 a 33,9) | 27,7 | 3% | 18,1 | 5% | 53.226 / 14.358 | 2,8 | 17,8 | 0,0 |
| 7 | 41,9 (29,9 a 50,0) | 26,5 (19,5 a 36,9) | 32,4 | 2% | 19,7 | 5% | 53.298 / 15.908 | 2,9 | 20,8 | 0,0 |
| 8 | 42,0 (30,2 a 50,0) | 26,5 (19,4 a 35,7) | 31,1 | 3% | 21,0 | 14% | 54.699 / 15.562 | 2,9 | 17,8 | 0,0 |
| 9 | 42,1 (30,0 a 50,0) | 26,8 (19,8 a 35,6) | 32,6 | 1% | 23,6 | 8% | 55.322 / 15.866 | 2,9 | 17,5 | 0,0 |
| 10 | 42,4 (30,0 a 50,0) | 26,5 (19,2 a 35,0) | 35,4 | 3% | 21,2 | 14% | 55.935 / 16.493 | 2,8 | 15,0 | 0,0 |

## Quem marca (liga, todos os países)

Gols da liga pela posição do autor; o pênalti fica à parte. *Artilheiro*: gols do maior artilheiro da 1ª na liga, média das temporadas. A máquina escolhe a formação a cada jogo (§7.2 dá os pesos por formação).

| País | Atacante | Meia | Defensor | Goleiro | Pênalti | Artilheiro da 1ª |
|---|---|---|---|---|---|---|
| ARG | 51% | 32% | 9% | 1 | 8% | 21,2 |
| BRA | 52% | 32% | 9% | 0 | 8% | 21,6 |
| ENG | 52% | 31% | 9% | 2 | 8% | 22,8 |
| ESP | 52% | 31% | 9% | 1 | 8% | 22,0 |
| ITA | 52% | 32% | 9% | 1 | 8% | 19,2 |
| POR | 53% | 31% | 8% | 3 | 8% | 20,3 |
| Todos | 52% | 32% | 9% | 8 | 8% | 21,2 |

## Quem sobe da 2ª (todos os países)

Clubes que subiram no fim da temporada anterior, na 1ª. *Caem de volta*: terminam entre os últimos, tantos quantos subiram (a zona de rebaixamento). *Força*: força em campo no início menos a média da 1ª.

| Temporada | Promovidos | Posição média | Caem de volta | Força em relação à média da 1ª |
|---|---|---|---|---|
| 2 | 18 | 18,6ª | 100% | -11,1 |
| 3 | 18 | 17,4ª | 61% | -10,8 |
| 4 | 18 | 16,9ª | 50% | -11,1 |
| 5 | 18 | 17,7ª | 72% | -11,5 |
| 6 | 18 | 16,9ª | 56% | -11,4 |
| 7 | 18 | 17,7ª | 61% | -11,6 |
| 8 | 18 | 16,5ª | 50% | -11,6 |
| 9 | 18 | 15,9ª | 39% | -11,7 |
| 10 | 18 | 16,8ª | 44% | -12,0 |
| Todas | 162 | 17,2ª | 59% | -11,4 |

## Caixa por temporada, por tipo (todos os países)

Média dos clubes da divisão no fim da temporada, em rodadas da folha do próprio clube (positivo entra, negativo sai). Uma temporada de liga com 20 clubes tem 38 rodadas.

| Temporada | Divisão | BILHETERIA | COMPRA | CUSTO_DO_JOGO | DESLOCAMENTO | ESTADIO | PATROCINIO | PREMIO | SALARIOS | VENDA | Saldo da temporada |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 1 | 1ª | 54,6 | -1,7 | -8,2 | -6,2 | -2,3 | 1,0 | 1,0 | -37,9 | 5,2 | 5,5 |
| 1 | 2ª | 57,2 | -3,8 | -8,6 | -7,4 | 0,0 | 0,8 | 0,0 | -36,7 | 5,0 | 6,6 |
| 2 | 1ª | 54,4 | -0,2 | -8,2 | -6,4 | -4,3 | 0,8 | 1,1 | -37,5 | 3,5 | 3,2 |
| 2 | 2ª | 58,5 | -4,8 | -8,8 | -7,5 | -1,5 | 0,7 | 0,0 | -36,9 | 4,4 | 4,2 |
| 3 | 1ª | 54,5 | -0,2 | -8,2 | -6,2 | -2,8 | 0,7 | 1,2 | -37,4 | 2,7 | 4,3 |
| 3 | 2ª | 56,3 | -3,3 | -8,4 | -7,4 | -1,6 | 0,7 | 0,0 | -36,6 | 3,2 | 2,8 |
| 4 | 1ª | 53,4 | -0,2 | -8,0 | -6,0 | -2,8 | 0,7 | 0,8 | -37,7 | 3,3 | 3,7 |
| 4 | 2ª | 57,1 | -4,2 | -8,6 | -7,7 | -2,1 | 0,7 | 0,4 | -36,7 | 2,7 | 1,7 |
| 5 | 1ª | 52,4 | -0,1 | -7,9 | -5,9 | -1,9 | 0,7 | 0,8 | -37,9 | 3,4 | 3,6 |
| 5 | 2ª | 57,9 | -4,1 | -8,7 | -7,6 | -1,6 | 0,7 | 0,0 | -36,7 | 2,3 | 2,1 |
| 6 | 1ª | 52,5 | -0,2 | -7,9 | -5,6 | -1,7 | 0,7 | 0,6 | -37,7 | 2,6 | 3,4 |
| 6 | 2ª | 56,1 | -3,4 | -8,4 | -7,6 | -2,0 | 0,7 | 0,0 | -36,6 | 1,5 | 0,3 |
| 7 | 1ª | 53,2 | -0,1 | -8,0 | -5,7 | -1,6 | 0,7 | 1,0 | -38,0 | 3,3 | 4,9 |
| 7 | 2ª | 58,8 | -4,4 | -8,8 | -7,6 | -1,2 | 0,7 | 0,1 | -36,9 | 1,7 | 2,4 |
| 8 | 1ª | 51,9 | -0,3 | -7,8 | -5,6 | -1,1 | 0,7 | 0,8 | -37,7 | 3,0 | 3,9 |
| 8 | 2ª | 57,4 | -3,6 | -8,6 | -7,7 | -1,3 | 0,7 | 0,0 | -36,6 | 1,0 | 1,3 |
| 9 | 1ª | 51,7 | -0,1 | -7,8 | -5,7 | -0,8 | 0,7 | 0,9 | -38,0 | 3,1 | 4,1 |
| 9 | 2ª | 58,4 | -3,5 | -8,8 | -7,5 | -0,8 | 0,7 | 0,0 | -37,2 | 1,5 | 2,9 |
| 10 | 1ª | 51,5 | -0,1 | -7,7 | -5,5 | -1,2 | 0,7 | 0,7 | -38,0 | 3,0 | 3,4 |
| 10 | 2ª | 57,6 | -3,5 | -8,6 | -7,6 | -2,2 | 0,7 | 0,0 | -36,6 | 0,3 | 0,1 |

Tempo de simulação: 1818 s para 60 temporadas.
