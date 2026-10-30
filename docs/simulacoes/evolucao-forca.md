# Simulação da evolução de força

Gerado por `RelatorioEvolucaoTest` (ferramenta de desenvolvimento). As 6 ligas do banco padrão, 3 sementes cada, 6 temporadas seguidas com promoção, rebaixamento e fila. Só clubes da máquina, que escalam sempre os mais fortes. Depois de cada partida:

- cada jogador muda no máximo 1 ponto por partida;
- entre os que jogaram (titulares e quem entrou), um número de jogadores sorteados perde 1: 1 na vitória, 3 no empate, 5 na derrota (rev. 55);
- entre os que não jogaram (reserva que não entrou e quem ficou fora da escalação), um número de jogadores sorteados ganha 1, se estiver abaixo do teto da divisão (quantos: em cada variante; parte fracionária = chance de mais um);
- quem está acima do teto não cresce e perde 1 com 25% de chance por partida, se não perdeu pelo resultado;
- com piso, o desgaste não leva abaixo da força cadastrada do clube − 5 (como o "nível base do clube" do Elifoot 98);
- a força fica entre 1 e 50. Sem mercado e sem caixa.

*Em campo*: média dos 11 mais fortes de cada clube. *Plantel*: média dos 23. *Faixa*: o clube mais fraco e o mais forte da divisão (média entre as sementes). Temporada 0 = início do jogo.

## A · literal: perdem 1/3/5 jogadores; crescem 2/1/0, sem piso

| Divisão (teto) | Temporada | Em campo | Plantel | Faixa em campo | No teto |
|---|---|---|---|---|---|
| 1ª (50) | 0 | 42,5 | 40,0 | 35,1 – 49,6 | 4% |
| 1ª (50) | 1 | 37,0 | 36,1 | 27,3 – 45,8 | 0% |
| 1ª (50) | 2 | 32,8 | 31,5 | 16,6 – 44,5 | 0% |
| 1ª (50) | 3 | 30,6 | 28,9 | 15,0 – 44,2 | 1% |
| 1ª (50) | 4 | 27,5 | 25,4 | 11,3 – 43,9 | 1% |
| 1ª (50) | 5 | 24,9 | 22,5 | 7,8 – 43,7 | 2% |
| 1ª (50) | 6 | 22,5 | 19,8 | 5,2 – 43,7 | 2% |
| 2ª (30) | 0 | 26,6 | 24,2 | 23,2 – 29,7 | 10% |
| 2ª (30) | 1 | 21,0 | 20,2 | 15,5 – 26,2 | 0% |
| 2ª (30) | 2 | 18,7 | 17,6 | 10,2 – 29,1 | 3% |
| 2ª (30) | 3 | 14,8 | 13,3 | 6,6 – 24,1 | 1% |
| 2ª (30) | 4 | 11,6 | 9,8 | 3,6 – 22,7 | 1% |
| 2ª (30) | 5 | 8,9 | 6,9 | 2,7 – 18,7 | 1% |
| 2ª (30) | 6 | 7,0 | 5,0 | 2,2 – 15,9 | 1% |

## B · literal com piso (força do clube − 5)

| Divisão (teto) | Temporada | Em campo | Plantel | Faixa em campo | No teto |
|---|---|---|---|---|---|
| 1ª (50) | 0 | 42,5 | 40,0 | 35,1 – 49,6 | 4% |
| 1ª (50) | 1 | 37,8 | 37,6 | 30,2 – 45,6 | 0% |
| 1ª (50) | 2 | 36,4 | 36,2 | 23,0 – 45,1 | 0% |
| 1ª (50) | 3 | 37,2 | 37,0 | 27,1 – 45,0 | 0% |
| 1ª (50) | 4 | 36,6 | 36,4 | 23,6 – 45,1 | 0% |
| 1ª (50) | 5 | 37,2 | 36,9 | 26,1 – 45,0 | 0% |
| 1ª (50) | 6 | 36,7 | 36,5 | 23,7 – 45,0 | 0% |
| 2ª (30) | 0 | 26,6 | 24,2 | 23,2 – 29,7 | 10% |
| 2ª (30) | 1 | 21,9 | 21,7 | 18,2 – 26,1 | 0% |
| 2ª (30) | 2 | 22,4 | 22,2 | 17,0 – 30,1 | 15% |
| 2ª (30) | 3 | 21,8 | 21,7 | 17,5 – 27,9 | 3% |
| 2ª (30) | 4 | 22,2 | 22,1 | 17,1 – 30,0 | 12% |
| 2ª (30) | 5 | 21,9 | 21,7 | 17,4 – 28,4 | 4% |
| 2ª (30) | 6 | 22,2 | 22,1 | 17,4 – 30,2 | 11% |

## C · proposta: crescem 4/2/0,5, com piso

| Divisão (teto) | Temporada | Em campo | Plantel | Faixa em campo | No teto |
|---|---|---|---|---|---|
| 1ª (50) | 0 | 42,5 | 40,0 | 35,1 – 49,6 | 4% |
| 1ª (50) | 1 | 39,3 | 38,7 | 30,4 – 48,3 | 1% |
| 1ª (50) | 2 | 38,1 | 37,5 | 23,1 – 49,1 | 4% |
| 1ª (50) | 3 | 38,9 | 38,3 | 25,0 – 49,6 | 7% |
| 1ª (50) | 4 | 38,5 | 38,0 | 24,2 – 49,7 | 9% |
| 1ª (50) | 5 | 38,9 | 38,4 | 25,2 – 49,9 | 9% |
| 1ª (50) | 6 | 38,6 | 38,2 | 23,7 – 49,9 | 11% |
| 2ª (30) | 0 | 26,6 | 24,2 | 23,2 – 29,7 | 10% |
| 2ª (30) | 1 | 23,4 | 22,9 | 18,4 – 29,0 | 2% |
| 2ª (30) | 2 | 23,2 | 22,8 | 17,2 – 30,2 | 17% |
| 2ª (30) | 3 | 22,6 | 22,3 | 17,6 – 29,9 | 7% |
| 2ª (30) | 4 | 22,7 | 22,4 | 17,4 – 30,2 | 11% |
| 2ª (30) | 5 | 22,6 | 22,2 | 17,5 – 29,8 | 7% |
| 2ª (30) | 6 | 22,7 | 22,4 | 17,3 – 30,1 | 11% |

## D · crescem 5/3/1, com piso

| Divisão (teto) | Temporada | Em campo | Plantel | Faixa em campo | No teto |
|---|---|---|---|---|---|
| 1ª (50) | 0 | 42,5 | 40,0 | 35,1 – 49,6 | 4% |
| 1ª (50) | 1 | 40,8 | 39,9 | 30,9 – 49,4 | 5% |
| 1ª (50) | 2 | 40,2 | 39,3 | 24,5 – 50,0 | 16% |
| 1ª (50) | 3 | 40,9 | 40,2 | 25,8 – 50,0 | 22% |
| 1ª (50) | 4 | 41,1 | 40,4 | 25,5 – 50,0 | 26% |
| 1ª (50) | 5 | 41,3 | 40,7 | 26,2 – 50,0 | 28% |
| 1ª (50) | 6 | 41,2 | 40,6 | 25,2 – 50,0 | 31% |
| 2ª (30) | 0 | 26,6 | 24,2 | 23,2 – 29,7 | 10% |
| 2ª (30) | 1 | 24,8 | 24,1 | 19,1 – 29,8 | 10% |
| 2ª (30) | 2 | 24,7 | 24,1 | 17,4 – 30,2 | 26% |
| 2ª (30) | 3 | 24,6 | 24,0 | 17,9 – 30,0 | 21% |
| 2ª (30) | 4 | 24,5 | 24,0 | 17,3 – 30,0 | 24% |
| 2ª (30) | 5 | 24,6 | 24,1 | 17,7 – 30,0 | 23% |
| 2ª (30) | 6 | 24,6 | 24,2 | 17,3 – 30,1 | 27% |

## E · crescem 5/3/1, sem piso

| Divisão (teto) | Temporada | Em campo | Plantel | Faixa em campo | No teto |
|---|---|---|---|---|---|
| 1ª (50) | 0 | 42,5 | 40,0 | 35,1 – 49,6 | 4% |
| 1ª (50) | 1 | 40,8 | 39,8 | 30,5 – 49,5 | 5% |
| 1ª (50) | 2 | 40,2 | 38,9 | 24,6 – 50,0 | 18% |
| 1ª (50) | 3 | 40,2 | 38,8 | 24,8 – 50,0 | 25% |
| 1ª (50) | 4 | 39,9 | 38,6 | 23,4 – 50,0 | 29% |
| 1ª (50) | 5 | 39,7 | 38,3 | 22,8 – 50,0 | 34% |
| 1ª (50) | 6 | 39,4 | 38,1 | 22,1 – 50,0 | 36% |
| 2ª (30) | 0 | 26,6 | 24,2 | 23,2 – 29,7 | 10% |
| 2ª (30) | 1 | 24,8 | 24,0 | 18,7 – 29,9 | 10% |
| 2ª (30) | 2 | 24,6 | 23,6 | 16,3 – 30,1 | 23% |
| 2ª (30) | 3 | 23,9 | 22,7 | 15,0 – 30,1 | 24% |
| 2ª (30) | 4 | 23,1 | 21,7 | 11,9 – 30,1 | 22% |
| 2ª (30) | 5 | 22,3 | 20,7 | 9,8 – 30,1 | 27% |
| 2ª (30) | 6 | 21,1 | 19,4 | 7,0 – 30,2 | 26% |

