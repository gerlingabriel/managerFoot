# Compra da máquina: por que a 2ª não compra (SPEC rev. 124)

Estudo de 2026-11-06, pedido do usuário: o clube pequeno da 2ª conseguir comprar sem que o dinheiro se dilua rápido. Jogo completo (`RelatorioJogoCompletoIT`), 6 países × 1 semente × 5 temporadas, só máquina, com uma instrumentação temporária dos leilões (não fica no repositório): antes de cada rodada guarda caixa e plantel de cada clube da 1ª e da 2ª e, depois dos leilões, classifica o clube pelo ponto mais longe a que chegou. Aproximado: não refaz a ordem dos leilões dentro da rodada. Rodada 4 em diante; o terço é a posição na tabela antes da rodada.

## Diagnóstico (regra das rev. 44/45, antes da rev. 124)

| Grupo | Comprou | Deu lance e perdeu | Sem caixa | Sem interesse | Plantel cheio | Sem oferta | Caixa (rodadas da folha) | Precisaria |
|---|---|---|---|---|---|---|---|---|
| 1ª topo | 3% | 4% | 7% | 24% | 10% | 52% | 33,8 | 17,5 |
| 1ª meio | 3% | 5% | 21% | 11% | 8% | 52% | 17,6 | 17,6 |
| 1ª fundo | 1% | 13% | 27% | 3% | 3% | 54% | 15,5 | 17,9 |
| 2ª topo | 2% | 37% | 8% | 0% | 1% | 52% | 42,9 | 23,9 |
| 2ª meio | 0% | 29% | 19% | 0% | 0% | 52% | 30,5 | 25,7 |
| 2ª fundo | 0% | 8% | 38% | 0% | 0% | 54% | 14,4 | 27,5 |

*Sem caixa*: havia jogador de interesse, mas pago o lance não sobrariam o limite (10 rodadas da folha) e a folga (5). *Precisaria*: mediana do caixa que bastaria para o jogador de interesse mais barato. *Sem oferta*: nenhum jogador de outro clube em leilão na rodada (só leiloa quem está abaixo do limite).

- **Fundo da 2ª:** falta caixa (14 contra 27). O jogador que serve custa cerca de 12 rodadas da folha dele; comprar deixaria o clube quase sem caixa.
- **Meio e topo da 2ª:** têm caixa, mas perdem o leilão. Quem levou quando o meio da 2ª perdeu: 1ª (meio, topo e fundo) 67%, 2ª topo 31%. O acréscimo de até 5% pela força (até 50) faz a 1ª sempre dar mais.

## Cenários

- **A (aprovado, rev. 124):** o acréscimo de até 5% conta a força do clube em relação ao teto da divisão (até 1).
- **A+C (descartado):** A e, além disso, o clube com caixa acima do limite e plantel acima de 23 leiloa por rodada a reserva mais cara sem asterisco (fora dos 11 mais fortes).

| | Antes | A | A+C |
|---|---|---|---|
| 2ª topo: comprou / perdeu / sem caixa | 2% / 37% / 8% | 6% / 20% / 12% | 15% / 30% / 16% |
| 2ª meio: comprou | 0% | 1% | 3% |
| 2ª fundo: comprou / sem caixa | 0% / 38% | 0% / 38% | 0% / 56% |
| Caixa da 2ª no fim da 2ª / 5ª temporada (mediana, rodadas da folha) | 36 / 48 | 23 / 32 | 22 / 29 |
| Caixa da 1ª no fim da 2ª / 5ª temporada | 26 / 30 | 30 / 54 | 29 / 50 |
| Vendas por liga e temporada (média; na 5ª) | 23 (19) | 23 (16) | 55 (78) |
| Campeão da 1ª entre os 3 mais fortes | 87% | 77% | 60% |
| Mesmo campeão do ano anterior | 42% | 21% | 29% |
| Promovidos que caem de volta | 82% | 75% | 74% |
| Gols por jogo | 2,8 | 2,8 | 2,9 |

Com A, o dinheiro que a 2ª gasta vai para os clubes da 1ª que vendem; a 2ª continua com caixa positivo e crescendo, mais devagar. A+C faz o mercado girar cada vez mais (as vendas sobem a cada temporada) e foi descartado. O fundo da 2ª fica como está (usuário, rev. 124).
