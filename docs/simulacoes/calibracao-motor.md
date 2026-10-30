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
| asterisco: bônus de conversão / peso na escolha / pênalti contra goleiro★ / limite / penalidade por excedente | 0,15 / 1,5 / 0,65 / 3 / 0,010 |
| sorte: chancesExtras / fatorDominioPrejudicado | 0,095 / 0,83 |

## Times equilibrados (25 × 25, 4-4-2 × 4-4-2)

| Métrica | Medido | Meta (§7.6) |
|---|---|---|
| Gols por partida | 2,19 | 1,75 a 2,5 |
| Vitória mandante | 45,6% | ~45% |
| Empate | 29,5% | ~27% |
| Vitória visitante | 24,9% | ~28% |
| Amarelos por partida | 2,04 | ~2 |
| Expulsões por partida | 0,20 | 0,1 a 0,2 |
| Pênaltis por partida | 0,31 | ~0,3 |
| Conversão de pênaltis | 76,1% | 75% |
| Partidas com 5+ gols de um time | 0,8% | raras |

Placares mais comuns (mandante × visitante):

| Placar | Frequência |
|---|---|
| 1x1 | 15,4% |
| 1x0 | 14,4% |
| 2x1 | 9,9% |
| 2x0 | 9,7% |
| 0x1 | 9,5% |
| 0x0 | 9,4% |
| 1x2 | 6,7% |
| 2x2 | 4,3% |
| 0x2 | 4,2% |
| 3x0 | 3,7% |
| 3x1 | 3,3% |
| 3x2 | 1,6% |

## Zebras (time mais fraco em casa, 4-4-2 × 4-4-2)

| Força casa × fora | Diferença | Fraco vence | Empate | Forte vence | Gols/partida | Meta fraco vence |
|---|---|---|---|---|---|---|
| 20 × 30 | 10 | 17,9% | 25,5% | 56,6% | 2,33 | 15% a 20% |
| 20 × 40 | 20 | 9,1% | 17,7% | 73,2% | 2,80 | 5% a 10% |
| 10 × 45 | 35 | 1,6% | 5,3% | 93,1% | 3,84 | 1% a 3% |
| 1 × 50 | 49 | 0,1% | 1,0% | 99,0% | 4,74 | ≥ ~1% (piso) |

## Formações contra 4-4-2 (força 25 × 25, formação testada em casa)

| Formação | Estilo | Gols pró | Gols contra | Vitória | Empate | Derrota |
|---|---|---|---|---|---|---|
| 4-4-2 | NORMAL | 1,29 | 0,91 | 45,6% | 29,5% | 24,9% |
| 3-5-2 | NORMAL | 1,34 | 1,02 | 44,1% | 28,5% | 27,4% |
| 4-3-3 | OFENSIVO | 1,36 | 0,98 | 45,6% | 28,8% | 25,6% |
| 3-4-3 | OFENSIVO | 1,43 | 1,08 | 45,3% | 27,6% | 27,1% |
| 4-2-4 | OFENSIVO | 1,35 | 1,07 | 43,4% | 28,3% | 28,2% |
| 5-4-1 | DEFENSIVO | 1,07 | 0,78 | 42,0% | 32,6% | 25,4% |
| 4-5-1 | DEFENSIVO | 1,11 | 0,85 | 41,5% | 31,7% | 26,8% |
| 3-6-1 | DEFENSIVO | 1,14 | 0,96 | 39,5% | 30,7% | 29,8% |

## Todas as formações contra todas (força 25 × 25, ida e volta, 2000 partidas por jogo)

Pontos por jogo (vitória 3, empate 1) e contra quantas das outras 7 formações tem saldo positivo de vitórias.

| Formação | Estilo | Pontos por jogo | Supera |
|---|---|---|---|
| 4-4-2 | NORMAL | 1,42 | 7 de 7 |
| 3-5-2 | NORMAL | 1,35 | 3 de 7 |
| 4-3-3 | OFENSIVO | 1,41 | 6 de 7 |
| 3-4-3 | OFENSIVO | 1,37 | 4 de 7 |
| 4-2-4 | OFENSIVO | 1,33 | 1 de 7 |
| 5-4-1 | DEFENSIVO | 1,36 | 5 de 7 |
| 4-5-1 | DEFENSIVO | 1,33 | 2 de 7 |
| 3-6-1 | DEFENSIVO | 1,25 | 0 de 7 |

## Temperamento e cartões (25 × 25, 4-4-2; todos os titulares com o temperamento indicado)

| Temperamento do mandante | Amarelos/partida (mandante) | Expulsões/partida (mandante) |
|---|---|---|
| CALMO | 0,72 | 0,063 |
| EDUCADO | 0,85 | 0,079 |
| NORMAL | 1,02 | 0,102 |
| ANSIOSO | 1,13 | 0,115 |
| VIOLENTO | 1,21 | 0,127 |

## Sorte e azar da temporada (25 × 25, 4-4-2; SPEC §5.3.1)

Meta: cerca de 5 pontos percentuais a mais de vitórias (beneficiado) e de derrotas (prejudicado).

| Cenário | Vitórias do clube sorteado | Empates | Derrotas do clube sorteado | Diferença vs. neutro (V / D) | Gols/partida |
|---|---|---|---|---|---|
| BENEFICIADO em casa | 50,2% | 27,8% | 22,1% | +4,6 p.p. / -2,9 p.p. | 2,34 |
| BENEFICIADO fora | 30,5% | 29,2% | 40,3% | +5,6 p.p. / -5,3 p.p. | 2,35 |
| PREJUDICADO em casa | 40,1% | 30,0% | 29,9% | -5,5 p.p. / +5,0 p.p. | 2,20 |
| PREJUDICADO fora | 20,8% | 28,4% | 50,9% | -4,2 p.p. / +5,3 p.p. | 2,19 |

## Asteriscos em campo (25 × 25, 4-4-2; SPEC §5.3)

Asteriscos dados primeiro aos atacantes, depois aos meias, depois aos defensores. Pontos por jogo
contra um time sem asterisco, média de casa e fora. Meta: do 4º em diante, quase sem ganho.

| Asteriscos | Pontos por jogo |
|---|---|
| 0 | 1,353 |
| 1 | 1,404 |
| 2 | 1,431 |
| 3 | 1,441 |
| 4 | 1,444 |
| 5 | 1,443 |
| 6 | 1,441 |
| 7 | 1,436 |

