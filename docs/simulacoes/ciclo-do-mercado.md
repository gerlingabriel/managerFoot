# Ciclo do mercado: quem vende, quem compra

Gerado por `RelatorioCicloMercadoTest` (ferramenta de desenvolvimento). As 6 ligas do banco padrão, 3 sementes, 6 temporadas seguidas com promoção e rebaixamento. Economia atual: público pela posição e pela força (§9.2), estádio com ampliação da máquina, evolução de força (§6.1), salário da máquina pela força atual com as faixas da rev. 61, prêmio da liga e patrocínio fixo pela posição. Antes de cada rodada, os leilões da máquina (`LeiloesDaRodada`, as regras da rev. 44): quem está abaixo de 10 rodadas da folha põe o mais caro sem asterisco em leilão; dá lance quem fica acima do limite mais 5 rodadas da folha depois de pagar, tem menos de 25 jogadores e o jogador tem força de pelo menos a do time menos 5. Só máquina.

## Inflação do salário pela força do clube (rev. 66)

*Pelo clube*: todos os jogadores recebem com a faixa da força em campo do clube (abaixo de 15 / 15–24 / 25–34 / 35–44 / 45–50), e o preço sai do salário sem faixa (só a força e o asterisco). *Plantel até 23*: abaixo de 23 jogadores, o clube com caixa (limite mais a folga) dá lance em jogador de qualquer força. O prêmio e a ampliação do estádio seguem o preço do jogador de referência. Saldo no fim da 6ª temporada, em limites (1 limite = 10 rodadas da folha), por terço final da tabela.

| Cenário | 1ª topo | 1ª meio | 1ª fundo | 2ª topo | 2ª meio | 2ª fundo | Clubes negativos (2ª) | Plantel médio 1ª / 2ª | Leilões sem lance 1ª / 2ª | Vendas do rico no fundo | Mobilidade | Ligas travadas |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Antes (rev. 61): faixa pelo jogador (−10/−5/0/+5/+10) | 21,5 | 10,9 | 7,9 | 10,9 | 5,5 | 1,3 | 8% | 20,5 / 20,1 | 27% / 24% | 2,7 | 6% | 0 de 18 |
| Pelo clube −10/−5/0/+10/+20, preço sem faixa | 18,4 | 9,0 | 8,5 | 11,0 | 5,2 | 1,2 | 10% | 20,5 / 20,3 | 32% / 20% | 2,5 | 5% | 0 de 18 |
| Pelo clube −10/−5/0/+10/+20, preço sem faixa, plantel até 23 | 14,2 | 8,1 | 8,1 | 13,2 | 9,6 | 1,9 | 10% | 22,5 / 18,2 | 32% / 20% | 2,0 | 4% | 0 de 18 |
| Pelo clube −20/−10/0/+10/+20, preço sem faixa, plantel até 23 | 14,2 | 8,1 | 8,6 | 14,8 | 10,2 | 2,9 | 7% | 22,2 / 18,5 | 34% / 21% | 1,8 | 5% | 0 de 18 |
| Escolhido (rev. 66): pelo clube −30/−15/0/+10/+20, preço sem faixa, plantel até 23 | 14,5 | 7,9 | 8,9 | 16,0 | 12,0 | 3,3 | 5% | 22,2 / 18,4 | 33% / 22% | 1,8 | 6% | 0 de 18 |

*Ligas travadas*: liga × semente em que um clube ficou sem 7 disponíveis (vendas até o plantel mínimo de 14 mais suspensões); a simulação dessa liga para ali e as medidas contam só as temporadas jogadas.

As tabelas abaixo são do cenário "Escolhido (rev. 66): pelo clube −30/−15/0/+10/+20, preço sem faixa, plantel até 23".

*Riqueza*: terço do clube na divisão pelo saldo dividido pelo limite, no início da temporada. *Terço da tabela*: posição na hora da venda.

## Quem vende: riqueza no início da temporada × posição na hora (vendas por 100 clubes-rodada)

| Riqueza | Topo | Meio | Fundo |
|---|---|---|---|
| Rico | 0,0 | 0,1 | 1,8 |
| Médio | 0,0 | 0,1 | 1,0 |
| Pobre | 0,1 | 0,5 | 3,5 |

## Para onde vão os jogadores: divisão do vendedor × do comprador (% das vendas)

| Vendedor \ comprador | 1ª | 2ª | 3ª |
|---|---|---|---|
| 1ª | 32% | 1% | 0% |
| 2ª | 59% | 7% | 0% |
| 3ª | 0% | 0% | 0% |

## Na mesma divisão: terço do vendedor × do comprador (% das vendas na mesma divisão)

| Vendedor \ comprador | Topo | Meio | Fundo |
|---|---|---|---|
| Topo | 0% | 0% | 0% |
| Meio | 4% | 2% | 0% |
| Fundo | 56% | 31% | 5% |

## Quem consegue comprar: clubes que dão lance por leilão aberto, por terço da tabela

% dos clubes do terço que deram lance em cada leilão do país (todos os clubes da máquina podem dar lance em qualquer leilão do país).

| Divisão | Leilões | Sem lance | Topo | Meio | Fundo | Pago / preço | Força do vendido − força do comprador |
|---|---|---|---|---|---|---|---|
| 1ª | 605 | 33% | 20% | 31% | 35% | 95% | -7,4 |
| 2ª | 1016 | 22% | 64% | 53% | 25% | 94% | -17,3 |

*Leilões*: pela divisão do vendedor. *Topo/Meio/Fundo*: pela divisão de quem dá o lance.

## Caixa por temporada: saldo no fim em limites (1,0 = 10 rodadas da folha) por terço final

| Divisão | Temporada | Topo | Meio | Fundo | Clubes negativos | Jogadores trocados por clube |
|---|---|---|---|---|---|---|
| 1ª | 1 | 3,6 | 2,4 | 1,5 | 0% | 1,9 |
| 1ª | 2 | 6,1 | 3,7 | 3,8 | 0% | 0,7 |
| 1ª | 3 | 8,4 | 5,3 | 4,3 | 0% | 0,6 |
| 1ª | 4 | 10,4 | 6,2 | 6,6 | 0% | 0,5 |
| 1ª | 5 | 12,2 | 7,4 | 7,1 | 0% | 0,3 |
| 1ª | 6 | 14,5 | 7,9 | 8,9 | 0% | 0,3 |
| 2ª | 1 | 5,1 | 3,4 | 1,7 | 0% | 0,6 |
| 2ª | 2 | 7,7 | 5,6 | 2,0 | 0% | 0,6 |
| 2ª | 3 | 9,7 | 7,4 | 2,6 | 0% | 0,5 |
| 2ª | 4 | 11,3 | 8,9 | 2,9 | 1% | 0,4 |
| 2ª | 5 | 13,8 | 10,0 | 4,2 | 2% | 0,2 |
| 2ª | 6 | 16,0 | 12,0 | 3,3 | 5% | 0,3 |

## Mobilidade

Clubes que mudaram de terço de força em campo no país entre o início e o fim das 6 temporadas: 6%.
