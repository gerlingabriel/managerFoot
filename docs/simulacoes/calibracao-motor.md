# Calibração do motor de partidas

Gerado por `RelatorioCalibracaoTest` (ferramenta de desenvolvimento; SPEC §7.6).
Cada cenário: 10000 partidas, sementes 0 a 9999 (reproduzível).
Todos os titulares de um time têm a mesma força; temperamento normal salvo indicação.

## Parâmetros usados

| Parâmetro | Valor |
|---|---|
| chancePorMinuto | 0,150 |
| fatorMando | 1,45 |
| conversaoBase | 0,23 |
| sensibilidadeForca | 0,95 |
| expoentes meias / atacantes / defensores | 0,50 / 0,55 / 1,25 |
| fatorPorGolMarcado | 0,85 |
| conversaoMinima / Maxima | 0,10 / 0,35 |
| cartoesPorMinuto (por time) | 0,0119 |
| proporcaoVermelhoDireto | 0,05 |
| penaltisPorMinuto | 0,0033 |
| conversão do pênalti | 0,75 |

## Times equilibrados (25 × 25, 4-4-2 × 4-4-2)

| Métrica | Medido | Meta (§7.6) |
|---|---|---|
| Gols por partida | 2,18 | 1,75 a 2,5 |
| Vitória mandante | 45,1% | ~45% |
| Empate | 29,4% | ~27% |
| Vitória visitante | 25,6% | ~28% |
| Amarelos por partida | 2,04 | ~2 |
| Expulsões por partida | 0,20 | 0,1 a 0,2 |
| Pênaltis por partida | 0,29 | ~0,3 |
| Conversão de pênaltis | 74,8% | 75% |
| Partidas com 5+ gols de um time | 0,7% | raras |

Placares mais comuns (mandante × visitante):

| Placar | Frequência |
|---|---|
| 1x1 | 15,3% |
| 1x0 | 14,0% |
| 2x0 | 10,0% |
| 0x1 | 9,7% |
| 0x0 | 9,5% |
| 2x1 | 9,4% |
| 1x2 | 6,5% |
| 0x2 | 4,8% |
| 2x2 | 4,2% |
| 3x1 | 3,8% |
| 3x0 | 3,5% |
| 3x2 | 1,7% |

## Zebras (time mais fraco em casa, 4-4-2 × 4-4-2)

| Força casa × fora | Diferença | Fraco vence | Empate | Forte vence | Gols/partida | Meta fraco vence |
|---|---|---|---|---|---|---|
| 20 × 30 | 10 | 17,6% | 25,4% | 57,1% | 2,32 | 15% a 20% |
| 20 × 40 | 20 | 9,2% | 17,4% | 73,4% | 2,78 | 5% a 10% |
| 10 × 45 | 35 | 1,5% | 5,6% | 92,9% | 3,85 | 1% a 3% |
| 1 × 50 | 49 | 0,1% | 1,1% | 98,9% | 4,75 | ≥ ~1% (piso) |

## Formações contra 4-4-2 (força 25 × 25, formação testada em casa)

| Formação | Estilo | Gols pró | Gols contra | Vitória | Empate | Derrota |
|---|---|---|---|---|---|---|
| 4-4-2 | NORMAL | 1,27 | 0,91 | 45,1% | 29,4% | 25,6% |
| 3-5-2 | NORMAL | 1,32 | 1,02 | 43,3% | 28,5% | 28,2% |
| 4-3-3 | OFENSIVO | 1,35 | 0,98 | 45,6% | 28,2% | 26,2% |
| 3-4-3 | OFENSIVO | 1,42 | 1,09 | 44,8% | 27,4% | 27,8% |
| 4-2-4 | OFENSIVO | 1,34 | 1,07 | 42,4% | 28,6% | 29,0% |
| 5-4-1 | DEFENSIVO | 1,05 | 0,78 | 41,3% | 32,2% | 26,6% |
| 4-5-1 | DEFENSIVO | 1,09 | 0,85 | 40,4% | 31,5% | 28,1% |
| 3-6-1 | DEFENSIVO | 1,12 | 0,97 | 38,2% | 30,6% | 31,2% |

## Todas as formações contra todas (força 25 × 25, ida e volta, 2000 partidas por jogo)

Pontos por jogo (vitória 3, empate 1) e contra quantas das outras 7 formações tem saldo positivo de vitórias.

| Formação | Estilo | Pontos por jogo | Supera |
|---|---|---|---|
| 4-4-2 | NORMAL | 1,42 | 7 de 7 |
| 3-5-2 | NORMAL | 1,37 | 4 de 7 |
| 4-3-3 | OFENSIVO | 1,40 | 6 de 7 |
| 3-4-3 | OFENSIVO | 1,37 | 3 de 7 |
| 4-2-4 | OFENSIVO | 1,31 | 1 de 7 |
| 5-4-1 | DEFENSIVO | 1,36 | 5 de 7 |
| 4-5-1 | DEFENSIVO | 1,34 | 2 de 7 |
| 3-6-1 | DEFENSIVO | 1,27 | 0 de 7 |

## Temperamento e cartões (25 × 25, 4-4-2; todos os titulares com o temperamento indicado)

| Temperamento do mandante | Amarelos/partida (mandante) | Expulsões/partida (mandante) |
|---|---|---|
| CALMO | 0,71 | 0,060 |
| EDUCADO | 0,85 | 0,078 |
| NORMAL | 1,02 | 0,098 |
| ANSIOSO | 1,13 | 0,113 |
| VIOLENTO | 1,21 | 0,125 |

