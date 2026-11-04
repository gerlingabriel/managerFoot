# Expulsão e domínio do meio

Gerado por `RelatorioExpulsaoTest` (SPEC §7.4, rev. 98). Cada cenário: 40000 partidas, sementes 0 a 39999 (reproduzível), 4-4-2 × 4-4-2, temperamento normal.
As expulsões acontecem naturalmente (cerca de 0,2 por partida). Contam só as partidas em que um time
terminou com mais expulsos que o outro; o resultado é o desse time.

- **Domínio:** quanto a chance do adversário de ficar com a jogada cresce (0 = regra anterior, só o buraco
  no setor; 0,5 = rev. 98, 50/50 vira 75/25).
- **Lances extras:** lance a mais por minuto para quem tem um a mais, em fração da chance normal de lance
  (1 = o dobro de lances desse time; proposta da rev. 99).

## Times iguais (25 × 25)

| Domínio | Lances extras | Partidas com expulso a mais | Quem tem o expulso perde | Empata | Vence | Vence ou empata, expulsão no 1º tempo | Idem, no 2º tempo | Gols por partida (todas) | Gols do adversário depois da expulsão |
|---|---|---|---|---|---|---|---|---|---|
| 0,0 | 0,0 | 6894 | 36,5% | 29,8% | 33,7% | 60,6% | 65,4% | 2,18 | 0,50 |
| 0,5 | 0,0 | 6894 | 49,5% | 27,2% | 23,3% | 38,8% | 58,0% | 2,19 | 0,75 |
| 0,5 | 0,5 | 6899 | 59,8% | 22,2% | 18,0% | 24,2% | 50,4% | 2,26 | 1,17 |
| 0,5 | 1,0 | 6854 | 66,2% | 19,0% | 14,8% | 15,6% | 45,3% | 2,33 | 1,55 |
| 0,5 | 2,0 | 6867 | 75,8% | 13,4% | 10,8% | 6,0% | 35,7% | 2,45 | 2,24 |
| 0,5 | 3,0 | 6886 | 80,3% | 11,5% | 8,2% | 3,2% | 30,1% | 2,57 | 2,89 |

## Mandante mais forte (30 × 20)

| Domínio | Lances extras | Partidas com expulso a mais | Quem tem o expulso perde | Empata | Vence | Vence ou empata, expulsão no 1º tempo | Idem, no 2º tempo | Gols por partida (todas) | Gols do adversário depois da expulsão |
|---|---|---|---|---|---|---|---|---|---|
| 0,0 | 0,0 | 6859 | 42,2% | 17,1% | 40,7% | 55,5% | 59,3% | 2,52 | 0,57 |
| 0,5 | 0,0 | 6863 | 49,2% | 16,1% | 34,7% | 44,1% | 55,1% | 2,53 | 0,81 |
| 0,5 | 0,5 | 6887 | 55,2% | 14,7% | 30,1% | 35,9% | 50,6% | 2,61 | 1,24 |
| 0,5 | 1,0 | 6831 | 59,7% | 13,6% | 26,7% | 28,8% | 47,8% | 2,68 | 1,61 |
| 0,5 | 2,0 | 6852 | 67,6% | 11,3% | 21,1% | 17,7% | 41,9% | 2,80 | 2,33 |
| 0,5 | 3,0 | 6866 | 72,6% | 9,8% | 17,6% | 11,1% | 37,8% | 2,92 | 2,97 |

## Visitante mais forte (20 × 30)

| Domínio | Lances extras | Partidas com expulso a mais | Quem tem o expulso perde | Empata | Vence | Vence ou empata, expulsão no 1º tempo | Idem, no 2º tempo | Gols por partida (todas) | Gols do adversário depois da expulsão |
|---|---|---|---|---|---|---|---|---|---|
| 0,0 | 0,0 | 6843 | 38,5% | 25,2% | 36,3% | 60,6% | 62,1% | 2,33 | 0,52 |
| 0,5 | 0,0 | 6844 | 49,9% | 23,9% | 26,1% | 41,5% | 55,5% | 2,33 | 0,77 |
| 0,5 | 0,5 | 6867 | 59,0% | 19,7% | 21,3% | 28,8% | 48,9% | 2,41 | 1,22 |
| 0,5 | 1,0 | 6792 | 64,6% | 17,0% | 18,4% | 19,6% | 45,5% | 2,48 | 1,62 |
| 0,5 | 2,0 | 6835 | 72,8% | 13,2% | 14,0% | 11,2% | 37,5% | 2,60 | 2,33 |
| 0,5 | 3,0 | 6850 | 78,1% | 10,9% | 11,0% | 6,3% | 31,9% | 2,73 | 3,02 |

