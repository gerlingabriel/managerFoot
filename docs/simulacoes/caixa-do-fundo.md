# Caixa do fundo da tabela: público do último e deslocamento

Gerado por `RelatorioCicloMercadoTest` com `RELATORIO_CAIXA_FUNDO=1` (ferramenta de desenvolvimento; SPEC §15, Etapa 6). O mesmo simulador de `ciclo-do-mercado.md`, com o padrão atual (faixas da rev. 66, plantel até 23): as 6 ligas do banco padrão, 3 sementes, 6 temporadas com promoção e rebaixamento, só máquina, leilões da máquina antes de cada rodada. Sem copa, continental e Mundial (o jogo completo tem mais jogos em casa). Ingresso de R$ 30 em todas as linhas. Cada linha muda a ocupação do último colocado (fração da torcida; o líder fica com 75%) e o deslocamento da 2ª e da 3ª divisão (a 1ª fica com R$ 60 mil).

*Saldo*: média dos clubes da divisão no fim da temporada, em rodadas da folha do próprio clube (o jogo começa com 10). *Ganho por temporada*: (saldo no fim da 6ª − no fim da 1ª) ÷ 5. *Negativos*: clubes com saldo abaixo de zero no fim da temporada, média das temporadas. *Vendas*: leilões da máquina vendidos por liga e temporada.

| Cenário | Saldo 1ª fim da 1ª | Saldo 1ª fim da 6ª | Ganho por temporada 1ª | 1ª topo / meio / fundo (fim) | Saldo 2ª fim da 1ª | Saldo 2ª fim da 6ª | Ganho por temporada 2ª | 2ª topo / meio / fundo (fim) | Negativos 1ª / 2ª | Vendas 1ª temporada | Vendas 2ª a 6ª | Sem lance 2ª a 6ª | Ligas travadas |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Atual: último 15% da torcida, deslocamento R$ 60/30/15 mil | 14,7 | 23,5 | 1,8 | 51,0 / 13,8 / 3,4 | 17,3 | 11,0 | -1,3 | 23,2 / 13,0 / -5,0 | 6% / 8% | 45,2 | 35,4 | 84,8 | 0 de 18 |
| Só deslocamento da 2ª e 3ª pela metade | 15,8 | 30,2 | 2,9 | 55,2 / 17,8 / 15,8 | 19,6 | 33,0 | 2,7 | 37,2 / 42,6 / 17,4 | 1% / 1% | 40,8 | 27,2 | 0,0 | 0 de 18 |
| Último 18% | 16,0 | 30,3 | 2,9 | 55,4 / 20,4 / 13,1 | 18,8 | 19,3 | 0,1 | 29,9 / 25,2 / 0,5 | 1% / 5% | 42,1 | 30,4 | 7,0 | 0 de 18 |
| Último 18% e deslocamento da 2ª e 3ª −25% | 16,3 | 32,3 | 3,2 | 57,8 / 19,6 / 17,6 | 20,2 | 37,2 | 3,4 | 46,6 / 43,3 / 19,6 | 1% / 2% | 37,7 | 24,6 | 0,0 | 0 de 18 |
| Último 20% e deslocamento da 2ª e 3ª −25% | 16,4 | 36,3 | 4,0 | 61,2 / 20,3 / 26,2 | 21,8 | 45,8 | 4,8 | 59,3 / 54,0 / 21,1 | 0% / 1% | 33,7 | 20,6 | 0,0 | 0 de 18 |
| Último 25% e deslocamento da 2ª e 3ª pela metade | 18,0 | 55,1 | 7,4 | 68,0 / 37,1 / 60,8 | 26,6 | 72,8 | 9,3 | 102,3 / 76,7 / 34,8 | 0% / 0% | 21,7 | 7,7 | 0,0 | 0 de 18 |
| Último 35% e deslocamento da 2ª e 3ª pela metade | 21,2 | 82,9 | 12,3 | 101,4 / 71,0 / 75,2 | 29,9 | 93,1 | 12,7 | 119,3 / 108,1 / 46,4 | 0% / 0% | 7,1 | 1,7 | 0,0 | 0 de 18 |

## Saldo médio por temporada (rodadas da folha)

| Cenário | Divisão | 1ª | 2ª | 3ª | 4ª | 5ª | 6ª |
|---|---|---|---|---|---|---|---|
| Atual: último 15% da torcida, deslocamento R$ 60/30/15 mil | 1ª | 14,7 (2% neg.) | 18,8 (0% neg.) | 20,4 (2% neg.) | 21,8 (5% neg.) | 22,6 (13% neg.) | 23,5 (16% neg.) |
| Atual: último 15% da torcida, deslocamento R$ 60/30/15 mil | 2ª | 17,3 (0% neg.) | 19,2 (0% neg.) | 17,7 (4% neg.) | 15,9 (8% neg.) | 13,0 (15% neg.) | 11,0 (19% neg.) |
| Só deslocamento da 2ª e 3ª pela metade | 1ª | 15,8 (0% neg.) | 19,3 (0% neg.) | 21,9 (1% neg.) | 24,9 (2% neg.) | 26,8 (3% neg.) | 30,2 (1% neg.) |
| Só deslocamento da 2ª e 3ª pela metade | 2ª | 19,6 (0% neg.) | 26,8 (0% neg.) | 30,6 (0% neg.) | 33,6 (0% neg.) | 34,8 (1% neg.) | 33,0 (4% neg.) |
| Último 18% | 1ª | 16,0 (0% neg.) | 20,1 (0% neg.) | 22,7 (0% neg.) | 25,4 (1% neg.) | 27,5 (1% neg.) | 30,3 (1% neg.) |
| Último 18% | 2ª | 18,8 (0% neg.) | 23,2 (0% neg.) | 24,4 (2% neg.) | 23,3 (5% neg.) | 21,9 (8% neg.) | 19,3 (14% neg.) |
| Último 18% e deslocamento da 2ª e 3ª −25% | 1ª | 16,3 (0% neg.) | 20,2 (0% neg.) | 23,5 (1% neg.) | 27,7 (1% neg.) | 28,7 (1% neg.) | 32,3 (1% neg.) |
| Último 18% e deslocamento da 2ª e 3ª −25% | 2ª | 20,2 (0% neg.) | 28,2 (0% neg.) | 32,1 (0% neg.) | 34,1 (0% neg.) | 37,5 (4% neg.) | 37,2 (5% neg.) |
| Último 20% e deslocamento da 2ª e 3ª −25% | 1ª | 16,4 (0% neg.) | 21,4 (0% neg.) | 25,9 (1% neg.) | 30,8 (0% neg.) | 32,0 (0% neg.) | 36,3 (1% neg.) |
| Último 20% e deslocamento da 2ª e 3ª −25% | 2ª | 21,8 (0% neg.) | 30,2 (0% neg.) | 35,2 (0% neg.) | 39,0 (1% neg.) | 44,5 (2% neg.) | 45,8 (5% neg.) |
| Último 25% e deslocamento da 2ª e 3ª pela metade | 1ª | 18,0 (0% neg.) | 26,1 (0% neg.) | 33,6 (0% neg.) | 42,2 (0% neg.) | 47,5 (0% neg.) | 55,1 (0% neg.) |
| Último 25% e deslocamento da 2ª e 3ª pela metade | 2ª | 26,6 (0% neg.) | 39,2 (0% neg.) | 47,5 (0% neg.) | 54,5 (0% neg.) | 65,0 (0% neg.) | 72,8 (0% neg.) |
| Último 35% e deslocamento da 2ª e 3ª pela metade | 1ª | 21,2 (0% neg.) | 33,6 (0% neg.) | 45,5 (0% neg.) | 58,3 (0% neg.) | 69,7 (0% neg.) | 82,9 (0% neg.) |
| Último 35% e deslocamento da 2ª e 3ª pela metade | 2ª | 29,9 (0% neg.) | 44,7 (0% neg.) | 57,1 (0% neg.) | 69,6 (0% neg.) | 82,2 (0% neg.) | 93,1 (0% neg.) |

## Saldo do terço de baixo por temporada (rodadas da folha)

| Cenário | Divisão | 1ª | 2ª | 3ª | 4ª | 5ª | 6ª |
|---|---|---|---|---|---|---|---|
| Atual: último 15% da torcida, deslocamento R$ 60/30/15 mil | 1ª | 9,6 | 11,5 | 8,9 | 8,3 | 3,2 | 3,4 |
| Atual: último 15% da torcida, deslocamento R$ 60/30/15 mil | 2ª | 13,0 | 12,9 | 10,1 | 5,4 | -1,0 | -5,0 |
| Só deslocamento da 2ª e 3ª pela metade | 1ª | 12,6 | 12,9 | 12,3 | 15,3 | 12,1 | 15,8 |
| Só deslocamento da 2ª e 3ª pela metade | 2ª | 13,7 | 15,0 | 16,7 | 16,4 | 20,2 | 17,4 |
| Último 18% | 1ª | 12,4 | 12,7 | 13,1 | 13,6 | 11,1 | 13,1 |
| Último 18% | 2ª | 13,4 | 14,1 | 12,9 | 10,0 | 9,0 | 0,5 |
| Último 18% e deslocamento da 2ª e 3ª −25% | 1ª | 12,9 | 13,4 | 14,7 | 19,7 | 15,0 | 17,6 |
| Último 18% e deslocamento da 2ª e 3ª −25% | 2ª | 13,1 | 15,2 | 16,8 | 17,2 | 17,5 | 19,6 |
| Último 20% e deslocamento da 2ª e 3ª −25% | 1ª | 12,9 | 15,2 | 19,2 | 26,0 | 21,9 | 26,2 |
| Último 20% e deslocamento da 2ª e 3ª −25% | 2ª | 13,7 | 16,1 | 17,6 | 18,3 | 19,3 | 21,1 |
| Último 25% e deslocamento da 2ª e 3ª pela metade | 1ª | 13,2 | 22,0 | 28,9 | 42,8 | 47,2 | 60,8 |
| Último 25% e deslocamento da 2ª e 3ª pela metade | 2ª | 16,6 | 20,4 | 24,5 | 26,7 | 28,2 | 34,8 |
| Último 35% e deslocamento da 2ª e 3ª pela metade | 1ª | 14,9 | 29,8 | 39,1 | 52,1 | 57,9 | 75,2 |
| Último 35% e deslocamento da 2ª e 3ª pela metade | 2ª | 20,1 | 30,9 | 34,3 | 44,4 | 46,2 | 46,4 |

## Compras e vendas por terço da tabela (por liga e temporada)

*Compras*: jogadores comprados em leilão pelos clubes da divisão, pelo terço do comprador na hora. *Vendas*: pelo terço do vendedor.

| Cenário | Divisão | Compras topo / meio / fundo | Vendas topo / meio / fundo |
|---|---|---|---|
| Atual: último 15% da torcida, deslocamento R$ 60/30/15 mil | 1ª | 10,1 / 4,6 / 1,4 | 0,5 / 3,5 / 15,8 |
| Atual: último 15% da torcida, deslocamento R$ 60/30/15 mil | 2ª | 17,2 / 3,2 / 0,6 | 1,5 / 3,6 / 12,1 |
| Só deslocamento da 2ª e 3ª pela metade | 1ª | 9,2 / 5,3 / 1,9 | 0,4 / 4,4 / 14,6 |
| Só deslocamento da 2ª e 3ª pela metade | 2ª | 10,9 / 1,9 / 0,3 | 0,4 / 1,4 / 8,3 |
| Último 18% | 1ª | 8,9 / 5,6 / 2,2 | 0,3 / 3,7 / 15,1 |
| Último 18% | 2ª | 12,7 / 2,5 / 0,5 | 0,6 / 2,2 / 10,5 |
| Último 18% e deslocamento da 2ª e 3ª −25% | 1ª | 9,1 / 5,7 / 2,1 | 0,3 / 3,6 / 12,4 |
| Último 18% e deslocamento da 2ª e 3ª −25% | 2ª | 8,5 / 1,3 / 0,2 | 0,4 / 1,7 / 8,4 |
| Último 20% e deslocamento da 2ª e 3ª −25% | 1ª | 8,2 / 5,4 / 2,2 | 0,2 / 3,2 / 10,2 |
| Último 20% e deslocamento da 2ª e 3ª −25% | 2ª | 6,2 / 0,6 / 0,1 | 0,3 / 1,4 / 7,5 |
| Último 25% e deslocamento da 2ª e 3ª pela metade | 1ª | 5,1 / 3,5 / 0,9 | 0,1 / 1,1 / 4,7 |
| Último 25% e deslocamento da 2ª e 3ª pela metade | 2ª | 0,5 / 0,0 / 0,0 | 0,2 / 0,4 / 3,5 |
| Último 35% e deslocamento da 2ª e 3ª pela metade | 1ª | 1,9 / 0,5 / 0,2 | 0,0 / 0,2 / 1,5 |
| Último 35% e deslocamento da 2ª e 3ª pela metade | 2ª | 0,0 / 0,0 / 0,0 | 0,1 / 0,2 / 0,6 |
