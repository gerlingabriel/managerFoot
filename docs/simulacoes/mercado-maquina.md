# Simulação do mercado da máquina

Gerado por `RelatorioMercadoTest` (ferramenta de desenvolvimento). As 6 ligas do banco padrão, 10 temporadas cada, começando do zero, só com clubes da máquina. Depois de cada rodada entra o caixa (bilheteria, despesas, salários) e então o mercado:

- clube com saldo abaixo de **10 rodadas da folha** põe em leilão o jogador mais caro sem asterisco que não deixa a posição abaixo do elenco mínimo (1 por rodada); com saldo negativo, o leilão é **sem lance mínimo**;
- todos os outros clubes do país dão lance se, pago o lance, o saldo continuar acima do limite mais 5 rodadas da folha (já com o novo salário), o plantel tiver menos de 25 (1 compra por rodada) e o jogador tiver força de pelo menos a força em campo do clube menos 5; o lance é 10% abaixo do preço, mais até 5% pela força do clube;
- leva o maior lance; empate por sorteio; sem lance, o jogador fica.

O patrocínio da primeira temporada (igual para todos da divisão) entra no saldo inicial. Sem premiação (paga no fim) nem compra direta de usuário. Preço: `ParametrosMercado.padrao()`.

## Premiação da liga e patrocínio (rev. 45)

Prêmio do campeão = preço de 2 jogadores com asterisco da **força de referência** (proposta: a do clube mais forte da divisão); 2º = 70%; 3º = 10%; patrocínio do 1º colocado = 10% do prêmio do campeão, caindo em proporção até o último; na primeira temporada, a média para todos. Médias por temporada simulada.

| Divisão | Força de referência | Campeão | 2º | 3º | Patrocínio no 1º ano | Bilheteria média do clube na temporada | Campeão / bilheteria |
|---|---|---|---|---|---|---|---|
| 1ª | 49,8 | R$ 3.923.333 | R$ 2.746.667 | R$ 392.500 | R$ 205.833 | R$ 15.909.314 | 25% |
| 2ª | 29,8 | R$ 977.000 | R$ 684.167 | R$ 97.833 | R$ 51.500 | R$ 4.556.318 | 21% |
| 3ª | 22,0 | R$ 566.000 | R$ 396.000 | R$ 57.000 | R$ 30.000 | R$ 2.595.736 | 22% |

## Por posição final na tabela (média por clube e temporada)

| Divisão | Terço da tabela | Vendas | Compras | Força em campo: início → fim | Saldo final (em limites) |
|---|---|---|---|---|---|
| 1ª | Topo | 0,0 | 0,5 | 46,3 → 46,3 | 3,3 |
| 1ª | Meio | 0,1 | 0,4 | 42,4 → 42,4 | 2,1 |
| 1ª | Fundo | 0,8 | 0,0 | 38,2 → 37,8 | 1,5 |
| 2ª | Topo | 0,1 | 1,5 | 28,0 → 28,0 | 2,3 |
| 2ª | Meio | 0,5 | 0,5 | 25,9 → 25,7 | 1,6 |
| 2ª | Fundo | 1,7 | 0,1 | 23,9 → 23,1 | 1,3 |
| 3ª | Topo | 0,0 | 1,8 | 20,5 → 20,6 | 2,4 |
| 3ª | Meio | 0,6 | 0,6 | 17,9 → 17,7 | 1,6 |
| 3ª | Fundo | 2,2 | 0,0 | 15,0 → 14,0 | 1,3 |

*Força em campo*: média dos 11 jogadores mais fortes. *Saldo final em limites*: saldo dividido pelo limite (1,0 = exatamente 10 rodadas da folha).

## Leilões

| Medida | Valor |
|---|---|
| Leilões por clube e temporada | 2,2 |
| Sem lance (jogador fica) | 72% |
| Sem lance mínimo (saldo negativo) | 0% |
| Valor pago / preço da tabela | 93% |
| Clubes que ficaram negativos em algum momento | 0% |
| Compras seguidas de venda em até 3 rodadas | 0% |
| Vendas nas 3 primeiras rodadas | 0% |
| Clubes que nunca venderam | 62% |
