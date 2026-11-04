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
| Gols por partida | 2,46 | 1,75 a 2,5 |
| Vitória mandante | 46,4% | ~45% |
| Empate | 26,4% | ~27% |
| Vitória visitante | 27,1% | ~28% |
| Amarelos por partida | 2,04 | ~2 |
| Expulsões por partida | 0,20 | 0,1 a 0,2 |
| Pênaltis por partida | 0,30 | ~0,3 |
| Conversão de pênaltis | 76,4% | 75% |
| Partidas com 5+ gols de um time | 4,0% | raras |

Placares mais comuns (mandante × visitante):

| Placar | Frequência |
|---|---|
| 1x1 | 13,7% |
| 1x0 | 12,9% |
| 2x1 | 9,4% |
| 2x0 | 9,4% |
| 0x1 | 8,4% |
| 0x0 | 8,1% |
| 1x2 | 6,5% |
| 3x0 | 4,2% |
| 2x2 | 4,1% |
| 0x2 | 4,1% |
| 3x1 | 3,6% |
| 1x3 | 2,0% |

## Zebras (time mais fraco em casa, 4-4-2 × 4-4-2)

| Força casa × fora | Diferença | Fraco vence | Empate | Forte vence | Gols/partida | Meta fraco vence |
|---|---|---|---|---|---|---|
| 20 × 30 | 10 | 20,0% | 23,6% | 56,4% | 2,61 | 15% a 20% |
| 20 × 40 | 20 | 11,2% | 17,0% | 71,8% | 3,08 | 5% a 10% |
| 10 × 45 | 35 | 2,9% | 5,5% | 91,6% | 4,18 | 1% a 3% |
| 1 × 50 | 49 | 0,6% | 1,3% | 98,2% | 5,12 | ≥ ~1% (piso) |

## Formações contra 4-4-2 (força 25 × 25, formação testada em casa)

| Formação | Estilo | Gols pró | Gols contra | Vitória | Empate | Derrota |
|---|---|---|---|---|---|---|
| 4-4-2 | NORMAL | 1,43 | 1,04 | 46,4% | 26,4% | 27,1% |
| 3-5-2 | NORMAL | 1,48 | 1,16 | 45,0% | 25,5% | 29,6% |
| 4-3-3 | OFENSIVO | 1,50 | 1,11 | 46,3% | 26,0% | 27,7% |
| 3-4-3 | OFENSIVO | 1,57 | 1,23 | 45,8% | 24,9% | 29,3% |
| 4-2-4 | OFENSIVO | 1,50 | 1,20 | 44,4% | 25,5% | 30,2% |
| 5-4-1 | DEFENSIVO | 1,19 | 0,90 | 43,0% | 29,7% | 27,4% |
| 4-5-1 | DEFENSIVO | 1,23 | 0,98 | 42,3% | 28,8% | 28,9% |
| 3-6-1 | DEFENSIVO | 1,27 | 1,11 | 40,3% | 27,9% | 31,8% |

## Todas as formações contra todas (força 25 × 25, ida e volta, 2000 partidas por jogo)

Pontos por jogo (vitória 3, empate 1) e contra quantas das outras 7 formações tem saldo positivo de vitórias.

| Formação | Estilo | Pontos por jogo | Supera |
|---|---|---|---|
| 4-4-2 | NORMAL | 1,43 | 7 de 7 |
| 3-5-2 | NORMAL | 1,37 | 3 de 7 |
| 4-3-3 | OFENSIVO | 1,42 | 6 de 7 |
| 3-4-3 | OFENSIVO | 1,39 | 4 de 7 |
| 4-2-4 | OFENSIVO | 1,35 | 2 de 7 |
| 5-4-1 | DEFENSIVO | 1,38 | 5 de 7 |
| 4-5-1 | DEFENSIVO | 1,35 | 1 de 7 |
| 3-6-1 | DEFENSIVO | 1,27 | 0 de 7 |

## Temperamento e cartões (25 × 25, 4-4-2; todos os titulares com o temperamento indicado)

| Temperamento do mandante | Amarelos/partida (mandante) | Expulsões/partida (mandante) |
|---|---|---|
| CALMO | 0,71 | 0,062 |
| EDUCADO | 0,85 | 0,077 |
| NORMAL | 1,02 | 0,100 |
| ANSIOSO | 1,13 | 0,114 |
| VIOLENTO | 1,21 | 0,125 |

## Sorte e azar da temporada (25 × 25, 4-4-2; SPEC §5.3.1)

Meta: cerca de 5 pontos percentuais a mais de vitórias (beneficiado) e de derrotas (prejudicado).

| Cenário | Vitórias do clube sorteado | Empates | Derrotas do clube sorteado | Diferença vs. neutro (V / D) | Gols/partida |
|---|---|---|---|---|---|
| BENEFICIADO em casa | 50,2% | 25,4% | 24,5% | +3,8 p.p. / -2,7 p.p. | 2,60 |
| BENEFICIADO fora | 32,1% | 26,6% | 41,3% | +5,0 p.p. / -5,2 p.p. | 2,61 |
| PREJUDICADO em casa | 41,4% | 26,9% | 31,7% | -5,0 p.p. / +4,5 p.p. | 2,47 |
| PREJUDICADO fora | 23,3% | 25,5% | 51,3% | -3,9 p.p. / +4,8 p.p. | 2,46 |

## Asteriscos em campo (25 × 25, 4-4-2; SPEC §5.3)

Asteriscos dados primeiro aos atacantes, depois aos meias, depois aos defensores. Pontos por jogo
contra um time sem asterisco, média de casa e fora. Meta: do 4º em diante, quase sem ganho.

| Asteriscos | Pontos por jogo |
|---|---|
| 0 | 1,368 |
| 1 | 1,417 |
| 2 | 1,440 |
| 3 | 1,450 |
| 4 | 1,453 |
| 5 | 1,452 |
| 6 | 1,450 |
| 7 | 1,445 |

