# Ciclo do mercado: quem vende, quem compra

Gerado por `RelatorioCicloMercadoTest` (ferramenta de desenvolvimento). As 6 ligas do banco padrão, 3 sementes, 6 temporadas seguidas com promoção e rebaixamento. Economia atual: público pela posição e pela força (§9.2), estádio com ampliação da máquina, evolução de força (§6.1), salário da máquina pela força atual, prêmio da liga e patrocínio fixo pela posição. Antes de cada rodada, os leilões da máquina (`LeiloesDaRodada`, as regras da rev. 44): quem está abaixo de 10 rodadas da folha põe o mais caro sem asterisco em leilão; dá lance quem fica acima do limite mais 5 rodadas da folha depois de pagar, tem menos de 25 jogadores e o jogador tem força de pelo menos a do time menos 5. Só máquina, sem compra direta.

*Riqueza*: terço do clube na divisão pelo saldo dividido pelo limite, no início da temporada. *Terço da tabela*: posição na hora da venda.

## Quem vende: riqueza no início da temporada × posição na hora (vendas por 100 clubes-rodada)

| Riqueza | Topo | Meio | Fundo |
|---|---|---|---|
| Rico | 0,0 | 0,1 | 3,2 |
| Médio | 0,0 | 0,2 | 2,1 |
| Pobre | 0,4 | 0,7 | 4,1 |

## Para onde vão os jogadores: divisão do vendedor × do comprador (% das vendas)

| Vendedor \ comprador | 1ª | 2ª | 3ª |
|---|---|---|---|
| 1ª | 21% | 5% | 0% |
| 2ª | 9% | 65% | 0% |
| 3ª | 0% | 0% | 0% |

## Na mesma divisão: terço do vendedor × do comprador (% das vendas na mesma divisão)

| Vendedor \ comprador | Topo | Meio | Fundo |
|---|---|---|---|
| Topo | 1% | 1% | 1% |
| Meio | 5% | 3% | 2% |
| Fundo | 31% | 45% | 12% |

## Quem consegue comprar: clubes que dão lance por leilão aberto, por terço da tabela

% dos clubes do terço que deram lance em cada leilão do país (todos os clubes da máquina podem dar lance em qualquer leilão do país).

| Divisão | Leilões | Sem lance | Topo | Meio | Fundo | Pago / preço | Força do vendido − força do comprador |
|---|---|---|---|---|---|---|---|
| 1ª | 610 | 27% | 2% | 5% | 9% | 94% | -2,4 |
| 2ª | 1780 | 26% | 25% | 23% | 9% | 93% | -3,2 |

*Leilões*: pela divisão do vendedor. *Topo/Meio/Fundo*: pela divisão de quem dá o lance.

## Caixa por temporada: saldo no fim em limites (1,0 = 10 rodadas da folha) por terço final

| Divisão | Temporada | Topo | Meio | Fundo | Clubes negativos | Jogadores trocados por clube |
|---|---|---|---|---|---|---|
| 1ª | 1 | 4,5 | 2,7 | 1,4 | 0% | 1,2 |
| 1ª | 2 | 8,5 | 4,5 | 2,5 | 0% | 0,5 |
| 1ª | 3 | 12,8 | 6,4 | 4,0 | 0% | 0,4 |
| 1ª | 4 | 16,9 | 8,4 | 5,1 | 0% | 0,3 |
| 1ª | 5 | 21,7 | 10,1 | 6,1 | 0% | 0,2 |
| 1ª | 6 | 25,9 | 11,4 | 8,4 | 0% | 0,2 |
| 2ª | 1 | 4,0 | 2,3 | 1,3 | 0% | 1,7 |
| 2ª | 2 | 6,6 | 2,9 | 1,4 | 0% | 1,4 |
| 2ª | 3 | 6,6 | 3,5 | 1,4 | 2% | 1,4 |
| 2ª | 4 | 8,6 | 3,8 | 1,2 | 5% | 1,2 |
| 2ª | 5 | 8,9 | 3,8 | 1,2 | 6% | 0,9 |
| 2ª | 6 | 9,9 | 4,4 | 0,7 | 10% | 0,6 |

## Mobilidade

Clubes que mudaram de terço de força em campo no país entre o início e o fim das 6 temporadas: 6%.
