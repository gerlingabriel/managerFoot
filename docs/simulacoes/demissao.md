# Demissão pela moral e troca de treinador

Gerado por `RelatorioDemissaoTest` (ferramenta de desenvolvimento). País com 2 divisões de 20 clubes: 1ª com forças de 50 a 35 e 2ª de 30 a 22 (como o banco padrão), turno e returno, 100 temporadas (sementes 0 a 99). Cada clube joga em 4-4-2 com os 11 na força do clube, sem evolução de força, mercado nem acesso; toda temporada começa com moral 22.

**Regras (SPEC §5.4 e §6.2, rev. 61):** verde de 15 a 30, amarelo de 7 a 14, vermelho de 0 a 6. Ao entrar no vermelho, o jogo seguinte é decisivo; se a moral continua vermelha, o treinador é demitido — máquina e usuário (aqui todos são máquina). Depois da rodada, do clube mais forte ao mais fraco, quem foi demitido chama o treinador de um clube do país de força inferior, no máximo 12 abaixo, com moral verde (o mais forte deles). Os dois trocam de clube e os dois clubes voltam a 22; nenhum dos dois entra em outra troca na mesma rodada. Sem chamado, o treinador fica e o jogo seguinte no vermelho é decisivo de novo.

**Ajuste pela força do adversário (proposta da rev. 61):** a surpresa vale mais. A cada *D* pontos de diferença de força, 1 ponto, no máximo *M*: o favorito que empata ou perde perde esse tanto a mais; o azarão que empata ou vence ganha esse tanto a mais; o resultado esperado vale o normal. Ex. com D = 3, M = 3: o clube de força 50 que perde em casa para um de 41 perde 3 + 3 = 6; o de 41 que vence fora ganha 3 + 3 = 6; se o de 50 vence, os dois ficam com o normal (+2 e −2).

## Comparação

Por temporada. *Trocas*: demissões que acharam quem chamar. *Decisivos sem chamado*: jogos decisivos perdidos sem ninguém para chamar (o treinador fica). *Chamado da 2ª*: % das trocas da 1ª em que o treinador veio da 2ª. *Vermelho no fim*: % dos clubes no vermelho na última rodada.

| D | M | Trocas 1ª | Trocas 2ª | Decisivos sem chamado | Chamado da 2ª | Diferença de força na troca | Vermelho no fim 1ª | Vermelho no fim 2ª |
|---|---|---|---|---|---|---|---|---|
| sem ajuste | 0 | 8,7 | 4,1 | 42,1 | 30% | 2,1 | 2% | 16% |
| 5 | 2 | 7,7 | 5,4 | 13,3 | 9% | 1,4 | 2% | 6% |
| 4 | 2 | 7,1 | 4,7 | 8,3 | 9% | 1,3 | 2% | 5% |
| 4 | 3 | 7,4 | 4,7 | 8,3 | 6% | 1,3 | 3% | 5% |
| **3** | 3 | 9,8 | 6,7 | 5,8 | 4% | 1,3 | 3% | 4% |
| 3 | 4 | 10,3 | 6,7 | 5,8 | 3% | 1,3 | 3% | 4% |
| 2 | 4 | 11,0 | 6,4 | 2,0 | 1% | 1,3 | 3% | 2% |

## Por clube, com os valores do jogo (D = 3, M = 3)

| Divisão | Força | Demitido e trocado por temporada | Decisivos sem chamado | Cedeu o treinador (subiu) |
|---|---|---|---|---|
| 1ª | 50 | 1,06 | 0,00 | 0,00 |
| 1ª | 49 | 0,88 | 0,00 | 0,78 |
| 1ª | 48 | 0,71 | 0,00 | 0,90 |
| 1ª | 48 | 0,82 | 0,00 | 0,12 |
| 1ª | 47 | 0,76 | 0,00 | 1,19 |
| 1ª | 46 | 0,73 | 0,00 | 0,80 |
| 1ª | 45 | 0,65 | 0,00 | 0,76 |
| 1ª | 44 | 0,51 | 0,00 | 0,74 |
| 1ª | 44 | 0,54 | 0,00 | 0,25 |
| 1ª | 43 | 0,45 | 0,00 | 0,89 |
| 1ª | 42 | 0,32 | 0,00 | 0,58 |
| 1ª | 41 | 0,29 | 0,00 | 0,37 |
| 1ª | 41 | 0,27 | 0,00 | 0,04 |
| 1ª | 40 | 0,21 | 0,00 | 0,47 |
| 1ª | 39 | 0,28 | 0,00 | 0,28 |
| 1ª | 38 | 0,23 | 0,00 | 0,30 |
| 1ª | 37 | 0,28 | 0,00 | 0,22 |
| 1ª | 37 | 0,24 | 0,00 | 0,02 |
| 1ª | 36 | 0,28 | 0,00 | 0,46 |
| 1ª | 35 | 0,28 | 0,00 | 0,22 |
| 2ª | 30 | 0,59 | 0,00 | 0,26 |
| 2ª | 30 | 0,54 | 0,00 | 0,09 |
| 2ª | 29 | 0,46 | 0,00 | 0,87 |
| 2ª | 29 | 0,42 | 0,00 | 0,26 |
| 2ª | 28 | 0,47 | 0,00 | 0,76 |
| 2ª | 28 | 0,31 | 0,00 | 0,13 |
| 2ª | 27 | 0,38 | 0,00 | 0,62 |
| 2ª | 27 | 0,36 | 0,00 | 0,14 |
| 2ª | 27 | 0,33 | 0,00 | 0,03 |
| 2ª | 26 | 0,39 | 0,00 | 0,85 |
| 2ª | 26 | 0,27 | 0,00 | 0,20 |
| 2ª | 25 | 0,27 | 0,00 | 0,54 |
| 2ª | 25 | 0,29 | 0,00 | 0,12 |
| 2ª | 25 | 0,23 | 0,00 | 0,04 |
| 2ª | 24 | 0,29 | 0,00 | 0,64 |
| 2ª | 24 | 0,41 | 0,01 | 0,10 |
| 2ª | 23 | 0,36 | 0,11 | 0,62 |
| 2ª | 23 | 0,34 | 0,10 | 0,11 |
| 2ª | 22 | 0,00 | 2,38 | 0,57 |
| 2ª | 22 | 0,00 | 3,24 | 0,16 |
