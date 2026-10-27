# Calibração do motor de partidas

Gerado por `RelatorioCalibracaoTest` (ferramenta de desenvolvimento; SPEC §7.6).
Cada cenário: 10000 partidas, sementes 0 a 9999 (reproduzível).
Todos os titulares de um time têm a mesma força; temperamento normal salvo indicação.

## Parâmetros usados

| Parâmetro | Valor |
|---|---|
| chancePorMinuto | 0,150 |
| fatorMando | 1,45 |
| conversaoBase | 0,24 |
| sensibilidadeForca | 0,85 |
| fatorPorGolMarcado | 0,85 |
| conversaoMinima / Maxima | 0,09 / 0,60 |
| cartoesPorMinuto (por time) | 0,0119 |
| proporcaoVermelhoDireto | 0,05 |
| penaltisPorMinuto | 0,0033 |
| conversão do pênalti | 0,75 |

## Times equilibrados (25 × 25, 4-4-2 × 4-4-2)

| Métrica | Medido | Meta (§7.6) |
|---|---|---|
| Gols por partida | 2,10 | 1,75 a 2,5 |
| Vitória mandante | 44,8% | ~45% |
| Empate | 29,5% | ~27% |
| Vitória visitante | 25,7% | ~28% |
| Amarelos por partida | 2,04 | ~2 |
| Expulsões por partida | 0,20 | 0,1 a 0,2 |
| Pênaltis por partida | 0,29 | ~0,3 |
| Conversão de pênaltis | 75,0% | 75% |
| Partidas com 5+ gols de um time | 0,6% | raras |

Placares mais comuns (mandante × visitante):

| Placar | Frequência |
|---|---|
| 1x1 | 15,2% |
| 1x0 | 14,9% |
| 0x1 | 10,2% |
| 0x0 | 10,2% |
| 2x0 | 10,1% |
| 2x1 | 9,1% |
| 1x2 | 6,5% |
| 0x2 | 4,8% |
| 2x2 | 3,8% |
| 3x1 | 3,6% |
| 3x0 | 3,4% |
| 1x3 | 1,6% |

## Zebras (time mais fraco em casa, 4-4-2 × 4-4-2)

| Força casa × fora | Diferença | Fraco vence | Empate | Forte vence | Gols/partida | Meta fraco vence |
|---|---|---|---|---|---|---|
| 20 × 30 | 10 | 18,7% | 27,2% | 54,2% | 2,19 | 15% a 20% |
| 20 × 40 | 20 | 9,9% | 19,5% | 70,6% | 2,56 | 5% a 10% |
| 10 × 45 | 35 | 0,9% | 3,8% | 95,3% | 3,96 | 1% a 3% |
| 1 × 50 | 49 | 0,0% | 0,1% | 99,9% | 6,34 | ≥ ~1% (piso) |

## Formações contra 4-4-2 (força 25 × 25, formação testada em casa)

| Formação | Estilo | Gols pró | Gols contra | Vitória | Empate | Derrota |
|---|---|---|---|---|---|---|
| 4-4-2 | NORMAL | 1,23 | 0,87 | 44,8% | 29,5% | 25,7% |
| 3-5-2 | NORMAL | 1,32 | 0,88 | 47,1% | 28,7% | 24,1% |
| 4-3-3 | OFENSIVO | 1,34 | 0,99 | 44,7% | 28,4% | 26,9% |
| 3-4-3 | OFENSIVO | 1,48 | 0,98 | 49,2% | 27,0% | 23,8% |
| 4-2-4 | OFENSIVO | 1,29 | 1,16 | 39,7% | 27,8% | 32,6% |
| 5-4-1 | DEFENSIVO | 0,89 | 0,80 | 35,3% | 33,9% | 30,9% |
| 4-5-1 | DEFENSIVO | 0,95 | 0,78 | 37,7% | 33,4% | 28,9% |
| 3-6-1 | DEFENSIVO | 1,01 | 0,80 | 38,9% | 32,6% | 28,4% |

## Temperamento e cartões (25 × 25, 4-4-2; todos os titulares com o temperamento indicado)

| Temperamento do mandante | Amarelos/partida (mandante) | Expulsões/partida (mandante) |
|---|---|---|
| CALMO | 0,51 | 0,040 |
| EDUCADO | 0,71 | 0,061 |
| NORMAL | 1,02 | 0,098 |
| ANSIOSO | 1,25 | 0,131 |
| VIOLENTO | 1,44 | 0,163 |

