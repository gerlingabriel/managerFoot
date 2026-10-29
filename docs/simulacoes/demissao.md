# Demissão pela moral

Gerado por `RelatorioDemissaoTest` (ferramenta de desenvolvimento). 200 temporadas por cenário, divisão de 20 clubes em turno e returno, calendário sorteado; sementes 0 a 199.

**Regra (SPEC §5.4, rev. 60):** moral de 0 a 30, começa em 22; verde de 15 a 30, amarelo de 7 a 14, vermelho de 0 a 6. Ao entrar no vermelho, o jogo seguinte é decisivo: se depois dele a moral continua vermelha, o treinador é demitido. Aqui a regra vale para **todos** os clubes, para medir a frequência.

**Troca (rev. 61):** é chamado o treinador de um clube de força inferior com moral verde (entre os 3 mais fortes, o de maior força; aqui não há usuário). Os dois trocam de clube e os dois clubes voltam à moral inicial. Sem chamado (nenhum inferior de moral verde), o treinador fica e a moral segue: o jogo seguinte no vermelho é decisivo de novo.

**Simplificação:** cada clube joga em 4-4-2 com os 11 na força do clube, sem evolução de força nem mercado, como em `temporada.md`.

## Cenário A: forças de 50 a 12, de 2 em 2

Trocas por temporada na divisão (média): **1,0**; diferença média de força entre o clube que demitiu e o que cedeu o treinador: **2,6**. Jogos decisivos perdidos sem ninguém para chamar (o treinador fica): **105,1** por temporada.

| Força | Trocas por temporada | Temporadas com troca | Rodada da 1ª troca (média) | Entradas no vermelho por temporada | Saídas do vermelho no jogo decisivo | Decisivos perdidos sem chamado | Vezes que cedeu o treinador |
|---|---|---|---|---|---|---|---|
| 50 | 0,00 | 0,0% | – | 0,00 | – | 0,00 | 0,00 |
| 48 | 0,00 | 0,0% | – | 0,00 | – | 0,00 | 0,00 |
| 46 | 0,00 | 0,0% | – | 0,00 | – | 0,00 | 0,00 |
| 44 | 0,00 | 0,0% | – | 0,00 | – | 0,00 | 0,00 |
| 42 | 0,00 | 0,0% | – | 0,00 | – | 0,00 | 0,00 |
| 40 | 0,00 | 0,0% | – | 0,00 | – | 0,00 | 0,00 |
| 38 | 0,00 | 0,0% | – | 0,00 | – | 0,00 | 0,00 |
| 36 | 0,00 | 0,0% | – | 0,00 | – | 0,00 | 0,00 |
| 34 | 0,00 | 0,0% | – | 0,02 | 100% | 0,00 | 0,00 |
| 32 | 0,01 | 1,0% | 21,0 | 0,03 | 60% | 0,00 | 0,00 |
| 30 | 0,03 | 2,5% | 20,0 | 0,08 | 69% | 0,00 | 0,01 |
| 28 | 0,12 | 12,0% | 28,3 | 0,24 | 48% | 0,03 | 0,03 |
| 26 | 0,16 | 16,0% | 25,0 | 0,47 | 54% | 0,25 | 0,10 |
| 24 | 0,22 | 21,0% | 22,3 | 1,08 | 65% | 1,11 | 0,14 |
| 22 | 0,22 | 21,0% | 20,1 | 1,67 | 71% | 4,10 | 0,19 |
| 20 | 0,13 | 12,5% | 15,8 | 2,13 | 64% | 10,04 | 0,22 |
| 18 | 0,07 | 7,0% | 14,6 | 2,26 | 61% | 16,32 | 0,17 |
| 16 | 0,02 | 1,5% | 11,3 | 1,98 | 51% | 21,09 | 0,10 |
| 14 | 0,01 | 0,5% | 9,0 | 1,63 | 39% | 24,59 | 0,03 |
| 12 | 0,00 | 0,0% | – | 1,25 | 20% | 27,59 | 0,01 |

Trocas por trecho da temporada (% do total): rodadas 1–10 9%; rodadas 11–20 38%; rodadas 21–30 37%; rodadas 31–38 17%.

## Cenário B: forças de 50 a 31, de 1 em 1 (hierarquia apertada)

Trocas por temporada na divisão (média): **2,5**; diferença média de força entre o clube que demitiu e o que cedeu o treinador: **1,6**. Jogos decisivos perdidos sem ninguém para chamar (o treinador fica): **21,5** por temporada.

| Força | Trocas por temporada | Temporadas com troca | Rodada da 1ª troca (média) | Entradas no vermelho por temporada | Saídas do vermelho no jogo decisivo | Decisivos perdidos sem chamado | Vezes que cedeu o treinador |
|---|---|---|---|---|---|---|---|
| 50 | 0,00 | 0,0% | – | 0,01 | 100% | 0,00 | 0,00 |
| 49 | 0,00 | 0,0% | – | 0,00 | – | 0,00 | 0,00 |
| 48 | 0,00 | 0,0% | – | 0,01 | 0% | 0,00 | 0,00 |
| 47 | 0,00 | 0,0% | – | 0,01 | 100% | 0,00 | 0,00 |
| 46 | 0,00 | 0,0% | – | 0,00 | – | 0,00 | 0,00 |
| 45 | 0,01 | 0,5% | 26,0 | 0,02 | 75% | 0,00 | 0,00 |
| 44 | 0,02 | 2,0% | 29,0 | 0,03 | 20% | 0,00 | 0,01 |
| 43 | 0,03 | 3,0% | 22,8 | 0,05 | 40% | 0,00 | 0,02 |
| 42 | 0,06 | 5,5% | 26,5 | 0,09 | 29% | 0,00 | 0,02 |
| 41 | 0,05 | 4,5% | 24,2 | 0,08 | 38% | 0,00 | 0,06 |
| 40 | 0,08 | 7,5% | 28,7 | 0,17 | 50% | 0,00 | 0,05 |
| 39 | 0,12 | 12,0% | 24,4 | 0,23 | 42% | 0,00 | 0,05 |
| 38 | 0,19 | 19,0% | 27,3 | 0,37 | 41% | 0,04 | 0,08 |
| 37 | 0,24 | 23,5% | 26,5 | 0,51 | 47% | 0,14 | 0,14 |
| 36 | 0,42 | 40,5% | 24,5 | 0,74 | 34% | 0,32 | 0,16 |
| 35 | 0,41 | 37,5% | 22,8 | 0,83 | 42% | 0,70 | 0,30 |
| 34 | 0,41 | 37,5% | 21,9 | 1,16 | 51% | 1,66 | 0,44 |
| 33 | 0,29 | 28,0% | 22,7 | 1,36 | 58% | 3,41 | 0,43 |
| 32 | 0,20 | 18,5% | 21,6 | 1,76 | 68% | 6,03 | 0,40 |
| 31 | 0,00 | 0,0% | – | 2,11 | 74% | 9,24 | 0,36 |

Trocas por trecho da temporada (% do total): rodadas 1–10 2%; rodadas 11–20 35%; rodadas 21–30 36%; rodadas 31–38 27%.

