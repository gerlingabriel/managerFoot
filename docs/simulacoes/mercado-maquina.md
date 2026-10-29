# Simulação do mercado da máquina

Gerado por `RelatorioMercadoTest` (ferramenta de desenvolvimento). As 6 ligas do banco padrão, 10 temporadas cada, começando do zero, só com clubes da máquina. Depois de cada rodada entra o caixa (bilheteria, despesas, salários) e então o mercado:

- clube com saldo abaixo de **10 rodadas da folha** põe em leilão o jogador mais caro sem asterisco que não deixa a posição abaixo do elenco mínimo (1 por rodada); com saldo negativo, o leilão é **sem lance mínimo**;
- todos os outros clubes do país dão lance se, pago o lance, o saldo continuar acima do limite mais 5 rodadas da folha (já com o novo salário), o plantel tiver menos de 25 (1 compra por rodada) e o jogador tiver força de pelo menos a força em campo do clube menos 5; o lance é 10% abaixo do preço, mais até 5% pela força do clube;
- leva o maior lance; empate por sorteio; sem lance, o jogador fica.

O patrocínio da primeira temporada (igual para todos da divisão) entra no saldo inicial. Sem premiação (paga no fim) nem compra direta de usuário. Preço: `ParametrosMercado.padrao()`.

## Premiação da liga e patrocínio (rev. 45)

Prêmio do campeão = preço de 2 jogadores com asterisco do **teto de força da divisão**; 2º = 70%; 3º = 10%; patrocínio do 1º colocado = 10% do prêmio do campeão, o 2º 10% menos e cada posição seguinte 8% menos que a anterior; na primeira temporada, a média para todos.

| Divisão | Teto de força | Campeão | 2º | 3º | Patrocínio no 1º ano | Bilheteria média do clube na temporada | Campeão / bilheteria |
|---|---|---|---|---|---|---|---|
| 1ª | 50,0 | R$ 4.364.000 | R$ 3.055.000 | R$ 436.000 | R$ 219.333 | R$ 15.423.523 | 28% |
| 2ª | 30,0 | R$ 988.000 | R$ 692.000 | R$ 99.000 | R$ 49.500 | R$ 4.605.201 | 21% |

## Por posição final na tabela (média por clube e temporada)

| Divisão | Terço da tabela | Vendas | Compras | Força em campo: início → fim | Saldo final (em limites) |
|---|---|---|---|---|---|
| 1ª | Topo | 0,0 | 0,3 | 46,5 → 46,5 | 3,4 |
| 1ª | Meio | 0,1 | 0,3 | 42,5 → 42,5 | 2,4 |
| 1ª | Fundo | 0,6 | 0,0 | 37,9 → 37,6 | 1,6 |
| 2ª | Topo | 0,0 | 0,8 | 28,2 → 28,2 | 3,0 |
| 2ª | Meio | 0,1 | 0,2 | 26,5 → 26,5 | 2,2 |
| 2ª | Fundo | 1,0 | 0,0 | 24,8 → 24,3 | 1,5 |

*Força em campo*: média dos 11 jogadores mais fortes. *Saldo final em limites*: saldo dividido pelo limite (1,0 = exatamente 10 rodadas da folha).

## Leilões

| Medida | Valor |
|---|---|
| Leilões por clube e temporada | 0,9 |
| Sem lance (jogador fica) | 67% |
| Sem lance mínimo (saldo negativo) | 0% |
| Valor pago / preço da tabela | 94% |
| Clubes que ficaram negativos em algum momento | 0% |
| Compras seguidas de venda em até 3 rodadas | 0% |
| Vendas nas 3 primeiras rodadas | 0% |
| Clubes que nunca venderam | 78% |
