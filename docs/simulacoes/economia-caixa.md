# Simulação do caixa da rodada

Gerado por `RelatorioEconomiaTest` (ferramenta de desenvolvimento). As 6 ligas do banco padrão, 30 temporadas cada (sementes 0 a 29), cada uma começando do zero: saldo inicial, moral inicial, sem promoção. Valores em Real; parâmetros de `ParametrosEconomia.padrao()` (proposta da rev. 37).

Só o caixa da rodada: bilheteria, custo do jogo, deslocamento e salários. Ainda não há patrocínio, premiação, mercado nem empréstimos.

## Tabelas propostas

| Força | Estádio | Salário por rodada | Folha de 23 por rodada (todos nessa força) |
|---|---|---|---|
| 12 | 5.000 | R$ 1.271 | R$ 29.243 |
| 17 | 7.000 | R$ 1.900 | R$ 43.700 |
| 22 | 10.500 | R$ 2.690 | R$ 61.867 |
| 26 | 14.000 | R$ 3.739 | R$ 86.003 |
| 30 | 18.500 | R$ 4.938 | R$ 113.577 |
| 35 | 27.000 | R$ 7.341 | R$ 168.832 |
| 40 | 38.500 | R$ 10.392 | R$ 239.016 |
| 45 | 55.500 | R$ 15.413 | R$ 354.490 |
| 50 | 80.000 | R$ 21.820 | R$ 501.855 |

| Divisão | Ingresso | Deslocamento por jogo fora |
|---|---|---|
| 1ª | R$ 40 | R$ 60.000 |
| 2ª | R$ 40 | R$ 30.000 |
| 3ª | R$ 40 | R$ 15.000 |

Ocupação: 45% com moral inicial e adversário de mesma força; moral máxima soma 20% e moral zero tira 30%; adversário 20 pontos mais forte soma 15%; sorteio de ±8%. Custo do jogo em casa: 15% da bilheteria.

## Por divisão (média por clube e temporada)

| Divisão | Forças | Público médio | Ocupação | Bilheteria por jogo | Receita líquida | Folha | Resultado | Fecham no vermelho |
|---|---|---|---|---|---|---|---|---|
| 1ª | 35–50 | 17.963 | 39% | R$ 718.524 | R$ 10.296.135 | R$ 8.442.824 | R$ 1.853.311 | 28% |
| 2ª | 23–30 | 5.715 | 39% | R$ 228.596 | R$ 3.071.699 | R$ 2.488.681 | R$ 583.018 | 29% |

## Resultado da temporada por terço de força dentro da divisão

| Divisão | Mais fortes | Medianos | Mais fracos | Saldo final mínimo |
|---|---|---|---|---|
| 1ª | R$ 432.094 | R$ 2.434.825 | R$ 2.809.639 | R$ -4.712.140 |
| 2ª | R$ 529.781 | R$ 582.966 | R$ 643.710 | R$ -885.916 |

*Receita líquida* é bilheteria menos custo do jogo e deslocamento. *Resultado* é a receita líquida menos a folha. *Fecham no vermelho*: clubes cujo resultado da temporada é negativo (o saldo inicial cobre 10 rodadas de folha).

## Mercado: preço do jogador (proposta da rev. 41)

Preço da compra direta = 80 rodadas do salário do jogador (cerca de duas temporadas de 38 rodadas), arredondado em R$ 1.000. O leilão começa 20% abaixo e o estrangeiro custa 25% menos; o jogador com asterisco custa 25% mais (regras do usuário). A tabela é do jogador nacional sem asterisco.

| Força | Salário por rodada | Preço (compra direta) | Lance inicial do leilão |
|---|---|---|---|
| 5 | R$ 782 | R$ 63.000 | R$ 50.000 |
| 12 | R$ 1.271 | R$ 102.000 | R$ 82.000 |
| 17 | R$ 1.900 | R$ 152.000 | R$ 122.000 |
| 22 | R$ 2.690 | R$ 215.000 | R$ 172.000 |
| 26 | R$ 3.739 | R$ 299.000 | R$ 239.000 |
| 30 | R$ 4.938 | R$ 395.000 | R$ 316.000 |
| 35 | R$ 7.341 | R$ 587.000 | R$ 470.000 |
| 40 | R$ 10.392 | R$ 831.000 | R$ 665.000 |
| 45 | R$ 15.413 | R$ 1.233.000 | R$ 986.000 |
| 50 | R$ 21.820 | R$ 1.746.000 | R$ 1.397.000 |

### Quantos jogadores da própria força o clube compra

Por terço de força dentro da divisão: força média do terço, preço de um jogador dessa força, quantos o saldo inicial paga e quantos o resultado de uma temporada (só o caixa da rodada) paga.

| Divisão | Terço | Força média | Preço | Saldo inicial compra | Resultado da temporada compra |
|---|---|---|---|---|---|
| 1ª | Mais fortes | 47,6 | R$ 1.519.000 | 2,1 | 0,3 |
| 1ª | Medianos | 42,2 | R$ 955.000 | 2,2 | 2,5 |
| 1ª | Mais fracos | 37,0 | R$ 675.000 | 2,1 | 4,2 |
| 2ª | Mais fortes | 29,1 | R$ 369.000 | 2,1 | 1,4 |
| 2ª | Medianos | 26,4 | R$ 299.000 | 2,2 | 1,9 |
| 2ª | Mais fracos | 24,1 | R$ 247.000 | 2,2 | 2,6 |

Vender um jogador rende o preço (ou o lance do leilão) e tira o salário dele da folha. Ainda fora: patrocínio, premiação e regra de lance da máquina.
