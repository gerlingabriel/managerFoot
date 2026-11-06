# Simulação do jogo completo

Gerado por `RelatorioJogoCompletoIT` (ferramenta de desenvolvimento; SPEC §15, Etapa 6). Cada jogo é criado do banco padrão e jogado pelo caminho do servidor, só com clubes da máquina: leilões da máquina antes de cada rodada, liga, copa, continental, Mundial, caixa, evolução de força, trocas de treinador e virada de temporada com promoção e rebaixamento. 1 semente por país (0 a 0), 5 temporadas seguidas cada; a semente da temporada é fixa, então o relatório se refaz igual.

*Força em campo*: média dos 11 mais fortes do plantel no início da temporada. *Rodadas de folha*: saldo no fim da temporada dividido pelos salários da última rodada. *Goleada*: um time com 5 gols ou mais. *Entre os 3 mais fortes*: o campeão da 1ª estava entre as 3 maiores forças em campo no início.

## Por país

| País | Gols por jogo | Mandante vence | Empate | Visitante vence | Goleadas | Expulsões por jogo | Lesões por jogo | Pontos do campeão (1ª) | Pontos do lanterna (1ª) | Campeão entre os 3 mais fortes | Mesmo campeão do ano anterior | Copa: campeão da 1ª | Continental: campeão do país | Vendas por temporada | Trocas de treinador por temporada |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| ARG | 2,8 | 48% | 22% | 30% | 3% | 0,21 | 0,06 | 81,8 (72%) | 22,8 (20%) | 100% | 75% | 100% | 40% | 24,2 | 20,2 |
| BRA | 2,9 | 48% | 22% | 30% | 4% | 0,21 | 0,06 | 81,2 (71%) | 22,0 (19%) | 100% | 50% | 100% | 20% | 25,0 | 23,0 |
| ENG | 2,9 | 47% | 22% | 31% | 4% | 0,21 | 0,06 | 85,2 (75%) | 24,0 (21%) | 100% | 75% | 100% | 20% | 25,8 | 22,4 |
| ESP | 2,9 | 47% | 22% | 31% | 4% | 0,20 | 0,06 | 79,8 (70%) | 23,8 (21%) | 60% | 25% | 100% | 20% | 27,6 | 19,4 |
| ITA | 2,9 | 48% | 21% | 30% | 4% | 0,21 | 0,06 | 84,8 (74%) | 22,8 (20%) | 100% | 25% | 100% | 20% | 24,0 | 21,4 |
| POR | 2,9 | 49% | 22% | 29% | 3% | 0,20 | 0,06 | 73,8 (72%) | 19,2 (19%) | 80% | 0% | 100% | 20% | 20,4 | 19,0 |
| Todos | 2,9 | 48% | 22% | 30% | 4% | 0,21 | 0,06 | 81,1 (72%) | 22,4 (20%) | 90% | 42% | 100% | 23% | 24,5 | 20,9 |

## Por temporada (todos os países)

| Temporada | Força em campo 1ª (média; menor a maior) | Força em campo 2ª | Saldo 1ª em rodadas de folha (mediana) | Clubes negativos 1ª | Saldo 2ª (mediana) | Clubes negativos 2ª | Estádio médio 1ª / 2ª | Gols por jogo | Vendas | Sem lance |
|---|---|---|---|---|---|---|---|---|---|---|
| 1 | 42,5 (35,1 a 49,6) | 26,6 (23,2 a 29,7) | 17,1 | 0% | 18,0 | 0% | 38.266 / 11.189 | 2,9 | 33,8 | 30,0 |
| 2 | 38,6 (27,1 a 48,7) | 24,6 (19,1 a 34,4) | 27,2 | 0% | 21,3 | 0% | 39.269 / 12.734 | 2,9 | 27,7 | 0,0 |
| 3 | 38,6 (27,2 a 49,3) | 23,1 (18,2 a 30,9) | 39,8 | 0% | 24,4 | 0% | 41.496 / 11.922 | 2,9 | 22,8 | 0,0 |
| 4 | 38,5 (27,3 a 49,7) | 23,0 (17,1 a 34,0) | 43,8 | 0% | 23,4 | 1% | 41.878 / 12.188 | 2,9 | 23,7 | 0,0 |
| 5 | 38,4 (26,5 a 49,8) | 23,3 (17,6 a 33,8) | 47,0 | 0% | 29,3 | 5% | 42.155 / 12.528 | 2,8 | 14,5 | 0,0 |

## Quem marca (liga, todos os países)

Gols da liga pela posição do autor; o pênalti fica à parte. *Artilheiro*: gols do maior artilheiro da 1ª na liga, média das temporadas. A máquina escolhe a formação a cada jogo (§7.2 dá os pesos por formação).

| País | Atacante | Meia | Defensor | Goleiro | Pênalti | Artilheiro da 1ª |
|---|---|---|---|---|---|---|
| ARG | 51% | 32% | 9% | 0 | 8% | 19,4 |
| BRA | 52% | 31% | 9% | 0 | 8% | 19,2 |
| ENG | 52% | 32% | 9% | 0 | 8% | 18,2 |
| ESP | 52% | 32% | 9% | 0 | 8% | 18,2 |
| ITA | 52% | 32% | 9% | 0 | 8% | 19,4 |
| POR | 52% | 32% | 8% | 1 | 8% | 19,0 |
| Todos | 52% | 32% | 9% | 1 | 8% | 18,9 |

## Quem sobe da 2ª (todos os países)

Clubes que subiram no fim da temporada anterior, na 1ª. *Caem de volta*: terminam entre os últimos, tantos quantos subiram (a zona de rebaixamento). *Força*: força em campo no início menos a média da 1ª.

| Temporada | Promovidos | Posição média | Caem de volta | Força em relação à média da 1ª |
|---|---|---|---|---|
| 2 | 18 | 18,6ª | 94% | -10,3 |
| 3 | 18 | 16,4ª | 50% | -9,6 |
| 4 | 18 | 16,6ª | 44% | -9,7 |
| 5 | 18 | 18,0ª | 78% | -9,8 |
| Todas | 72 | 17,4ª | 67% | -9,9 |

## Caixa por temporada, por tipo (todos os países)

Média dos clubes da divisão no fim da temporada, em rodadas da folha do próprio clube (positivo entra, negativo sai). Uma temporada de liga com 20 clubes tem 38 rodadas.

| Temporada | Divisão | BILHETERIA | COMPRA | CUSTO_DO_JOGO | DESLOCAMENTO | ESTADIO | PATROCINIO | PREMIO | SALARIOS | VENDA | Saldo da temporada |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 1 | 1ª | 60,0 | -1,2 | -9,0 | -7,2 | -1,5 | 1,1 | 1,3 | -40,4 | 4,6 | 7,7 |
| 1 | 2ª | 63,0 | -3,3 | -9,5 | -8,6 | 0,0 | 1,0 | 0,0 | -39,4 | 4,3 | 7,4 |
| 2 | 1ª | 57,8 | -0,1 | -8,7 | -7,9 | -2,4 | 1,0 | 1,2 | -38,8 | 3,2 | 5,2 |
| 2 | 2ª | 61,0 | -4,3 | -9,1 | -8,9 | 0,0 | 0,8 | 0,0 | -37,5 | 4,4 | 6,4 |
| 3 | 1ª | 57,1 | -0,2 | -8,6 | -7,6 | -1,0 | 0,9 | 1,2 | -37,9 | 2,5 | 6,6 |
| 3 | 2ª | 59,2 | -3,4 | -8,9 | -9,2 | -0,1 | 0,9 | 0,0 | -37,3 | 3,7 | 5,0 |
| 4 | 1ª | 55,7 | -0,2 | -8,4 | -7,4 | -1,0 | 0,9 | 1,4 | -38,3 | 2,9 | 5,6 |
| 4 | 2ª | 60,6 | -3,8 | -9,1 | -9,6 | 0,0 | 0,9 | 0,0 | -37,0 | 3,4 | 5,4 |
| 5 | 1ª | 54,8 | -0,0 | -8,2 | -7,5 | -0,4 | 0,9 | 1,1 | -38,3 | 2,2 | 4,6 |
| 5 | 2ª | 60,6 | -2,7 | -9,1 | -9,5 | -0,1 | 0,9 | 0,1 | -37,0 | 1,5 | 4,7 |

Tempo de simulação: 788 s para 30 temporadas.
