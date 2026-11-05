# Simulação do jogo completo

Gerado por `RelatorioJogoCompletoIT` (ferramenta de desenvolvimento; SPEC §15, Etapa 6). Cada jogo é criado do banco padrão e jogado pelo caminho do servidor, só com clubes da máquina: leilões da máquina antes de cada rodada, liga, copa, continental, Mundial, caixa, evolução de força, trocas de treinador e virada de temporada com promoção e rebaixamento. 1 semente por país (0 a 0), 5 temporadas seguidas cada; a semente da temporada é fixa, então o relatório se refaz igual.

*Força em campo*: média dos 11 mais fortes do plantel no início da temporada. *Rodadas de folha*: saldo no fim da temporada dividido pelos salários da última rodada. *Goleada*: um time com 5 gols ou mais. *Entre os 3 mais fortes*: o campeão da 1ª estava entre as 3 maiores forças em campo no início.

## Por país

| País | Gols por jogo | Mandante vence | Empate | Visitante vence | Goleadas | Expulsões por jogo | Lesões por jogo | Pontos do campeão (1ª) | Pontos do lanterna (1ª) | Campeão entre os 3 mais fortes | Mesmo campeão do ano anterior | Copa: campeão da 1ª | Continental: campeão do país | Vendas por temporada | Trocas de treinador por temporada |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| ARG | 2,7 | 47% | 24% | 29% | 5% | 0,21 | 0,06 | 76,0 (67%) | 20,6 (18%) | 100% | 25% | 100% | 20% | 10,0 | 21,6 |
| BRA | 2,7 | 47% | 23% | 30% | 5% | 0,20 | 0,06 | 76,6 (67%) | 20,2 (18%) | 60% | 0% | 100% | 40% | 9,8 | 20,4 |
| ENG | 2,7 | 47% | 24% | 29% | 6% | 0,21 | 0,06 | 82,0 (72%) | 21,4 (19%) | 80% | 0% | 100% | 20% | 11,4 | 22,4 |
| ESP | 2,6 | 46% | 24% | 30% | 5% | 0,21 | 0,06 | 76,6 (67%) | 20,8 (18%) | 80% | 25% | 80% | 0% | 10,2 | 22,4 |
| ITA | 2,7 | 47% | 23% | 30% | 5% | 0,20 | 0,06 | 76,4 (67%) | 23,4 (21%) | 60% | 0% | 100% | 0% | 11,0 | 19,6 |
| POR | 2,7 | 47% | 24% | 29% | 5% | 0,19 | 0,06 | 68,4 (67%) | 20,6 (20%) | 20% | 0% | 100% | 0% | 7,6 | 19,6 |
| Todos | 2,7 | 47% | 24% | 30% | 5% | 0,20 | 0,06 | 76,0 (68%) | 21,2 (19%) | 67% | 8% | 97% | 13% | 10,0 | 21,0 |

## Por temporada (todos os países)

| Temporada | Força em campo 1ª (média; menor a maior) | Força em campo 2ª | Saldo 1ª em rodadas de folha (mediana) | Clubes negativos 1ª | Saldo 2ª (mediana) | Clubes negativos 2ª | Estádio médio 1ª / 2ª | Gols por jogo | Vendas | Sem lance |
|---|---|---|---|---|---|---|---|---|---|---|
| 1 | 42,5 (35,1 a 49,6) | 26,6 (23,2 a 29,7) | 34,7 | 0% | 39,6 | 0% | 40.553 / 11.231 | 2,7 | 21,2 | 19,0 |
| 2 | 38,5 (25,6 a 48,0) | 24,5 (19,6 a 33,0) | 66,6 | 0% | 59,7 | 0% | 40.403 / 12.623 | 2,6 | 9,7 | 0,0 |
| 3 | 38,3 (27,2 a 47,3) | 22,5 (18,0 a 28,7) | 91,9 | 0% | 84,8 | 0% | 41.854 / 11.560 | 2,6 | 9,5 | 0,0 |
| 4 | 37,8 (25,1 a 48,2) | 22,9 (17,1 a 32,9) | 128,3 | 0% | 94,2 | 0% | 41.080 / 12.443 | 2,7 | 6,5 | 0,0 |
| 5 | 38,0 (25,2 a 48,7) | 22,6 (17,7 a 31,2) | 151,9 | 0% | 119,8 | 2% | 41.795 / 12.094 | 2,6 | 3,2 | 0,0 |

## Caixa por temporada, por tipo (todos os países)

Média dos clubes da divisão no fim da temporada, em rodadas da folha do próprio clube (positivo entra, negativo sai). Uma temporada de liga com 20 clubes tem 38 rodadas.

| Temporada | Divisão | BILHETERIA | COMPRA | CUSTO_DO_JOGO | DESLOCAMENTO | ESTADIO | PATROCINIO | PREMIO | SALARIOS | VENDA | Saldo da temporada |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 1 | 1ª | 82,8 | -1,1 | -12,4 | -7,4 | -4,5 | 1,1 | 1,3 | -39,5 | 2,0 | 22,4 |
| 1 | 2ª | 85,9 | 0,0 | -12,9 | -11,8 | -0,3 | 1,0 | 0,0 | -40,4 | 3,9 | 25,5 |
| 2 | 1ª | 79,0 | -0,5 | -11,9 | -8,7 | -1,1 | 1,1 | 1,1 | -38,0 | 0,2 | 21,2 |
| 2 | 2ª | 83,8 | 0,0 | -12,6 | -12,1 | 0,0 | 0,9 | 0,0 | -38,2 | 2,8 | 24,7 |
| 3 | 1ª | 75,7 | -0,6 | -11,4 | -7,8 | -0,3 | 1,0 | 1,4 | -37,0 | 0,4 | 21,4 |
| 3 | 2ª | 82,3 | 0,0 | -12,3 | -12,8 | 0,0 | 1,0 | 0,0 | -37,5 | 2,4 | 23,0 |
| 4 | 1ª | 73,3 | -0,4 | -11,0 | -8,3 | -0,1 | 1,0 | 1,0 | -37,1 | 0,1 | 18,6 |
| 4 | 2ª | 82,6 | -0,0 | -12,4 | -12,7 | 0,0 | 0,9 | 0,6 | -37,4 | 1,9 | 23,4 |
| 5 | 1ª | 72,9 | -0,2 | -10,9 | -7,8 | -0,3 | 1,0 | 1,1 | -37,2 | 0,0 | 18,6 |
| 5 | 2ª | 81,8 | -0,1 | -12,3 | -12,9 | -0,2 | 1,0 | 0,0 | -37,4 | 0,9 | 20,9 |

Tempo de simulação: 394 s para 30 temporadas.
