# Ajuste do caixa: ingresso e salário

Gerado por `RelatorioCicloMercadoTest` com `RELATORIO_AJUSTE_CAIXA=1` (ferramenta de desenvolvimento; SPEC §15, Etapa 6). O mesmo simulador de `ciclo-do-mercado.md`, com o padrão atual (faixas da rev. 66, plantel até 23): as 6 ligas do banco padrão, 3 sementes, 6 temporadas com promoção e rebaixamento, só máquina, leilões da máquina antes de cada rodada. Sem copa, continental e Mundial (o jogo completo tem mais jogos em casa). Cada linha muda só o ingresso (todas as divisões) ou o salário de referência; o salário maior também encarece o preço do jogador, o prêmio, o patrocínio e a obra do estádio, como no jogo.

*Saldo*: média dos clubes da divisão no fim da temporada, em rodadas da folha do próprio clube (o jogo começa com 10). *Ganho por temporada*: (saldo no fim da 6ª − no fim da 1ª) ÷ 5. *Negativos*: clubes com saldo abaixo de zero no fim da temporada, média das temporadas. *Vendas*: leilões da máquina vendidos por liga e temporada.

| Cenário | Saldo 1ª fim da 1ª | Saldo 1ª fim da 6ª | Ganho por temporada 1ª | 1ª topo / meio / fundo (fim) | Saldo 2ª fim da 1ª | Saldo 2ª fim da 6ª | Ganho por temporada 2ª | 2ª topo / meio / fundo (fim) | Negativos 1ª / 2ª | Vendas 1ª temporada | Vendas 2ª a 6ª | Sem lance 2ª a 6ª | Ligas travadas |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Atual: ingresso R$ 40, salário ×1 | 26,2 | 109,0 | 16,6 | 149,6 / 83,4 / 91,8 | 34,8 | 107,6 | 14,6 | 155,0 / 117,3 / 42,8 | 0% / 1% | 21,9 | 7,0 | 0,0 | 0 de 18 |
| Ingresso R$ 35 | 20,0 | 61,2 | 8,2 | 95,6 / 40,3 / 45,8 | 27,9 | 69,0 | 8,2 | 95,4 / 82,6 / 23,5 | 0% / 3% | 32,4 | 14,8 | 0,0 | 0 de 18 |
| Ingresso R$ 30 | 14,4 | 24,6 | 2,0 | 50,0 / 17,2 / 4,0 | 17,6 | 11,4 | -1,2 | 22,4 / 15,7 / -5,8 | 5% / 6% | 44,2 | 37,1 | 68,0 | 0 de 18 |
| Ingresso R$ 25 | 5,0 | -15,0 | -4,0 | 16,0 / -19,9 / -44,8 | 10,5 | -30,6 | -8,2 | -13,7 / -24,9 / -56,2 | 51% / 50% | 24,1 | 15,8 | 913,8 | 0 de 18 |
| Salário ×1,3 | 16,7 | 35,1 | 3,7 | 61,3 / 22,1 / 19,8 | 21,5 | 39,8 | 3,7 | 50,1 / 54,2 / 11,5 | 1% / 3% | 39,3 | 24,2 | 0,0 | 0 de 18 |
| Salário ×1,45 | 12,3 | 16,2 | 0,8 | 37,0 / 11,2 / -1,8 | 17,2 | 12,7 | -0,9 | 20,6 / 16,2 / -0,5 | 9% / 4% | 38,1 | 36,5 | 126,2 | 0 de 18 |
| Salário ×1,6 | 7,2 | -4,7 | -2,4 | 19,8 / -7,5 / -29,6 | 15,6 | -0,8 | -3,3 | 2,2 / 4,4 / -10,2 | 41% / 18% | 29,3 | 24,1 | 601,9 | 0 de 18 |
| Ingresso R$ 35 e salário ×1,2 | 14,5 | 24,3 | 2,0 | 48,7 / 15,4 / 6,6 | 17,5 | 14,2 | -0,7 | 23,5 / 18,0 / -0,7 | 2% / 5% | 44,1 | 35,3 | 33,8 | 0 de 18 |

## Saldo médio por temporada (rodadas da folha)

| Cenário | Divisão | 1ª | 2ª | 3ª | 4ª | 5ª | 6ª |
|---|---|---|---|---|---|---|---|
| Atual: ingresso R$ 40, salário ×1 | 1ª | 26,2 (0% neg.) | 47,2 (0% neg.) | 63,2 (0% neg.) | 82,7 (0% neg.) | 94,4 (0% neg.) | 109,0 (0% neg.) |
| Atual: ingresso R$ 40, salário ×1 | 2ª | 34,8 (0% neg.) | 52,3 (0% neg.) | 66,9 (0% neg.) | 78,6 (0% neg.) | 95,1 (1% neg.) | 107,6 (4% neg.) |
| Ingresso R$ 35 | 1ª | 20,0 (0% neg.) | 30,6 (0% neg.) | 39,0 (0% neg.) | 48,7 (0% neg.) | 52,5 (0% neg.) | 61,2 (1% neg.) |
| Ingresso R$ 35 | 2ª | 27,9 (0% neg.) | 40,1 (0% neg.) | 47,9 (0% neg.) | 54,8 (2% neg.) | 64,3 (5% neg.) | 69,0 (8% neg.) |
| Ingresso R$ 30 | 1ª | 14,4 (2% neg.) | 19,0 (0% neg.) | 20,7 (1% neg.) | 22,2 (3% neg.) | 23,2 (9% neg.) | 24,6 (13% neg.) |
| Ingresso R$ 30 | 2ª | 17,6 (0% neg.) | 20,4 (0% neg.) | 18,5 (2% neg.) | 16,0 (6% neg.) | 13,3 (12% neg.) | 11,4 (17% neg.) |
| Ingresso R$ 25 | 1ª | 5,0 (37% neg.) | 1,2 (46% neg.) | -3,0 (48% neg.) | -5,8 (53% neg.) | -11,8 (58% neg.) | -15,0 (64% neg.) |
| Ingresso R$ 25 | 2ª | 10,5 (13% neg.) | 5,0 (31% neg.) | -1,4 (48% neg.) | -11,3 (61% neg.) | -18,0 (69% neg.) | -30,6 (77% neg.) |
| Salário ×1,3 | 1ª | 16,7 (0% neg.) | 21,6 (0% neg.) | 25,7 (1% neg.) | 30,0 (1% neg.) | 32,2 (1% neg.) | 35,1 (0% neg.) |
| Salário ×1,3 | 2ª | 21,5 (0% neg.) | 30,0 (0% neg.) | 34,5 (1% neg.) | 38,0 (1% neg.) | 39,9 (5% neg.) | 39,8 (10% neg.) |
| Salário ×1,45 | 1ª | 12,3 (6% neg.) | 16,8 (0% neg.) | 17,0 (2% neg.) | 16,9 (8% neg.) | 16,7 (16% neg.) | 16,2 (20% neg.) |
| Salário ×1,45 | 2ª | 17,2 (0% neg.) | 17,4 (0% neg.) | 16,8 (1% neg.) | 16,1 (3% neg.) | 14,2 (8% neg.) | 12,7 (14% neg.) |
| Salário ×1,6 | 1ª | 7,2 (29% neg.) | 6,6 (32% neg.) | 4,0 (37% neg.) | 2,4 (43% neg.) | -2,1 (51% neg.) | -4,7 (55% neg.) |
| Salário ×1,6 | 2ª | 15,6 (0% neg.) | 13,6 (2% neg.) | 12,2 (8% neg.) | 7,0 (22% neg.) | 4,4 (33% neg.) | -0,8 (44% neg.) |
| Ingresso R$ 35 e salário ×1,2 | 1ª | 14,5 (1% neg.) | 18,4 (0% neg.) | 20,8 (1% neg.) | 22,1 (2% neg.) | 23,0 (4% neg.) | 24,3 (6% neg.) |
| Ingresso R$ 35 e salário ×1,2 | 2ª | 17,5 (0% neg.) | 21,0 (0% neg.) | 20,8 (2% neg.) | 18,7 (6% neg.) | 16,3 (9% neg.) | 14,2 (14% neg.) |

## Conferência no jogo completo: ingresso R$ 30

`RelatorioJogoCompletoIT` com o ingresso trocado só para a simulação (6 países, 1 semente, 5 temporadas, com copa, continental, Mundial e trocas de treinador). Comparar com `jogo-completo.md` (R$ 40).

### Por temporada (todos os países)

| Temporada | Força em campo 1ª (média; menor a maior) | Força em campo 2ª | Saldo 1ª em rodadas de folha (mediana) | Clubes negativos 1ª | Saldo 2ª (mediana) | Clubes negativos 2ª | Estádio médio 1ª / 2ª | Gols por jogo | Vendas | Sem lance |
|---|---|---|---|---|---|---|---|---|---|---|
| 1 | 42,5 (35,1 a 49,6) | 26,6 (23,2 a 29,7) | 16,8 | 0% | 19,8 | 0% | 38.182 / 11.231 | 2,7 | 42,2 | 29,3 |
| 2 | 38,4 (27,1 a 48,2) | 24,5 (19,5 a 33,9) | 24,3 | 0% | 27,4 | 0% | 38.488 / 12.661 | 2,6 | 30,0 | 0,0 |
| 3 | 37,9 (25,9 a 48,4) | 22,6 (17,7 a 29,6) | 25,9 | 0% | 31,3 | 3% | 40.088 / 12.146 | 2,7 | 26,0 | 0,0 |
| 4 | 37,9 (23,8 a 48,8) | 22,6 (17,3 a 31,0) | 34,9 | 2% | 30,3 | 14% | 40.500 / 12.129 | 2,7 | 20,5 | 0,0 |
| 5 | 38,2 (25,7 a 48,8) | 22,7 (17,5 a 32,0) | 35,3 | 1% | 33,2 | 17% | 40.680 / 12.211 | 2,6 | 15,8 | 0,0 |

### Caixa por temporada, por tipo (todos os países)

Média dos clubes da divisão no fim da temporada, em rodadas da folha do próprio clube (positivo entra, negativo sai). Uma temporada de liga com 20 clubes tem 38 rodadas.

| Temporada | Divisão | BILHETERIA | COMPRA | CUSTO_DO_JOGO | DESLOCAMENTO | ESTADIO | PATROCINIO | PREMIO | SALARIOS | VENDA | Saldo da temporada |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 1 | 1ª | 61,4 | -2,9 | -9,2 | -7,5 | -1,6 | 1,2 | 1,0 | -39,8 | 4,8 | 7,4 |
| 1 | 2ª | 64,7 | -0,5 | -9,7 | -12,2 | -0,4 | 1,0 | 0,0 | -40,9 | 7,2 | 9,3 |
| 2 | 1ª | 57,5 | -1,8 | -8,6 | -8,7 | -2,1 | 1,0 | 1,1 | -38,3 | 2,5 | 2,6 |
| 2 | 2ª | 64,0 | -1,6 | -9,6 | -12,7 | 0,0 | 0,9 | 0,0 | -38,5 | 6,0 | 8,6 |
| 3 | 1ª | 54,6 | -1,6 | -8,2 | -8,0 | -0,9 | 1,0 | 1,2 | -37,1 | 2,6 | 3,5 |
| 3 | 2ª | 62,7 | -1,6 | -9,4 | -13,5 | 0,0 | 1,0 | 0,0 | -37,4 | 4,4 | 6,1 |
| 4 | 1ª | 53,4 | -1,3 | -8,0 | -8,0 | -0,2 | 1,0 | 1,3 | -37,4 | 2,5 | 3,2 |
| 4 | 2ª | 62,0 | -2,1 | -9,3 | -13,6 | 0,0 | 1,0 | 0,3 | -37,0 | 2,6 | 4,0 |
| 5 | 1ª | 54,0 | -1,3 | -8,1 | -7,6 | -0,1 | 0,9 | 1,0 | -37,7 | 2,1 | 3,1 |
| 5 | 2ª | 61,5 | -1,8 | -9,2 | -13,7 | 0,0 | 1,0 | 0,0 | -37,0 | 1,5 | 2,2 |

Tempo de simulação: 368 s para 30 temporadas.
