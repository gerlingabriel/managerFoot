# Simulação do jogo completo

Gerado por `RelatorioJogoCompletoIT` (ferramenta de desenvolvimento; SPEC §15, Etapa 6). Cada jogo é criado do banco padrão e jogado pelo caminho do servidor, só com clubes da máquina: leilões da máquina antes de cada rodada, liga, copa, continental, Mundial, caixa, evolução de força, trocas de treinador e virada de temporada com promoção e rebaixamento. 1 semente por país (0 a 0), 5 temporadas seguidas cada; a semente da temporada é fixa, então o relatório se refaz igual.

*Força em campo*: média dos 11 mais fortes do plantel no início da temporada. *Rodadas de folha*: saldo no fim da temporada dividido pelos salários da última rodada. *Goleada*: um time com 5 gols ou mais. *Entre os 3 mais fortes*: o campeão da 1ª estava entre as 3 maiores forças em campo no início.

## Por país

| País | Gols por jogo | Mandante vence | Empate | Visitante vence | Goleadas | Expulsões por jogo | Lesões por jogo | Pontos do campeão (1ª) | Pontos do lanterna (1ª) | Campeão entre os 3 mais fortes | Mesmo campeão do ano anterior | Copa: campeão da 1ª | Continental: campeão do país | Vendas por temporada | Trocas de treinador por temporada |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| ARG | 2,9 | 48% | 22% | 30% | 3% | 0,21 | 0,06 | 82,6 (72%) | 29,0 (25%) | 60% | 0% | 80% | 40% | 26,8 | 21,0 |
| BRA | 2,9 | 48% | 22% | 30% | 3% | 0,21 | 0,06 | 79,4 (70%) | 22,4 (20%) | 20% | 0% | 100% | 60% | 29,6 | 21,6 |
| ENG | 2,9 | 49% | 22% | 30% | 4% | 0,20 | 0,06 | 83,8 (74%) | 22,0 (19%) | 80% | 25% | 80% | 20% | 29,4 | 20,2 |
| ESP | 2,9 | 48% | 22% | 31% | 3% | 0,20 | 0,06 | 80,2 (70%) | 23,0 (20%) | 40% | 0% | 100% | 20% | 32,2 | 21,8 |
| ITA | 2,9 | 48% | 23% | 30% | 4% | 0,20 | 0,06 | 78,6 (69%) | 26,6 (23%) | 40% | 0% | 100% | 0% | 30,4 | 21,2 |
| POR | 2,9 | 48% | 23% | 29% | 4% | 0,19 | 0,06 | 71,2 (70%) | 22,8 (22%) | 60% | 25% | 100% | 20% | 24,2 | 19,2 |
| Todos | 2,9 | 48% | 22% | 30% | 3% | 0,20 | 0,06 | 79,3 (71%) | 24,3 (22%) | 50% | 8% | 93% | 27% | 28,8 | 20,8 |

## Por temporada (todos os países)

| Temporada | Força em campo 1ª (média; menor a maior) | Força em campo 2ª | Saldo 1ª em rodadas de folha (mediana) | Clubes negativos 1ª | Saldo 2ª (mediana) | Clubes negativos 2ª | Estádio médio 1ª / 2ª | Gols por jogo | Vendas | Sem lance |
|---|---|---|---|---|---|---|---|---|---|---|
| 1 | 42,5 (35,1 a 49,6) | 26,6 (23,2 a 29,7) | 14,6 | 0% | 16,7 | 0% | 38.983 / 11.189 | 2,8 | 40,8 | 36,3 |
| 2 | 40,2 (28,5 a 49,6) | 26,1 (21,3 a 36,4) | 17,7 | 0% | 18,6 | 0% | 42.844 / 12.948 | 3,0 | 31,5 | 0,0 |
| 3 | 41,0 (29,7 a 49,8) | 25,5 (19,8 a 32,2) | 21,8 | 0% | 19,2 | 0% | 47.706 / 12.678 | 3,0 | 27,0 | 0,0 |
| 4 | 41,3 (29,2 a 50,0) | 25,8 (19,2 a 34,8) | 23,7 | 2% | 19,6 | 0% | 49.851 / 13.446 | 3,0 | 23,2 | 0,0 |
| 5 | 41,4 (29,4 a 50,0) | 26,4 (19,7 a 36,1) | 28,4 | 1% | 20,1 | 4% | 50.794 / 14.798 | 2,8 | 21,3 | 0,0 |

## Quem marca (liga, todos os países)

Gols da liga pela posição do autor; o pênalti fica à parte. *Artilheiro*: gols do maior artilheiro da 1ª na liga, média das temporadas. A máquina escolhe a formação a cada jogo (§7.2 dá os pesos por formação).

| País | Atacante | Meia | Defensor | Goleiro | Pênalti | Artilheiro da 1ª |
|---|---|---|---|---|---|---|
| ARG | 52% | 31% | 9% | 0 | 8% | 19,2 |
| BRA | 53% | 31% | 9% | 1 | 8% | 20,4 |
| ENG | 52% | 31% | 9% | 0 | 8% | 23,6 |
| ESP | 53% | 30% | 9% | 1 | 7% | 23,0 |
| ITA | 52% | 31% | 8% | 0 | 8% | 18,6 |
| POR | 53% | 31% | 8% | 1 | 7% | 19,4 |
| Todos | 53% | 31% | 9% | 3 | 8% | 20,7 |

## Quem sobe da 2ª (todos os países)

Clubes que subiram no fim da temporada anterior, na 1ª. *Caem de volta*: terminam entre os últimos, tantos quantos subiram (a zona de rebaixamento). *Força*: força em campo no início menos a média da 1ª.

| Temporada | Promovidos | Posição média | Caem de volta | Força em relação à média da 1ª |
|---|---|---|---|---|
| 2 | 18 | 18,6ª | 94% | -11,2 |
| 3 | 18 | 17,1ª | 67% | -10,9 |
| 4 | 18 | 17,8ª | 61% | -11,4 |
| 5 | 18 | 17,9ª | 67% | -11,5 |
| Todas | 72 | 17,8ª | 72% | -11,2 |

## Caixa por temporada, por tipo (todos os países)

Média dos clubes da divisão no fim da temporada, em rodadas da folha do próprio clube (positivo entra, negativo sai). Uma temporada de liga com 20 clubes tem 38 rodadas.

| Temporada | Divisão | BILHETERIA | COMPRA | CUSTO_DO_JOGO | DESLOCAMENTO | ESTADIO | PATROCINIO | PREMIO | SALARIOS | VENDA | Saldo da temporada |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 1 | 1ª | 54,3 | -1,6 | -8,1 | -6,2 | -2,2 | 1,0 | 1,0 | -37,8 | 5,2 | 5,5 |
| 1 | 2ª | 57,4 | -3,8 | -8,6 | -7,4 | 0,0 | 0,8 | 0,0 | -36,8 | 4,9 | 6,5 |
| 2 | 1ª | 54,0 | -0,2 | -8,1 | -6,4 | -4,7 | 0,8 | 0,8 | -37,7 | 3,8 | 2,2 |
| 2 | 2ª | 58,5 | -4,7 | -8,8 | -7,5 | -0,8 | 0,7 | 0,0 | -36,9 | 4,3 | 4,9 |
| 3 | 1ª | 54,5 | -0,3 | -8,2 | -6,2 | -3,1 | 0,7 | 1,0 | -37,7 | 3,6 | 4,4 |
| 3 | 2ª | 55,9 | -4,0 | -8,4 | -7,4 | -2,1 | 0,7 | 0,1 | -36,5 | 3,5 | 1,8 |
| 4 | 1ª | 53,1 | -0,3 | -8,0 | -5,8 | -2,4 | 0,7 | 0,9 | -37,4 | 2,9 | 3,8 |
| 4 | 2ª | 57,6 | -3,9 | -8,6 | -7,7 | -1,8 | 0,7 | 0,5 | -36,7 | 2,8 | 3,0 |
| 5 | 1ª | 52,8 | -0,2 | -7,9 | -5,9 | -1,9 | 0,7 | 1,0 | -37,8 | 3,3 | 4,1 |
| 5 | 2ª | 57,7 | -4,1 | -8,7 | -7,6 | -2,3 | 0,7 | 0,0 | -36,8 | 2,1 | 1,0 |

Tempo de simulação: 1122 s para 30 temporadas.
