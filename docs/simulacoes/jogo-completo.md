# Simulação do jogo completo

Gerado por `RelatorioJogoCompletoIT` (ferramenta de desenvolvimento; SPEC §15, Etapa 6). Cada jogo é criado do banco padrão e jogado pelo caminho do servidor, só com clubes da máquina: leilões da máquina antes de cada rodada, liga, copa, continental, Mundial, caixa, evolução de força, trocas de treinador e virada de temporada com promoção e rebaixamento. 2 sementes por país (0 a 1), 5 temporadas seguidas cada; a semente da temporada é fixa, então o relatório se refaz igual.

*Força em campo*: média dos 11 mais fortes do plantel no início da temporada. *Rodadas de folha*: saldo no fim da temporada dividido pelos salários da última rodada. *Goleada*: um time com 5 gols ou mais. *Entre os 3 mais fortes*: o campeão da 1ª estava entre as 3 maiores forças em campo no início.

## Por país

| País | Gols por jogo | Mandante vence | Empate | Visitante vence | Goleadas | Expulsões por jogo | Lesões por jogo | Pontos do campeão (1ª) | Pontos do lanterna (1ª) | Campeão entre os 3 mais fortes | Mesmo campeão do ano anterior | Copa: campeão da 1ª | Continental: campeão do país | Vendas por temporada | Trocas de treinador por temporada |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| ARG | 2,9 | 49% | 22% | 30% | 3% | 0,21 | 0,06 | 80,4 (71%) | 28,0 (25%) | 70% | 13% | 90% | 50% | 28,1 | 22,6 |
| BRA | 2,9 | 48% | 22% | 30% | 4% | 0,20 | 0,06 | 82,1 (72%) | 21,8 (19%) | 50% | 13% | 100% | 50% | 31,0 | 23,2 |
| ENG | 2,9 | 48% | 22% | 30% | 3% | 0,21 | 0,06 | 81,4 (71%) | 24,4 (21%) | 60% | 0% | 100% | 10% | 30,9 | 21,2 |
| ESP | 3,0 | 49% | 21% | 30% | 4% | 0,20 | 0,06 | 81,4 (71%) | 24,2 (21%) | 50% | 13% | 90% | 20% | 30,5 | 22,2 |
| ITA | 2,9 | 48% | 22% | 29% | 3% | 0,21 | 0,06 | 83,8 (74%) | 23,7 (21%) | 80% | 50% | 100% | 30% | 30,1 | 20,7 |
| POR | 2,9 | 47% | 22% | 31% | 3% | 0,20 | 0,06 | 72,8 (71%) | 21,1 (21%) | 50% | 13% | 100% | 0% | 22,3 | 18,1 |
| Todos | 2,9 | 48% | 22% | 30% | 3% | 0,21 | 0,06 | 80,3 (72%) | 23,9 (21%) | 60% | 17% | 97% | 27% | 28,8 | 21,3 |

## Por temporada (todos os países)

| Temporada | Força em campo 1ª (média; menor a maior) | Força em campo 2ª | Saldo 1ª em rodadas de folha (mediana) | Clubes negativos 1ª | Saldo 2ª (mediana) | Clubes negativos 2ª | Estádio médio 1ª / 2ª | Gols por jogo | Vendas | Sem lance |
|---|---|---|---|---|---|---|---|---|---|---|
| 1 | 42,5 (35,1 a 49,6) | 26,6 (23,1 a 29,7) | 15,2 | 0% | 16,7 | 0% | 38.708 / 11.189 | 2,8 | 40,2 | 37,1 |
| 2 | 39,9 (28,4 a 49,4) | 26,2 (21,1 a 38,0) | 19,0 | 0% | 18,7 | 0% | 41.421 / 13.539 | 3,0 | 29,3 | 0,0 |
| 3 | 40,7 (29,8 a 49,9) | 25,4 (19,8 a 32,6) | 20,8 | 0% | 19,7 | 0% | 46.806 / 12.664 | 3,0 | 27,5 | 0,0 |
| 4 | 41,0 (29,6 a 50,0) | 25,8 (19,2 a 35,3) | 23,0 | 1% | 19,7 | 0% | 48.543 / 13.746 | 3,0 | 24,5 | 0,0 |
| 5 | 41,3 (29,8 a 50,0) | 26,2 (19,7 a 35,7) | 23,5 | 1% | 20,2 | 1% | 50.034 / 14.286 | 2,9 | 22,7 | 0,0 |

## Quem marca (liga, todos os países)

Gols da liga pela posição do autor; o pênalti fica à parte. *Artilheiro*: gols do maior artilheiro da 1ª na liga, média das temporadas. A máquina escolhe a formação a cada jogo (§7.2 dá os pesos por formação).

| País | Atacante | Meia | Defensor | Goleiro | Pênalti | Artilheiro da 1ª |
|---|---|---|---|---|---|---|
| ARG | 52% | 32% | 8% | 1 | 8% | 20,4 |
| BRA | 52% | 32% | 8% | 3 | 8% | 21,6 |
| ENG | 52% | 32% | 9% | 4 | 8% | 22,8 |
| ESP | 53% | 31% | 8% | 4 | 8% | 23,3 |
| ITA | 52% | 32% | 8% | 1 | 8% | 19,6 |
| POR | 53% | 31% | 8% | 2 | 8% | 18,1 |
| Todos | 52% | 32% | 8% | 15 | 8% | 21,0 |

## Quem sobe da 2ª (todos os países)

Clubes que subiram no fim da temporada anterior, na 1ª. *Caem de volta*: terminam entre os últimos, tantos quantos subiram (a zona de rebaixamento). *Força*: força em campo no início menos a média da 1ª.

| Temporada | Promovidos | Posição média | Caem de volta | Força em relação à média da 1ª |
|---|---|---|---|---|
| 2 | 36 | 18,1ª | 81% | -10,8 |
| 3 | 36 | 16,5ª | 61% | -10,1 |
| 4 | 36 | 17,2ª | 61% | -10,9 |
| 5 | 36 | 17,1ª | 50% | -11,1 |
| Todas | 144 | 17,2ª | 63% | -10,7 |

## Caixa por temporada, por tipo (todos os países)

Média dos clubes da divisão no fim da temporada, em rodadas da folha do próprio clube (positivo entra, negativo sai). Uma temporada de liga com 20 clubes tem 38 rodadas.

| Temporada | Divisão | BILHETERIA | COMPRA | CUSTO_DO_JOGO | DESLOCAMENTO | ESTADIO | PATROCINIO | PREMIO | SALARIOS | VENDA | Saldo da temporada |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 1 | 1ª | 54,5 | -1,7 | -8,2 | -6,1 | -1,8 | 1,0 | 1,0 | -37,9 | 5,0 | 5,7 |
| 1 | 2ª | 57,6 | -3,7 | -8,6 | -7,4 | 0,0 | 0,8 | 0,0 | -36,9 | 4,8 | 6,6 |
| 2 | 1ª | 54,7 | -0,2 | -8,2 | -6,6 | -4,2 | 0,8 | 0,9 | -37,7 | 3,6 | 3,1 |
| 2 | 2ª | 59,3 | -4,4 | -8,9 | -7,5 | -1,8 | 0,7 | 0,0 | -37,3 | 4,0 | 4,1 |
| 3 | 1ª | 54,1 | -0,4 | -8,1 | -6,1 | -3,1 | 0,7 | 1,0 | -37,4 | 3,5 | 4,3 |
| 3 | 2ª | 56,9 | -4,1 | -8,5 | -7,5 | -1,4 | 0,7 | 0,1 | -36,6 | 3,5 | 3,0 |
| 4 | 1ª | 53,5 | -0,3 | -8,0 | -6,0 | -2,5 | 0,7 | 1,0 | -37,7 | 3,4 | 4,2 |
| 4 | 2ª | 57,9 | -4,3 | -8,7 | -7,7 | -1,5 | 0,7 | 0,4 | -36,8 | 2,7 | 2,7 |
| 5 | 1ª | 52,6 | -0,3 | -7,9 | -5,8 | -1,6 | 0,7 | 0,9 | -37,7 | 3,4 | 4,4 |
| 5 | 2ª | 57,5 | -4,2 | -8,6 | -7,5 | -1,5 | 0,7 | 0,1 | -36,7 | 2,1 | 1,8 |

Tempo de simulação: 952 s para 60 temporadas.
