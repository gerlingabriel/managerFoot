# Expulsão e domínio do meio

Gerado por `RelatorioExpulsaoTest` (SPEC §7.4, rev. 98). Cada cenário: 40000 partidas, sementes 0 a 39999 (reproduzível), 4-4-2 × 4-4-2, temperamento normal.
As expulsões acontecem naturalmente (cerca de 0,2 por partida). Contam só as partidas em que um time
terminou com mais expulsos que o outro; o resultado é o desse time. As tabelas de domínio e lances extras
usam a trava das goleadas padrão (rev. 114).

- **Domínio:** quanto a chance do adversário de ficar com a jogada cresce (0 = regra anterior, só o buraco
  no setor; 0,5 = rev. 98, 50/50 vira 75/25).
- **Lances extras:** lance a mais por minuto para quem tem um a mais, em fração da chance normal de lance
  (1 = o dobro de lances desse time; proposta da rev. 99).

## Times iguais (25 × 25)

| Domínio | Lances extras | Partidas com expulso a mais | Quem tem o expulso perde | Empata | Vence | Vence ou empata, expulsão no 1º tempo | Idem, no 2º tempo | Gols por partida (todas) | Gols do adversário depois da expulsão |
|---|---|---|---|---|---|---|---|---|---|
| 0,0 | 0,0 | 6895 | 36,4% | 29,8% | 33,7% | 60,6% | 65,4% | 2,18 | 0,48 |
| 0,5 | 0,0 | 6895 | 49,5% | 27,2% | 23,3% | 38,8% | 58,0% | 2,18 | 0,69 |
| 0,5 | 0,5 | 6888 | 59,7% | 22,2% | 18,1% | 24,4% | 50,4% | 2,23 | 1,01 |
| 0,5 | 1,0 | 6854 | 66,2% | 19,0% | 14,8% | 15,7% | 45,3% | 2,28 | 1,28 |
| 0,5 | 2,0 | 6876 | 75,8% | 13,5% | 10,8% | 6,2% | 35,8% | 2,36 | 1,70 |
| 0,5 | 3,0 | 6872 | 80,3% | 11,5% | 8,2% | 3,1% | 30,1% | 2,41 | 2,00 |

## Mandante mais forte (30 × 20)

| Domínio | Lances extras | Partidas com expulso a mais | Quem tem o expulso perde | Empata | Vence | Vence ou empata, expulsão no 1º tempo | Idem, no 2º tempo | Gols por partida (todas) | Gols do adversário depois da expulsão |
|---|---|---|---|---|---|---|---|---|---|
| 0,0 | 0,0 | 6859 | 42,2% | 17,2% | 40,7% | 55,6% | 59,3% | 2,51 | 0,49 |
| 0,5 | 0,0 | 6862 | 49,2% | 16,1% | 34,7% | 44,2% | 55,1% | 2,50 | 0,66 |
| 0,5 | 0,5 | 6888 | 55,2% | 14,7% | 30,1% | 35,9% | 50,6% | 2,56 | 0,96 |
| 0,5 | 1,0 | 6832 | 59,6% | 13,6% | 26,7% | 29,0% | 47,7% | 2,60 | 1,19 |
| 0,5 | 2,0 | 6847 | 67,5% | 11,3% | 21,2% | 18,0% | 42,0% | 2,67 | 1,60 |
| 0,5 | 3,0 | 6880 | 72,6% | 9,9% | 17,5% | 11,4% | 37,7% | 2,73 | 1,91 |

## Visitante mais forte (20 × 30)

| Domínio | Lances extras | Partidas com expulso a mais | Quem tem o expulso perde | Empata | Vence | Vence ou empata, expulsão no 1º tempo | Idem, no 2º tempo | Gols por partida (todas) | Gols do adversário depois da expulsão |
|---|---|---|---|---|---|---|---|---|---|
| 0,0 | 0,0 | 6846 | 38,5% | 25,2% | 36,3% | 60,5% | 62,1% | 2,32 | 0,49 |
| 0,5 | 0,0 | 6849 | 49,9% | 23,9% | 26,1% | 41,5% | 55,5% | 2,32 | 0,69 |
| 0,5 | 0,5 | 6860 | 58,9% | 19,8% | 21,3% | 29,0% | 48,9% | 2,37 | 1,02 |
| 0,5 | 1,0 | 6809 | 64,6% | 17,0% | 18,4% | 19,7% | 45,4% | 2,42 | 1,27 |
| 0,5 | 2,0 | 6823 | 72,6% | 13,2% | 14,1% | 11,5% | 37,5% | 2,49 | 1,66 |
| 0,5 | 3,0 | 6843 | 78,1% | 11,0% | 10,9% | 6,3% | 31,9% | 2,55 | 1,98 |

## Trava das goleadas com expulso (rev. 114)

Com jogador a mais e 2 gols ou mais de vantagem, a conversão de quem tem um a mais é multiplicada pela trava
a cada gol de vantagem além do primeiro (1 = sem trava; padrão 0,40). *Sem expulso*: partidas que terminaram sem expulso a mais. *Perde por 4+* e *Adversário faz 5+*: do ponto
de vista de quem tem o expulso.

| Cenário | Trava | Gols, sem expulso | Diferença 4+, sem expulso | 5+ de um time, sem expulso | Gols, com expulso | Perde | Empata | Perde por 4+ | Adversário faz 5+ |
|---|---|---|---|---|---|---|---|---|---|
| 25 × 25 | 1,00 | 2,19 | 1,8% | 0,7% | 3,73 | 75,8% | 13,4% | 22,4% | 17,9% |
| 25 × 25 | 0,65 | 2,19 | 1,7% | 0,6% | 3,35 | 75,8% | 13,4% | 12,0% | 6,7% |
| 25 × 25 | 0,50 | 2,19 | 1,7% | 0,6% | 3,24 | 75,8% | 13,4% | 7,2% | 3,9% |
| 25 × 25 | 0,40 | 2,19 | 1,6% | 0,6% | 3,18 | 75,8% | 13,5% | 5,2% | 3,1% |
| 25 × 25 | 0,30 | 2,18 | 1,6% | 0,6% | 3,12 | 75,8% | 13,4% | 2,8% | 2,0% |
| 30 × 20 | 1,00 | 2,53 | 8,1% | 3,1% | 4,12 | 67,6% | 11,3% | 30,5% | 24,3% |
| 30 × 20 | 0,65 | 2,53 | 8,0% | 3,0% | 3,61 | 67,5% | 11,3% | 19,3% | 10,0% |
| 30 × 20 | 0,50 | 2,53 | 8,0% | 3,0% | 3,47 | 67,5% | 11,3% | 13,7% | 5,6% |
| 30 × 20 | 0,40 | 2,53 | 7,9% | 3,0% | 3,39 | 67,5% | 11,3% | 9,7% | 3,9% |
| 30 × 20 | 0,30 | 2,53 | 7,9% | 3,0% | 3,32 | 67,5% | 11,3% | 6,7% | 2,8% |
| 20 × 30 | 1,00 | 2,34 | 3,1% | 1,3% | 3,91 | 72,8% | 13,2% | 27,7% | 22,3% |
| 20 × 30 | 0,65 | 2,33 | 3,1% | 1,2% | 3,44 | 72,7% | 13,2% | 15,7% | 8,9% |
| 20 × 30 | 0,50 | 2,33 | 3,0% | 1,2% | 3,32 | 72,7% | 13,2% | 10,4% | 4,9% |
| 20 × 30 | 0,40 | 2,33 | 3,0% | 1,2% | 3,24 | 72,6% | 13,2% | 7,0% | 3,2% |
| 20 × 30 | 0,30 | 2,33 | 3,0% | 1,1% | 3,17 | 72,6% | 13,3% | 4,3% | 2,4% |

