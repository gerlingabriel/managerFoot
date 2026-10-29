# Ciclo do mercado: quem vende, quem compra

Gerado por `RelatorioCicloMercadoTest` (ferramenta de desenvolvimento). As 6 ligas do banco padrão, 3 sementes, 6 temporadas seguidas com promoção e rebaixamento. Economia atual: público pela posição e pela força (§9.2), estádio com ampliação da máquina, evolução de força (§6.1), salário da máquina pela força atual com as faixas da rev. 61, prêmio da liga e patrocínio fixo pela posição. Antes de cada rodada, os leilões da máquina (`LeiloesDaRodada`, as regras da rev. 44): quem está abaixo de 10 rodadas da folha põe o mais caro sem asterisco em leilão; dá lance quem fica acima do limite mais 5 rodadas da folha depois de pagar, tem menos de 25 jogadores e o jogador tem força de pelo menos a do time menos 5. Só máquina.

## Alavanca "rico gasta mais" (rev. 64)

*Rico*: saldo de pelo menos N limites (1 limite = 10 rodadas da folha). *Compras*: quantas por rodada o rico pode fazer (os outros, 1). *Compra direta*: depois dos leilões, o rico compra pelo preço cheio o jogador mais forte que lhe interessa entre os 5 ou 6 que cada clube da máquina deixa à venda (§9.4), com a mesma folga. Saldo no fim da 6ª temporada, em limites, por terço final da tabela.

| Cenário | 1ª topo | 1ª fundo | 2ª topo | 2ª fundo | 1ª leilões sem lance | Compras diretas por temporada (liga) | Vendas do rico no fundo (por 100 clubes-rodada) | Mobilidade | Ligas travadas |
|---|---|---|---|---|---|---|---|---|---|
| Atual: 1 compra, só leilão | 21,7 | 7,4 | 10,9 | 1,5 | 26% | 0,0 | 2,7 | 5% | 0 de 18 |
| Rico (2 limites): 2 compras | 21,9 | 7,1 | 10,4 | 1,2 | 26% | 0,0 | 2,8 | 4% | 0 de 18 |
| Rico (2 limites): compra direta | 40,9 | 4,9 | 5,8 | -0,9 | 60% | 121,8 | 9,6 | 3% | 0 de 18 |
| Rico (1,5 limite): 3 compras e compra direta | 40,0 | 3,5 | 6,2 | -0,7 | 60% | 174,0 | 11,4 | 4% | 1 de 18 |
| Rico (2 limites): 2 compras e compra direta | 42,2 | 3,2 | 5,9 | -0,7 | 66% | 150,1 | 9,6 | 5% | 0 de 18 |

*Ligas travadas*: liga × semente em que um clube ficou sem 11 disponíveis (vendas até o plantel mínimo de 14 mais suspensões); a simulação dessa liga para ali e as medidas contam só as temporadas jogadas.

As tabelas abaixo são do último cenário (a proposta).

*Riqueza*: terço do clube na divisão pelo saldo dividido pelo limite, no início da temporada. *Terço da tabela*: posição na hora da venda.

## Quem vende: riqueza no início da temporada × posição na hora (vendas por 100 clubes-rodada)

| Riqueza | Topo | Meio | Fundo |
|---|---|---|---|
| Rico | 21,7 | 11,6 | 9,6 |
| Médio | 12,1 | 9,8 | 5,7 |
| Pobre | 14,5 | 7,7 | 6,1 |

## Para onde vão os jogadores: divisão do vendedor × do comprador (% das vendas)

| Vendedor \ comprador | 1ª | 2ª | 3ª |
|---|---|---|---|
| 1ª | 81% | 9% | 0% |
| 2ª | 0% | 10% | 0% |
| 3ª | 0% | 0% | 0% |

## Na mesma divisão: terço do vendedor × do comprador (% das vendas na mesma divisão)

| Vendedor \ comprador | Topo | Meio | Fundo |
|---|---|---|---|
| Topo | 35% | 18% | 3% |
| Meio | 16% | 10% | 3% |
| Fundo | 6% | 6% | 2% |

## Quem consegue comprar: clubes que dão lance por leilão aberto, por terço da tabela

% dos clubes do terço que deram lance em cada leilão do país (todos os clubes da máquina podem dar lance em qualquer leilão do país).

| Divisão | Leilões | Sem lance | Topo | Meio | Fundo | Pago / preço | Força do vendido − força do comprador |
|---|---|---|---|---|---|---|---|
| 1ª | 2114 | 66% | 0% | 0% | 0% | 100% | 2,4 |
| 2ª | 13378 | 91% | 1% | 1% | 0% | 95% | -0,9 |

*Leilões*: pela divisão do vendedor. *Topo/Meio/Fundo*: pela divisão de quem dá o lance.

## Caixa por temporada: saldo no fim em limites (1,0 = 10 rodadas da folha) por terço final

| Divisão | Temporada | Topo | Meio | Fundo | Clubes negativos | Jogadores trocados por clube |
|---|---|---|---|---|---|---|
| 1ª | 1 | 6,3 | 2,2 | 1,5 | 0% | 12,2 |
| 1ª | 2 | 14,2 | 4,9 | 1,4 | 0% | 15,9 |
| 1ª | 3 | 20,7 | 9,0 | 1,2 | 2% | 15,0 |
| 1ª | 4 | 27,7 | 12,6 | 2,1 | 1% | 14,8 |
| 1ª | 5 | 34,3 | 15,9 | 3,1 | 1% | 14,9 |
| 1ª | 6 | 42,2 | 18,3 | 3,2 | 1% | 15,1 |
| 2ª | 1 | 2,0 | 1,6 | 1,2 | 0% | 4,5 |
| 2ª | 2 | 2,5 | 1,8 | 0,7 | 5% | 4,0 |
| 2ª | 3 | 3,8 | 2,1 | 0,5 | 9% | 2,0 |
| 2ª | 4 | 4,2 | 2,5 | -0,0 | 16% | 1,8 |
| 2ª | 5 | 5,3 | 2,7 | -0,1 | 16% | 1,4 |
| 2ª | 6 | 5,9 | 3,1 | -0,7 | 19% | 1,0 |

## Mobilidade

Clubes que mudaram de terço de força em campo no país entre o início e o fim das 6 temporadas: 5%.
