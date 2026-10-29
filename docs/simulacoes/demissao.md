# Demissão pela moral

Gerado por `RelatorioDemissaoTest` (ferramenta de desenvolvimento). 200 temporadas por cenário, divisão de 20 clubes em turno e returno, calendário sorteado; sementes 0 a 199.

**Regra (SPEC §5.4, rev. 60):** moral de 0 a 30, começa em 22; verde de 15 a 30, amarelo de 8 a 14, vermelho de 0 a 7. Ao entrar no vermelho, o jogo seguinte é decisivo: se depois dele a moral continua vermelha, o treinador é demitido. Aqui a regra vale para **todos** os clubes, para medir a frequência. Depois da demissão o clube volta à moral inicial (novo treinador).

**Simplificação:** cada clube joga em 4-4-2 com os 11 na força do clube, sem evolução de força nem mercado, como em `temporada.md`.

## Cenário A: forças de 50 a 12, de 2 em 2

Demissões por temporada na divisão (média): **15,0**.

| Força | Demissões por temporada | Temporadas com demissão | Rodada da 1ª demissão (média) | Entradas no vermelho por temporada | Saídas do vermelho no jogo decisivo |
|---|---|---|---|---|---|
| 50 | 0,00 | 0,0% | – | 0,00 | – |
| 48 | 0,00 | 0,0% | – | 0,00 | – |
| 46 | 0,00 | 0,0% | – | 0,00 | – |
| 44 | 0,00 | 0,0% | – | 0,00 | – |
| 42 | 0,00 | 0,0% | – | 0,00 | – |
| 40 | 0,00 | 0,0% | – | 0,00 | – |
| 38 | 0,00 | 0,0% | – | 0,00 | – |
| 36 | 0,00 | 0,0% | – | 0,00 | – |
| 34 | 0,01 | 1,0% | 24,0 | 0,03 | 40% |
| 32 | 0,02 | 2,0% | 19,8 | 0,07 | 62% |
| 30 | 0,07 | 6,5% | 20,8 | 0,16 | 59% |
| 28 | 0,22 | 21,5% | 27,9 | 0,44 | 47% |
| 26 | 0,41 | 39,5% | 26,2 | 0,76 | 41% |
| 24 | 0,71 | 63,5% | 23,3 | 1,21 | 36% |
| 22 | 1,12 | 86,0% | 21,1 | 1,66 | 29% |
| 20 | 1,60 | 96,0% | 18,6 | 2,29 | 28% |
| 18 | 2,08 | 99,5% | 14,5 | 2,73 | 21% |
| 16 | 2,46 | 100,0% | 13,0 | 3,04 | 17% |
| 14 | 2,96 | 100,0% | 11,4 | 3,43 | 11% |
| 12 | 3,40 | 100,0% | 9,9 | 3,86 | 9% |

Demissões por trecho da temporada (% do total): rodadas 1–10 13%; rodadas 11–20 31%; rodadas 21–30 31%; rodadas 31–38 25%.

## Cenário B: forças de 50 a 31, de 1 em 1 (hierarquia apertada)

Demissões por temporada na divisão (média): **7,4**.

| Força | Demissões por temporada | Temporadas com demissão | Rodada da 1ª demissão (média) | Entradas no vermelho por temporada | Saídas do vermelho no jogo decisivo |
|---|---|---|---|---|---|
| 50 | 0,00 | 0,0% | – | 0,01 | 100% |
| 49 | 0,00 | 0,0% | – | 0,00 | – |
| 48 | 0,00 | 0,0% | – | 0,02 | 67% |
| 47 | 0,01 | 0,5% | 11,0 | 0,01 | 0% |
| 46 | 0,00 | 0,0% | – | 0,00 | – |
| 45 | 0,02 | 1,5% | 28,7 | 0,04 | 63% |
| 44 | 0,03 | 3,0% | 30,5 | 0,06 | 45% |
| 43 | 0,05 | 5,0% | 23,9 | 0,09 | 44% |
| 42 | 0,09 | 8,5% | 24,9 | 0,15 | 43% |
| 41 | 0,08 | 7,5% | 22,5 | 0,15 | 47% |
| 40 | 0,15 | 14,5% | 26,9 | 0,32 | 52% |
| 39 | 0,20 | 20,0% | 23,7 | 0,38 | 40% |
| 38 | 0,31 | 30,5% | 27,6 | 0,53 | 39% |
| 37 | 0,42 | 39,0% | 25,3 | 0,77 | 42% |
| 36 | 0,67 | 61,5% | 24,1 | 1,10 | 38% |
| 35 | 0,73 | 62,0% | 22,4 | 1,20 | 34% |
| 34 | 0,94 | 74,5% | 21,3 | 1,41 | 29% |
| 33 | 1,03 | 79,5% | 21,3 | 1,47 | 24% |
| 32 | 1,26 | 89,5% | 21,0 | 1,88 | 29% |
| 31 | 1,47 | 94,5% | 18,9 | 2,06 | 26% |

Demissões por trecho da temporada (% do total): rodadas 1–10 4%; rodadas 11–20 33%; rodadas 21–30 35%; rodadas 31–38 28%.

