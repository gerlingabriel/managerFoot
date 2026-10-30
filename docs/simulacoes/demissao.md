# Demissão pela moral e troca de treinador

Gerado por `RelatorioDemissaoTest` (ferramenta de desenvolvimento). País com 2 divisões de 20 clubes: 1ª com forças de 50 a 35 e 2ª de 30 a 22 (como o banco padrão), turno e returno, 100 temporadas (sementes 0 a 99). Cada clube joga em 4-4-2 com os 11 na força do clube, sem evolução de força, mercado nem acesso; toda temporada começa com moral 22.

**Regras (SPEC §5.4 e §6.2, rev. 61):** verde de 15 a 30, amarelo de 7 a 14, vermelho de 0 a 6. Ao entrar no vermelho, o jogo seguinte é decisivo; se a moral continua vermelha, o treinador é demitido — máquina e usuário (aqui todos são máquina). Depois da rodada, do clube mais forte ao mais fraco, quem foi demitido chama o treinador de um clube do país de força inferior, no máximo 12 abaixo, com moral verde (o mais forte deles). Os dois trocam de clube e os dois clubes voltam a 22; nenhum dos dois entra em outra troca na mesma rodada. Sem chamado, o treinador fica e o jogo seguinte no vermelho é decisivo de novo.

**Ajuste pela força do adversário (proposta da rev. 61):** a surpresa vale mais. A cada *D* pontos de diferença de força, 1 ponto, no máximo *M*: o favorito que empata ou perde perde esse tanto a mais; o azarão que empata ou vence ganha esse tanto a mais; o resultado esperado vale o normal. Ex. com D = 3, M = 3: o clube de força 50 que perde em casa para um de 41 perde 3 + 3 = 6; o de 41 que vence fora ganha 3 + 3 = 6; se o de 50 vence, os dois ficam com o normal (+2 e −2).

## Comparação

Por temporada. *Trocas*: demissões que acharam quem chamar. *Decisivos sem chamado*: jogos decisivos perdidos sem ninguém para chamar (o treinador fica). *Chamado da 2ª*: % das trocas da 1ª em que o treinador veio da 2ª. *Vermelho no fim*: % dos clubes no vermelho na última rodada.

| D | M | Trocas 1ª | Trocas 2ª | Decisivos sem chamado | Chamado da 2ª | Diferença de força na troca | Vermelho no fim 1ª | Vermelho no fim 2ª |
|---|---|---|---|---|---|---|---|---|
| sem ajuste | 0 | 8,5 | 4,4 | 42,2 | 26% | 2,0 | 3% | 15% |
| 5 | 2 | 7,6 | 5,5 | 14,6 | 8% | 1,3 | 2% | 7% |
| 4 | 2 | 7,0 | 4,9 | 9,1 | 6% | 1,3 | 2% | 4% |
| 4 | 3 | 7,3 | 4,9 | 9,1 | 4% | 1,2 | 2% | 4% |
| **3** | 3 | 9,7 | 6,7 | 6,2 | 2% | 1,2 | 3% | 4% |
| 3 | 4 | 10,4 | 6,7 | 6,2 | 1% | 1,2 | 2% | 4% |
| 2 | 4 | 11,3 | 6,2 | 2,2 | 1% | 1,2 | 3% | 2% |

## Por clube, com os valores do jogo (D = 3, M = 3)

| Divisão | Força | Demitido e trocado por temporada | Decisivos sem chamado | Cedeu o treinador (subiu) |
|---|---|---|---|---|
| 1ª | 50 | 1,00 | 0,00 | 0,00 |
| 1ª | 49 | 0,98 | 0,00 | 0,58 |
| 1ª | 48 | 0,78 | 0,00 | 0,94 |
| 1ª | 48 | 0,83 | 0,00 | 0,33 |
| 1ª | 47 | 0,73 | 0,00 | 1,21 |
| 1ª | 46 | 0,75 | 0,00 | 0,83 |
| 1ª | 45 | 0,57 | 0,00 | 0,89 |
| 1ª | 44 | 0,48 | 0,00 | 0,64 |
| 1ª | 44 | 0,64 | 0,00 | 0,12 |
| 1ª | 43 | 0,54 | 0,00 | 0,93 |
| 1ª | 42 | 0,31 | 0,00 | 0,66 |
| 1ª | 41 | 0,32 | 0,00 | 0,36 |
| 1ª | 41 | 0,31 | 0,00 | 0,10 |
| 1ª | 40 | 0,30 | 0,00 | 0,53 |
| 1ª | 39 | 0,20 | 0,00 | 0,38 |
| 1ª | 38 | 0,18 | 0,00 | 0,23 |
| 1ª | 37 | 0,26 | 0,00 | 0,15 |
| 1ª | 37 | 0,18 | 0,00 | 0,04 |
| 1ª | 36 | 0,16 | 0,00 | 0,40 |
| 1ª | 35 | 0,13 | 0,00 | 0,17 |
| 2ª | 30 | 0,44 | 0,00 | 0,13 |
| 2ª | 30 | 0,46 | 0,00 | 0,03 |
| 2ª | 29 | 0,36 | 0,00 | 0,66 |
| 2ª | 29 | 0,56 | 0,00 | 0,20 |
| 2ª | 28 | 0,38 | 0,00 | 0,75 |
| 2ª | 28 | 0,32 | 0,00 | 0,14 |
| 2ª | 27 | 0,40 | 0,00 | 0,61 |
| 2ª | 27 | 0,38 | 0,00 | 0,13 |
| 2ª | 27 | 0,44 | 0,00 | 0,01 |
| 2ª | 26 | 0,36 | 0,00 | 0,98 |
| 2ª | 26 | 0,32 | 0,00 | 0,20 |
| 2ª | 25 | 0,30 | 0,00 | 0,53 |
| 2ª | 25 | 0,32 | 0,00 | 0,12 |
| 2ª | 25 | 0,29 | 0,00 | 0,06 |
| 2ª | 24 | 0,30 | 0,00 | 0,78 |
| 2ª | 24 | 0,42 | 0,00 | 0,14 |
| 2ª | 23 | 0,35 | 0,06 | 0,60 |
| 2ª | 23 | 0,33 | 0,13 | 0,09 |
| 2ª | 22 | 0,00 | 2,84 | 0,42 |
| 2ª | 22 | 0,00 | 3,16 | 0,31 |
