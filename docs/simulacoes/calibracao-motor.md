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
| jogo aberto com 0 / 1 / 2 / 3 / 4 atacantes | 1,00 / 1,00 / 1,00 / 1,15 / 1,30 |
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

Do ponto de vista da formação testada; *Total*: gols da partida.

| Formação | Estilo | Gols pró | Gols contra | Total | Vitória | Empate | Derrota |
|---|---|---|---|---|---|---|---|
| 4-4-2 | NORMAL | 1,43 | 1,04 | 2,46 | 46,4% | 26,4% | 27,1% |
| 3-5-2 | NORMAL | 1,48 | 1,16 | 2,64 | 45,0% | 25,5% | 29,6% |
| 4-3-3 | OFENSIVO | 1,68 | 1,23 | 2,91 | 48,0% | 24,0% | 28,0% |
| 3-4-3 | OFENSIVO | 1,76 | 1,37 | 3,13 | 47,1% | 23,1% | 29,8% |
| 4-2-4 | OFENSIVO | 1,86 | 1,49 | 3,34 | 46,9% | 21,8% | 31,3% |
| 5-4-1 | DEFENSIVO | 1,19 | 0,90 | 2,08 | 43,0% | 29,7% | 27,4% |
| 4-5-1 | DEFENSIVO | 1,23 | 0,98 | 2,21 | 42,3% | 28,8% | 28,9% |
| 3-6-1 | DEFENSIVO | 1,27 | 1,11 | 2,38 | 40,3% | 27,9% | 31,8% |

## Formações contra 4-4-2 (força 25 × 25, formação testada fora)

Do ponto de vista da formação testada; *Total*: gols da partida.

| Formação | Estilo | Gols pró | Gols contra | Total | Vitória | Empate | Derrota |
|---|---|---|---|---|---|---|---|
| 4-4-2 | NORMAL | 1,04 | 1,43 | 2,46 | 27,1% | 26,4% | 46,4% |
| 3-5-2 | NORMAL | 1,09 | 1,62 | 2,70 | 25,3% | 25,1% | 49,7% |
| 4-3-3 | OFENSIVO | 1,21 | 1,65 | 2,86 | 27,4% | 24,1% | 48,5% |
| 3-4-3 | OFENSIVO | 1,29 | 1,87 | 3,16 | 26,2% | 22,3% | 51,5% |
| 4-2-4 | OFENSIVO | 1,30 | 1,93 | 3,23 | 25,0% | 21,9% | 53,1% |
| 5-4-1 | DEFENSIVO | 0,85 | 1,24 | 2,09 | 25,2% | 29,1% | 45,7% |
| 4-5-1 | DEFENSIVO | 0,90 | 1,37 | 2,27 | 24,7% | 27,3% | 48,1% |
| 3-6-1 | DEFENSIVO | 0,94 | 1,56 | 2,50 | 22,5% | 25,1% | 52,5% |

## Todas as formações contra todas (força 25 × 25, ida e volta, 2000 partidas por jogo)

Pontos por jogo (vitória 3, empate 1) e contra quantas das outras 7 formações tem saldo positivo de vitórias.

| Formação | Estilo | Pontos por jogo | Supera |
|---|---|---|---|
| 4-4-2 | NORMAL | 1,43 | 6 de 7 |
| 3-5-2 | NORMAL | 1,37 | 3 de 7 |
| 4-3-3 | OFENSIVO | 1,45 | 7 de 7 |
| 3-4-3 | OFENSIVO | 1,40 | 4 de 7 |
| 4-2-4 | OFENSIVO | 1,37 | 2 de 7 |
| 5-4-1 | DEFENSIVO | 1,38 | 5 de 7 |
| 4-5-1 | DEFENSIVO | 1,34 | 1 de 7 |
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
| 1 | 1,413 |
| 2 | 1,434 |
| 3 | 1,446 |
| 4 | 1,450 |
| 5 | 1,450 |
| 6 | 1,449 |
| 7 | 1,444 |

