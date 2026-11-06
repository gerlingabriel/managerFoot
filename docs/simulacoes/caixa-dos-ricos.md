# Caixa dos ricos: salário dos fortes e público do líder

Gerado por `RelatorioCicloMercadoTest` com `RELATORIO_CAIXA_RICO=1` (ferramenta de desenvolvimento; SPEC §15, Etapa 6). O mesmo simulador de `ciclo-do-mercado.md`, com o padrão atual (faixas da rev. 66, plantel até 23): as 6 ligas do banco padrão, 3 sementes, 6 temporadas com promoção e rebaixamento, só máquina, leilões da máquina antes de cada rodada. Sem copa, continental e Mundial (o jogo completo tem mais jogos em casa). Ingresso de R$ 30 em todas as linhas. Base da rev. 119 (último com 18% da torcida, deslocamento R$ 60/22,5/11,25 mil). Cada linha muda o fator do salário das faixas fortes (força em campo do clube 35–44 e 45–50; o preço do jogador não usa a faixa) e a ocupação do líder.

*Saldo*: média dos clubes da divisão no fim da temporada, em rodadas da folha do próprio clube (o jogo começa com 10). *Ganho por temporada*: (saldo no fim da 6ª − no fim da 1ª) ÷ 5. *Negativos*: clubes com saldo abaixo de zero no fim da temporada, média das temporadas. *Vendas*: leilões da máquina vendidos por liga e temporada.

| Cenário | Saldo 1ª fim da 1ª | Saldo 1ª fim da 6ª | Ganho por temporada 1ª | 1ª topo / meio / fundo (fim) | Saldo 2ª fim da 1ª | Saldo 2ª fim da 6ª | Ganho por temporada 2ª | 2ª topo / meio / fundo (fim) | Negativos 1ª / 2ª | Vendas 1ª temporada | Vendas 2ª a 6ª | Sem lance 2ª a 6ª | Ligas travadas |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Atual (rev. 119): salário 35–44 +10%, 45–50 +20%; líder 75% da torcida | 16,3 | 32,3 | 3,2 | 57,8 / 19,6 / 17,6 | 20,2 | 37,2 | 3,4 | 46,6 / 43,3 / 19,6 | 1% / 2% | 37,7 | 24,6 | 0,0 | 0 de 18 |
| Salário 35–44 +20%, 45–50 +35% | 13,9 | 21,7 | 1,6 | 35,3 / 16,4 / 12,3 | 17,9 | 27,0 | 1,8 | 33,2 / 33,0 / 13,1 | 1% / 2% | 37,9 | 30,8 | 1,2 | 0 de 18 |
| Salário 35–44 +25%, 45–50 +50% | 11,7 | 16,3 | 0,9 | 24,4 / 13,6 / 10,1 | 17,7 | 20,6 | 0,6 | 27,6 / 23,2 / 9,8 | 2% / 2% | 33,0 | 35,0 | 10,9 | 0 de 18 |
| Líder 65% da torcida | 11,7 | 7,3 | -0,9 | 32,5 / -1,1 / -11,8 | 16,9 | 12,1 | -1,0 | 17,7 / 13,7 / 3,9 | 21% / 4% | 33,7 | 31,8 | 268,5 | 0 de 18 |
| Líder 65% e salário 35–44 +20%, 45–50 +35% | 8,3 | -3,9 | -2,4 | 16,4 / -9,8 / -20,2 | 16,7 | 8,7 | -1,6 | 12,4 / 11,8 / 1,0 | 36% / 6% | 27,4 | 29,2 | 441,6 | 0 de 18 |

## Saldo médio por temporada (rodadas da folha)

| Cenário | Divisão | 1ª | 2ª | 3ª | 4ª | 5ª | 6ª |
|---|---|---|---|---|---|---|---|
| Atual (rev. 119): salário 35–44 +10%, 45–50 +20%; líder 75% da torcida | 1ª | 16,3 (0% neg.) | 20,2 (0% neg.) | 23,5 (1% neg.) | 27,7 (1% neg.) | 28,7 (1% neg.) | 32,3 (1% neg.) |
| Atual (rev. 119): salário 35–44 +10%, 45–50 +20%; líder 75% da torcida | 2ª | 20,2 (0% neg.) | 28,2 (0% neg.) | 32,1 (0% neg.) | 34,1 (0% neg.) | 37,5 (4% neg.) | 37,2 (5% neg.) |
| Salário 35–44 +20%, 45–50 +35% | 1ª | 13,9 (1% neg.) | 17,1 (0% neg.) | 18,3 (1% neg.) | 20,2 (1% neg.) | 20,5 (1% neg.) | 21,7 (2% neg.) |
| Salário 35–44 +20%, 45–50 +35% | 2ª | 17,9 (0% neg.) | 23,7 (0% neg.) | 26,4 (0% neg.) | 26,8 (1% neg.) | 26,7 (5% neg.) | 27,0 (7% neg.) |
| Salário 35–44 +25%, 45–50 +50% | 1ª | 11,7 (6% neg.) | 14,8 (0% neg.) | 16,0 (1% neg.) | 15,9 (3% neg.) | 16,3 (3% neg.) | 16,3 (2% neg.) |
| Salário 35–44 +25%, 45–50 +50% | 2ª | 17,7 (0% neg.) | 20,5 (0% neg.) | 22,1 (0% neg.) | 24,1 (1% neg.) | 23,7 (3% neg.) | 20,6 (6% neg.) |
| Líder 65% da torcida | 1ª | 11,7 (9% neg.) | 14,8 (4% neg.) | 13,2 (15% neg.) | 11,7 (23% neg.) | 9,4 (33% neg.) | 7,3 (41% neg.) |
| Líder 65% da torcida | 2ª | 16,9 (0% neg.) | 16,1 (0% neg.) | 16,1 (1% neg.) | 15,6 (2% neg.) | 13,8 (8% neg.) | 12,1 (14% neg.) |
| Líder 65% e salário 35–44 +20%, 45–50 +35% | 1ª | 8,3 (20% neg.) | 8,4 (22% neg.) | 5,9 (29% neg.) | 3,0 (43% neg.) | -1,0 (49% neg.) | -3,9 (54% neg.) |
| Líder 65% e salário 35–44 +20%, 45–50 +35% | 2ª | 16,7 (0% neg.) | 15,8 (0% neg.) | 15,4 (1% neg.) | 13,6 (5% neg.) | 12,0 (9% neg.) | 8,7 (19% neg.) |

## Saldo do terço de baixo por temporada (rodadas da folha)

| Cenário | Divisão | 1ª | 2ª | 3ª | 4ª | 5ª | 6ª |
|---|---|---|---|---|---|---|---|
| Atual (rev. 119): salário 35–44 +10%, 45–50 +20%; líder 75% da torcida | 1ª | 12,9 | 13,4 | 14,7 | 19,7 | 15,0 | 17,6 |
| Atual (rev. 119): salário 35–44 +10%, 45–50 +20%; líder 75% da torcida | 2ª | 13,1 | 15,2 | 16,8 | 17,2 | 17,5 | 19,6 |
| Salário 35–44 +20%, 45–50 +35% | 1ª | 11,3 | 12,3 | 11,4 | 15,3 | 12,5 | 12,3 |
| Salário 35–44 +20%, 45–50 +35% | 2ª | 13,3 | 13,6 | 15,0 | 15,5 | 13,1 | 13,1 |
| Salário 35–44 +25%, 45–50 +50% | 1ª | 8,9 | 12,0 | 11,6 | 10,7 | 11,0 | 10,1 |
| Salário 35–44 +25%, 45–50 +50% | 2ª | 13,0 | 14,7 | 14,8 | 14,3 | 13,1 | 9,8 |
| Líder 65% da torcida | 1ª | 5,8 | 8,4 | 1,8 | -1,9 | -6,5 | -11,8 |
| Líder 65% da torcida | 2ª | 13,1 | 12,9 | 11,8 | 11,2 | 8,1 | 3,9 |
| Líder 65% e salário 35–44 +20%, 45–50 +35% | 1ª | 0,9 | 1,9 | -6,0 | -8,5 | -17,0 | -20,2 |
| Líder 65% e salário 35–44 +20%, 45–50 +35% | 2ª | 12,6 | 12,6 | 11,9 | 9,1 | 7,3 | 1,0 |

## Saldo do terço de cima por temporada (rodadas da folha)

| Cenário | Divisão | 1ª | 2ª | 3ª | 4ª | 5ª | 6ª |
|---|---|---|---|---|---|---|---|
| Atual (rev. 119): salário 35–44 +10%, 45–50 +20%; líder 75% da torcida | 1ª | 21,4 | 30,0 | 36,6 | 42,5 | 50,3 | 57,8 |
| Atual (rev. 119): salário 35–44 +10%, 45–50 +20%; líder 75% da torcida | 2ª | 25,0 | 40,0 | 45,3 | 44,3 | 49,1 | 46,6 |
| Salário 35–44 +20%, 45–50 +35% | 1ª | 18,5 | 23,8 | 27,0 | 29,5 | 32,6 | 35,3 |
| Salário 35–44 +20%, 45–50 +35% | 2ª | 21,6 | 32,2 | 35,7 | 33,2 | 32,8 | 33,2 |
| Salário 35–44 +25%, 45–50 +50% | 1ª | 16,0 | 18,8 | 21,9 | 22,5 | 23,3 | 24,4 |
| Salário 35–44 +25%, 45–50 +50% | 2ª | 21,6 | 25,1 | 27,6 | 30,0 | 28,7 | 27,6 |
| Líder 65% da torcida | 1ª | 18,7 | 23,4 | 26,0 | 26,5 | 29,7 | 32,5 |
| Líder 65% da torcida | 2ª | 20,9 | 19,2 | 20,6 | 19,9 | 18,7 | 17,7 |
| Líder 65% e salário 35–44 +20%, 45–50 +35% | 1ª | 16,3 | 17,9 | 18,5 | 18,6 | 17,8 | 16,4 |
| Líder 65% e salário 35–44 +20%, 45–50 +35% | 2ª | 20,8 | 19,1 | 19,1 | 16,5 | 15,7 | 12,4 |

## Compras e vendas por terço da tabela (por liga e temporada)

*Compras*: jogadores comprados em leilão pelos clubes da divisão, pelo terço do comprador na hora. *Vendas*: pelo terço do vendedor.

| Cenário | Divisão | Compras topo / meio / fundo | Vendas topo / meio / fundo |
|---|---|---|---|
| Atual (rev. 119): salário 35–44 +10%, 45–50 +20%; líder 75% da torcida | 1ª | 9,1 / 5,7 / 2,1 | 0,3 / 3,6 / 12,4 |
| Atual (rev. 119): salário 35–44 +10%, 45–50 +20%; líder 75% da torcida | 2ª | 8,5 / 1,3 / 0,2 | 0,4 / 1,7 / 8,4 |
| Salário 35–44 +20%, 45–50 +35% | 1ª | 9,6 / 4,9 / 2,1 | 0,6 / 4,9 / 15,4 |
| Salário 35–44 +20%, 45–50 +35% | 2ª | 12,9 / 2,2 / 0,3 | 0,5 / 1,6 / 9,1 |
| Salário 35–44 +25%, 45–50 +50% | 1ª | 10,0 / 4,8 / 1,7 | 1,0 / 5,4 / 16,2 |
| Salário 35–44 +25%, 45–50 +50% | 2ª | 14,4 / 3,3 / 0,5 | 0,8 / 1,9 / 9,3 |
| Líder 65% da torcida | 1ª | 9,1 / 3,1 / 1,4 | 1,2 / 3,1 / 10,2 |
| Líder 65% da torcida | 2ª | 14,9 / 3,2 / 0,5 | 1,8 / 3,9 / 12,0 |
| Líder 65% e salário 35–44 +20%, 45–50 +35% | 1ª | 7,8 / 2,4 / 1,3 | 1,3 / 2,5 / 7,2 |
| Líder 65% e salário 35–44 +20%, 45–50 +35% | 2ª | 13,4 / 3,4 / 0,6 | 2,2 / 4,0 / 11,6 |
