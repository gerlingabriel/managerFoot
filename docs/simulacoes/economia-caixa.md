# Simulação do caixa da rodada

Gerado por `RelatorioEconomiaTest` (ferramenta de desenvolvimento). As 6 ligas do banco padrão, 30 temporadas cada (sementes 0 a 29), cada uma começando do zero: saldo inicial, moral inicial, sem promoção. Valores em Real; parâmetros de `ParametrosEconomia.padrao()` (proposta da rev. 37).

Só o caixa da rodada: bilheteria, custo do jogo, deslocamento e salários. Ainda não há patrocínio, premiação, mercado nem empréstimos.

## Tabelas propostas

| Força | Estádio | Salário por rodada | Folha de 23 por rodada (todos nessa força) |
|---|---|---|---|
| 12 | 5.000 | R$ 1.413 | R$ 32.493 |
| 17 | 7.000 | R$ 2.000 | R$ 46.000 |
| 22 | 10.500 | R$ 2.831 | R$ 65.123 |
| 26 | 14.000 | R$ 3.739 | R$ 86.003 |
| 30 | 18.500 | R$ 4.938 | R$ 113.577 |
| 35 | 27.000 | R$ 6.991 | R$ 160.792 |
| 40 | 38.500 | R$ 9.897 | R$ 227.634 |
| 45 | 55.500 | R$ 14.011 | R$ 322.264 |
| 50 | 80.000 | R$ 19.836 | R$ 456.232 |

| Divisão | Ingresso | Deslocamento por jogo fora |
|---|---|---|
| 1ª | R$ 40 | R$ 60.000 |
| 2ª | R$ 40 | R$ 30.000 |
| 3ª | R$ 40 | R$ 15.000 |

Ocupação: 45% com moral inicial e adversário de mesma força; moral máxima soma 20% e moral zero tira 30%; adversário 20 pontos mais forte soma 15%; sorteio de ±8%. Custo do jogo em casa: 15% da bilheteria.

## Por divisão (média por clube e temporada)

| Divisão | Forças | Público médio | Ocupação | Bilheteria por jogo | Receita líquida | Folha | Resultado | Fecham no vermelho |
|---|---|---|---|---|---|---|---|---|
| 1ª | 35–50 | 21.288 | 42% | R$ 851.537 | R$ 12.409.800 | R$ 8.899.307 | R$ 3.510.494 | 23% |
| 2ª | 22–30 | 6.116 | 42% | R$ 244.648 | R$ 3.326.773 | R$ 2.747.629 | R$ 579.144 | 37% |
| 3ª | 13–22 | 3.412 | 42% | R$ 136.485 | R$ 1.919.235 | R$ 1.590.570 | R$ 328.665 | 42% |

## Resultado da temporada por terço de força dentro da divisão

| Divisão | Mais fortes | Medianos | Mais fracos | Saldo final mínimo |
|---|---|---|---|---|
| 1ª | R$ 7.294.565 | R$ 2.600.900 | R$ 236.782 | R$ -637.133 |
| 2ª | R$ 1.409.877 | R$ 469.081 | R$ -241.620 | R$ -657.618 |
| 3ª | R$ 971.042 | R$ 228.008 | R$ -303.340 | R$ -387.715 |

*Receita líquida* é bilheteria menos custo do jogo e deslocamento. *Resultado* é a receita líquida menos a folha. *Fecham no vermelho*: clubes cujo resultado da temporada é negativo (o saldo inicial cobre 10 rodadas de folha).

## Mercado: preço do jogador (proposta da rev. 41)

Preço da compra direta = 80 rodadas do salário do jogador (cerca de duas temporadas de 38 rodadas), arredondado em R$ 1.000. O leilão começa 20% abaixo e o estrangeiro custa 10% menos (regras do usuário). A tabela é do jogador nacional.

| Força | Salário por rodada | Preço (compra direta) | Lance inicial do leilão |
|---|---|---|---|
| 5 | R$ 868 | R$ 69.000 | R$ 55.000 |
| 12 | R$ 1.413 | R$ 113.000 | R$ 90.000 |
| 17 | R$ 2.000 | R$ 160.000 | R$ 128.000 |
| 22 | R$ 2.831 | R$ 227.000 | R$ 182.000 |
| 26 | R$ 3.739 | R$ 299.000 | R$ 239.000 |
| 30 | R$ 4.938 | R$ 395.000 | R$ 316.000 |
| 35 | R$ 6.991 | R$ 559.000 | R$ 447.000 |
| 40 | R$ 9.897 | R$ 792.000 | R$ 634.000 |
| 45 | R$ 14.011 | R$ 1.121.000 | R$ 897.000 |
| 50 | R$ 19.836 | R$ 1.587.000 | R$ 1.270.000 |

### Quantos jogadores da própria força o clube compra

Por terço de força dentro da divisão: força média do terço, preço de um jogador dessa força, quantos o saldo inicial paga e quantos o resultado de uma temporada (só o caixa da rodada) paga.

| Divisão | Terço | Força média | Preço | Saldo inicial compra | Resultado da temporada compra |
|---|---|---|---|---|---|
| 1ª | Mais fortes | 47,6 | R$ 1.381.000 | 2,3 | 5,3 |
| 1ª | Medianos | 42,1 | R$ 910.000 | 2,4 | 2,9 |
| 1ª | Mais fracos | 37,2 | R$ 643.000 | 2,5 | 0,4 |
| 2ª | Mais fortes | 28,8 | R$ 369.000 | 2,4 | 3,8 |
| 2ª | Medianos | 26,0 | R$ 299.000 | 2,4 | 1,6 |
| 2ª | Mais fracos | 23,1 | R$ 243.000 | 2,4 | nenhum (resultado negativo) |
| 3ª | Mais fortes | 20,9 | R$ 211.000 | 2,4 | 4,6 |
| 3ª | Medianos | 17,7 | R$ 172.000 | 2,4 | 1,3 |
| 3ª | Mais fracos | 14,7 | R$ 139.000 | 2,4 | nenhum (resultado negativo) |

Vender um jogador rende o preço (ou o lance do leilão) e tira o salário dele da folha. Ainda fora: patrocínio, premiação e regra de lance da máquina.
