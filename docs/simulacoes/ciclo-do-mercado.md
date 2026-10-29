# Ciclo do mercado: quem vende, quem compra

Gerado por `RelatorioCicloMercadoTest` (ferramenta de desenvolvimento). As 6 ligas do banco padrão, 3 sementes, 6 temporadas seguidas com promoção e rebaixamento. Economia atual: público pela posição e pela força (§9.2), estádio com ampliação da máquina, evolução de força (§6.1), salário da máquina pela força atual, prêmio da liga e patrocínio fixo pela posição. Antes de cada rodada, os leilões da máquina (`LeiloesDaRodada`, as regras da rev. 44): quem está abaixo de 10 rodadas da folha põe o mais caro sem asterisco em leilão; dá lance quem fica acima do limite mais 5 rodadas da folha depois de pagar, tem menos de 25 jogadores e o jogador tem força de pelo menos a do time menos 5. Só máquina, sem compra direta.

*Riqueza*: terço do clube na divisão pelo saldo dividido pelo limite, no início da temporada. *Terço da tabela*: posição na hora da venda.

## Quem vende: riqueza no início da temporada × posição na hora (vendas por 100 clubes-rodada)

| Riqueza | Topo | Meio | Fundo |
|---|---|---|---|
| Rico | 0,0 | 0,3 | 3,1 |
| Médio | 0,1 | 0,5 | 2,1 |
| Pobre | 0,4 | 1,1 | 3,7 |

## Para onde vão os jogadores: divisão do vendedor × do comprador (% das vendas)

| Vendedor \ comprador | 1ª | 2ª | 3ª |
|---|---|---|---|
| 1ª | 19% | 8% | 1% |
| 2ª | 1% | 25% | 19% |
| 3ª | 0% | 4% | 24% |

## Na mesma divisão: terço do vendedor × do comprador (% das vendas na mesma divisão)

| Vendedor \ comprador | Topo | Meio | Fundo |
|---|---|---|---|
| Topo | 2% | 1% | 1% |
| Meio | 12% | 6% | 1% |
| Fundo | 32% | 37% | 8% |

## Quem consegue comprar: clubes que dão lance por leilão aberto, por terço da tabela

% dos clubes do terço que deram lance em cada leilão do país (todos os clubes da máquina podem dar lance em qualquer leilão do país).

| Divisão | Leilões | Sem lance | Topo | Meio | Fundo | Pago / preço | Força do vendido − força do comprador |
|---|---|---|---|---|---|---|---|
| 1ª | 1064 | 32% | 0% | 0% | 0% | 94% | -1,3 |
| 2ª | 6028 | 80% | 1% | 1% | 0% | 93% | -1,8 |
| 3ª | 19825 | 96% | 2% | 0% | 0% | 92% | -3,3 |

*Leilões*: pela divisão do vendedor. *Topo/Meio/Fundo*: pela divisão de quem dá o lance.

## Caixa por temporada: saldo no fim em limites (1,0 = 10 rodadas da folha) por terço final

| Divisão | Temporada | Topo | Meio | Fundo | Clubes negativos | Jogadores trocados por clube |
|---|---|---|---|---|---|---|
| 1ª | 1 | 3,9 | 2,2 | 1,2 | 0% | 1,5 |
| 1ª | 2 | 6,7 | 3,3 | 1,8 | 0% | 0,5 |
| 1ª | 3 | 9,7 | 4,7 | 2,6 | 0% | 0,7 |
| 1ª | 4 | 12,9 | 5,7 | 3,6 | 0% | 0,3 |
| 1ª | 5 | 16,0 | 6,6 | 4,4 | 0% | 0,3 |
| 1ª | 6 | 18,8 | 8,0 | 5,2 | 0% | 0,2 |
| 2ª | 1 | 3,0 | 1,8 | 1,0 | 0% | 2,2 |
| 2ª | 2 | 5,0 | 2,5 | 1,3 | 4% | 1,2 |
| 2ª | 3 | 5,6 | 2,9 | 1,8 | 5% | 0,8 |
| 2ª | 4 | 6,6 | 3,9 | 2,0 | 6% | 0,8 |
| 2ª | 5 | 7,2 | 4,6 | 3,0 | 4% | 0,6 |
| 2ª | 6 | 8,8 | 4,8 | 2,8 | 6% | 0,6 |
| 3ª | 1 | 3,0 | 1,6 | 0,5 | 7% | 1,8 |
| 3ª | 2 | 3,9 | 1,8 | 0,1 | 15% | 1,3 |
| 3ª | 3 | 4,9 | 1,9 | -0,9 | 24% | 1,0 |
| 3ª | 4 | 5,4 | 2,2 | -1,3 | 27% | 0,9 |
| 3ª | 5 | 5,9 | 2,4 | -2,2 | 30% | 0,7 |
| 3ª | 6 | 6,9 | 2,7 | -2,7 | 28% | 0,7 |

## Mobilidade

Clubes que mudaram de terço de força em campo no país entre o início e o fim das 6 temporadas: 9%.
