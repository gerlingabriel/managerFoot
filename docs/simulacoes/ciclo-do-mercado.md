# Ciclo do mercado: quem vende, quem compra

Gerado por `RelatorioCicloMercadoTest` (ferramenta de desenvolvimento). As 6 ligas do banco padrão, 3 sementes, 6 temporadas seguidas com promoção e rebaixamento. Economia atual: público pela posição e pela força (§9.2), estádio com ampliação da máquina, evolução de força (§6.1), salário da máquina pela força atual com as faixas da rev. 61, prêmio da liga e patrocínio fixo pela posição. Antes de cada rodada, os leilões da máquina (`LeiloesDaRodada`, as regras da rev. 44): quem está abaixo de 10 rodadas da folha põe o mais caro sem asterisco em leilão; dá lance quem fica acima do limite mais 5 rodadas da folha depois de pagar, tem menos de 25 jogadores e o jogador tem força de pelo menos a do time menos 5. Só máquina.

## Inflação pela força do clube (rev. 65)

*Faixa*: o ajuste da rev. 61 (abaixo de 15 −10%, 15–24 −5%, 25–34 0, 35–44 +5%, 45–50 +10%; *faixas maiores*: 35–44 +20% e 45–50 +40%). *Preço pelo comprador*: o preço sai do salário sem faixa e leva a faixa da força em campo do clube que compra; o lance inicial do leilão (preço −20%) fica sem faixa. *Salário pelo clube*: todos os jogadores do clube recebem com a faixa da força em campo do clube. Prêmio e ampliação do estádio iguais em todos. Saldo no fim da 6ª temporada, em limites, por terço final da tabela.

| Cenário | 1ª topo | 1ª fundo | 2ª topo | 2ª fundo | 1ª leilões sem lance | 2ª leilões sem lance | Vendas do rico no fundo (por 100 clubes-rodada) | Mobilidade | Ligas travadas |
|---|---|---|---|---|---|---|---|---|---|
| Atual: faixa pelo jogador no salário e no preço | 21,7 | 7,4 | 10,9 | 1,5 | 26% | 22% | 2,7 | 5% | 0 de 18 |
| Preço pelo comprador; salário pelo jogador | 21,4 | 7,1 | 11,2 | 1,1 | 26% | 20% | 2,6 | 6% | 1 de 18 |
| Preço pelo comprador; salário pelo clube | 21,6 | 7,8 | 10,8 | 1,3 | 26% | 19% | 2,6 | 4% | 1 de 18 |
| Preço pelo comprador; salário pelo jogador; faixas maiores | 13,5 | 6,5 | 11,2 | 1,5 | 40% | 21% | 2,3 | 5% | 1 de 18 |
| Preço pelo comprador; salário pelo clube; faixas maiores | 13,8 | 6,9 | 10,5 | 1,0 | 43% | 19% | 2,3 | 5% | 0 de 18 |

*Ligas travadas*: liga × semente em que um clube ficou sem 11 disponíveis (vendas até o plantel mínimo de 14 mais suspensões); a simulação dessa liga para ali e as medidas contam só as temporadas jogadas.

As tabelas abaixo são do cenário "Preço pelo comprador; salário pelo clube".

*Riqueza*: terço do clube na divisão pelo saldo dividido pelo limite, no início da temporada. *Terço da tabela*: posição na hora da venda.

## Quem vende: riqueza no início da temporada × posição na hora (vendas por 100 clubes-rodada)

| Riqueza | Topo | Meio | Fundo |
|---|---|---|---|
| Rico | 0,0 | 0,1 | 2,6 |
| Médio | 0,0 | 0,1 | 1,7 |
| Pobre | 0,3 | 0,7 | 4,0 |

## Para onde vão os jogadores: divisão do vendedor × do comprador (% das vendas)

| Vendedor \ comprador | 1ª | 2ª | 3ª |
|---|---|---|---|
| 1ª | 26% | 3% | 0% |
| 2ª | 8% | 63% | 0% |
| 3ª | 0% | 0% | 0% |

## Na mesma divisão: terço do vendedor × do comprador (% das vendas na mesma divisão)

| Vendedor \ comprador | Topo | Meio | Fundo |
|---|---|---|---|
| Topo | 1% | 1% | 0% |
| Meio | 4% | 4% | 2% |
| Fundo | 29% | 46% | 13% |

## Quem consegue comprar: clubes que dão lance por leilão aberto, por terço da tabela

% dos clubes do terço que deram lance em cada leilão do país (todos os clubes da máquina podem dar lance em qualquer leilão do país).

| Divisão | Leilões | Sem lance | Topo | Meio | Fundo | Pago / preço | Força do vendido − força do comprador |
|---|---|---|---|---|---|---|---|
| 1ª | 615 | 26% | 2% | 7% | 11% | 98% | -3,0 |
| 2ª | 1373 | 19% | 32% | 34% | 15% | 90% | -3,4 |

*Leilões*: pela divisão do vendedor. *Topo/Meio/Fundo*: pela divisão de quem dá o lance.

## Caixa por temporada: saldo no fim em limites (1,0 = 10 rodadas da folha) por terço final

| Divisão | Temporada | Topo | Meio | Fundo | Clubes negativos | Jogadores trocados por clube |
|---|---|---|---|---|---|---|
| 1ª | 1 | 4,0 | 2,5 | 1,3 | 0% | 1,2 |
| 1ª | 2 | 7,3 | 4,1 | 2,6 | 0% | 0,5 |
| 1ª | 3 | 10,8 | 6,2 | 3,4 | 0% | 0,5 |
| 1ª | 4 | 14,0 | 7,9 | 5,7 | 0% | 0,3 |
| 1ª | 5 | 17,6 | 9,3 | 6,6 | 0% | 0,2 |
| 1ª | 6 | 21,6 | 10,7 | 7,8 | 0% | 0,1 |
| 2ª | 1 | 4,1 | 2,5 | 1,4 | 0% | 1,6 |
| 2ª | 2 | 6,6 | 3,5 | 1,5 | 0% | 1,3 |
| 2ª | 3 | 7,7 | 3,8 | 1,5 | 1% | 1,2 |
| 2ª | 4 | 8,8 | 4,3 | 1,5 | 4% | 0,9 |
| 2ª | 5 | 10,0 | 4,8 | 1,4 | 7% | 0,7 |
| 2ª | 6 | 10,8 | 5,7 | 1,3 | 10% | 0,4 |

## Mobilidade

Clubes que mudaram de terço de força em campo no país entre o início e o fim das 6 temporadas: 4%.
