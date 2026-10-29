# Demissão pela moral

Gerado por `RelatorioDemissaoTest` (ferramenta de desenvolvimento). 200 temporadas por cenário, divisão de 20 clubes em turno e returno, calendário sorteado; sementes 0 a 199.

**Regra (SPEC §5.4, rev. 60):** moral de 0 a 30, começa em 22; verde de 15 a 30, amarelo de 7 a 14, vermelho de 0 a 6. Ao entrar no vermelho, o jogo seguinte é decisivo: se depois dele a moral continua vermelha, o treinador é demitido. Aqui a regra vale para **todos** os clubes, para medir a frequência. Depois da demissão o clube volta à moral inicial (novo treinador).

**Simplificação:** cada clube joga em 4-4-2 com os 11 na força do clube, sem evolução de força nem mercado, como em `temporada.md`.

## Cenário A: forças de 50 a 12, de 2 em 2

Demissões por temporada na divisão (média): **12,8**.

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
| 34 | 0,00 | 0,0% | – | 0,02 | 100% |
| 32 | 0,01 | 1,0% | 21,0 | 0,03 | 60% |
| 30 | 0,03 | 2,5% | 20,0 | 0,08 | 67% |
| 28 | 0,14 | 13,5% | 29,0 | 0,24 | 40% |
| 26 | 0,24 | 24,0% | 26,6 | 0,45 | 39% |
| 24 | 0,53 | 49,0% | 24,6 | 0,90 | 36% |
| 22 | 0,91 | 77,5% | 23,0 | 1,31 | 28% |
| 20 | 1,34 | 93,5% | 19,9 | 1,76 | 20% |
| 18 | 1,80 | 99,5% | 16,5 | 2,40 | 22% |
| 16 | 2,16 | 100,0% | 14,6 | 2,68 | 17% |
| 14 | 2,61 | 100,0% | 12,7 | 3,04 | 11% |
| 12 | 3,06 | 100,0% | 10,9 | 3,36 | 7% |

Demissões por trecho da temporada (% do total): rodadas 1–10 10%; rodadas 11–20 32%; rodadas 21–30 32%; rodadas 31–38 26%.

## Cenário B: forças de 50 a 31, de 1 em 1 (hierarquia apertada)

Demissões por temporada na divisão (média): **5,6**.

| Força | Demissões por temporada | Temporadas com demissão | Rodada da 1ª demissão (média) | Entradas no vermelho por temporada | Saídas do vermelho no jogo decisivo |
|---|---|---|---|---|---|
| 50 | 0,00 | 0,0% | – | 0,01 | 100% |
| 49 | 0,00 | 0,0% | – | 0,00 | – |
| 48 | 0,00 | 0,0% | – | 0,01 | 0% |
| 47 | 0,00 | 0,0% | – | 0,01 | 100% |
| 46 | 0,00 | 0,0% | – | 0,00 | – |
| 45 | 0,01 | 0,5% | 26,0 | 0,02 | 75% |
| 44 | 0,02 | 2,0% | 29,0 | 0,03 | 20% |
| 43 | 0,03 | 3,0% | 22,8 | 0,05 | 40% |
| 42 | 0,06 | 5,5% | 26,5 | 0,09 | 29% |
| 41 | 0,05 | 4,5% | 24,2 | 0,08 | 38% |
| 40 | 0,08 | 7,5% | 28,7 | 0,17 | 50% |
| 39 | 0,12 | 12,0% | 24,4 | 0,23 | 42% |
| 38 | 0,20 | 19,5% | 27,4 | 0,38 | 42% |
| 37 | 0,28 | 27,0% | 26,1 | 0,48 | 38% |
| 36 | 0,51 | 49,0% | 25,3 | 0,73 | 23% |
| 35 | 0,56 | 51,0% | 24,1 | 0,80 | 26% |
| 34 | 0,69 | 60,0% | 22,7 | 1,06 | 30% |
| 33 | 0,79 | 68,5% | 23,9 | 1,22 | 32% |
| 32 | 1,04 | 81,0% | 22,2 | 1,51 | 28% |
| 31 | 1,21 | 88,5% | 21,0 | 1,64 | 23% |

Demissões por trecho da temporada (% do total): rodadas 1–10 3%; rodadas 11–20 31%; rodadas 21–30 37%; rodadas 31–38 29%.

