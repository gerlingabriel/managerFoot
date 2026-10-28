# Simulação do mercado da máquina

Gerado por `RelatorioMercadoTest` (ferramenta de desenvolvimento). As 6 ligas do banco padrão, 10 temporadas cada, começando do zero, só com clubes da máquina. Depois de cada rodada entra o caixa (bilheteria, despesas, salários) e então o mercado:

- clube com saldo abaixo de **10 rodadas da folha** põe em leilão o jogador mais caro sem asterisco que não deixa a posição abaixo do elenco mínimo (1 por rodada); com saldo negativo, o leilão é **sem lance mínimo**;
- todos os outros clubes do país dão lance se, pago o lance, o saldo continuar acima do limite mais 5 rodadas da folha (já com o novo salário), o plantel tiver menos de 25 (1 compra por rodada) e o jogador tiver força de pelo menos a força em campo do clube menos 5; o lance é 10% abaixo do preço, mais até 5% pela força do clube;
- leva o maior lance; empate por sorteio; sem lance, o jogador fica.

Sem patrocínio, premiação nem compra direta de usuário. Preço: `ParametrosMercado.padrao()`.

## Por posição final na tabela (média por clube e temporada)

| Divisão | Terço da tabela | Vendas | Compras | Força em campo: início → fim | Saldo final (em limites) |
|---|---|---|---|---|---|
| 1ª | Topo | 0,0 | 0,6 | 46,3 → 46,3 | 3,2 |
| 1ª | Meio | 0,2 | 0,5 | 42,5 → 42,4 | 2,1 |
| 1ª | Fundo | 1,0 | 0,1 | 38,1 → 37,7 | 1,4 |
| 2ª | Topo | 0,1 | 1,7 | 27,9 → 28,0 | 2,2 |
| 2ª | Meio | 0,6 | 0,6 | 26,0 → 25,8 | 1,6 |
| 2ª | Fundo | 1,8 | 0,1 | 23,8 → 23,1 | 1,3 |
| 3ª | Topo | 0,0 | 1,9 | 20,4 → 20,5 | 2,3 |
| 3ª | Meio | 0,6 | 0,6 | 17,9 → 17,6 | 1,5 |
| 3ª | Fundo | 2,3 | 0,0 | 15,1 → 14,0 | 1,2 |

*Força em campo*: média dos 11 jogadores mais fortes. *Saldo final em limites*: saldo dividido pelo limite (1,0 = exatamente 10 rodadas da folha).

## Leilões

| Medida | Valor |
|---|---|
| Leilões por clube e temporada | 3,9 |
| Sem lance (jogador fica) | 83% |
| Sem lance mínimo (saldo negativo) | 0% |
| Valor pago / preço da tabela | 93% |
| Clubes que ficaram negativos em algum momento | 0% |
| Compras seguidas de venda em até 3 rodadas | 0% |
| Vendas nas 3 primeiras rodadas | 0% |
| Clubes que nunca venderam | 59% |
