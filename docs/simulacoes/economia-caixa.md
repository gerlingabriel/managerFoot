# Simulação do caixa da rodada

Gerado por `RelatorioEconomiaTest` (ferramenta de desenvolvimento). As 6 ligas do banco padrão, 30 temporadas cada (sementes 0 a 29), cada uma começando do zero: saldo inicial, moral inicial, sem promoção. Valores em Real; parâmetros de `ParametrosEconomia.padrao()` (proposta da rev. 37).

Só o caixa da rodada: bilheteria, custo do jogo, deslocamento e salários. Ainda não há patrocínio, premiação, mercado nem empréstimos.

## Tabelas propostas

| Força | Estádio | Salário por rodada | Folha de 23 por rodada (todos nessa força) |
|---|---|---|---|
| 12 | 5.000 | R$ 989 | R$ 22.745 |
| 17 | 7.000 | R$ 1.700 | R$ 39.100 |
| 22 | 10.500 | R$ 2.407 | R$ 55.354 |
| 26 | 14.000 | R$ 3.739 | R$ 86.003 |
| 30 | 18.500 | R$ 4.938 | R$ 113.577 |
| 35 | 27.000 | R$ 7.690 | R$ 176.871 |
| 40 | 38.500 | R$ 10.887 | R$ 250.398 |
| 45 | 55.500 | R$ 16.814 | R$ 386.717 |
| 50 | 80.000 | R$ 23.803 | R$ 547.478 |

| Divisão | Ingresso | Deslocamento por jogo fora |
|---|---|---|
| 1ª | R$ 40 | R$ 60.000 |
| 2ª | R$ 40 | R$ 30.000 |
| 3ª | R$ 40 | R$ 15.000 |

Ocupação: 45% com moral inicial e adversário de mesma força; moral máxima soma 20% e moral zero tira 30%; adversário 20 pontos mais forte soma 15%; sorteio de ±8%. Custo do jogo em casa: 15% da bilheteria.

## Por divisão (média por clube e temporada)

| Divisão | Forças | Público médio | Ocupação | Bilheteria por jogo | Receita líquida | Folha | Resultado | Fecham no vermelho |
|---|---|---|---|---|---|---|---|---|
| 1ª | 35–50 | 17.786 | 39% | R$ 711.450 | R$ 10.183.733 | R$ 9.100.600 | R$ 1.083.133 | 35% |
| 2ª | 23–30 | 5.710 | 39% | R$ 228.413 | R$ 3.068.788 | R$ 2.457.430 | R$ 611.358 | 28% |

## Resultado da temporada por terço de força dentro da divisão

| Divisão | Mais fortes | Medianos | Mais fracos | Saldo final mínimo |
|---|---|---|---|---|
| 1ª | R$ -987.276 | R$ 1.916.932 | R$ 2.491.495 | R$ -5.642.091 |
| 2ª | R$ 539.948 | R$ 513.307 | R$ 804.355 | R$ -1.114.365 |

*Receita líquida* é bilheteria menos custo do jogo e deslocamento. *Resultado* é a receita líquida menos a folha. *Fecham no vermelho*: clubes cujo resultado da temporada é negativo (o saldo inicial cobre 10 rodadas de folha).

## Mercado: preço do jogador (proposta da rev. 41)

Preço da compra direta = 80 rodadas do salário do jogador (cerca de duas temporadas de 38 rodadas), arredondado em R$ 1.000. O leilão começa 20% abaixo e o estrangeiro custa 25% menos; o jogador com asterisco custa 25% mais (regras do usuário). A tabela é do jogador nacional sem asterisco.

| Força | Salário por rodada | Preço (compra direta) | Lance inicial do leilão |
|---|---|---|---|
| 5 | R$ 608 | R$ 69.000 | R$ 55.000 |
| 12 | R$ 989 | R$ 113.000 | R$ 90.000 |
| 17 | R$ 1.700 | R$ 160.000 | R$ 128.000 |
| 22 | R$ 2.407 | R$ 227.000 | R$ 182.000 |
| 26 | R$ 3.739 | R$ 299.000 | R$ 239.000 |
| 30 | R$ 4.938 | R$ 395.000 | R$ 316.000 |
| 35 | R$ 7.690 | R$ 559.000 | R$ 447.000 |
| 40 | R$ 10.887 | R$ 792.000 | R$ 634.000 |
| 45 | R$ 16.814 | R$ 1.121.000 | R$ 897.000 |
| 50 | R$ 23.803 | R$ 1.587.000 | R$ 1.270.000 |

### Quantos jogadores da própria força o clube compra

Por terço de força dentro da divisão: força média do terço, preço de um jogador dessa força, quantos o saldo inicial paga e quantos o resultado de uma temporada (só o caixa da rodada) paga.

| Divisão | Terço | Força média | Preço | Saldo inicial compra | Resultado da temporada compra |
|---|---|---|---|---|---|
| 1ª | Mais fortes | 47,5 | R$ 1.288.000 | 2,7 | nenhum (resultado negativo) |
| 1ª | Medianos | 42,3 | R$ 910.000 | 2,4 | 2,1 |
| 1ª | Mais fracos | 37,2 | R$ 643.000 | 2,4 | 3,9 |
| 2ª | Mais fortes | 29,1 | R$ 369.000 | 2,2 | 1,5 |
| 2ª | Medianos | 26,6 | R$ 321.000 | 2,1 | 1,6 |
| 2ª | Mais fracos | 24,0 | R$ 260.000 | 1,9 | 3,1 |

Vender um jogador rende o preço (ou o lance do leilão) e tira o salário dele da folha. Ainda fora: patrocínio, premiação e regra de lance da máquina.
