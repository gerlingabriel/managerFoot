# Cartões, trava das goleadas e lesões

Gerado por `RelatorioCartoesTest` (SPEC §7.4 e §7.5, rev. 131). Sementes 0 a N−1 (reproduzível). Os times têm
temperamentos misturados (2 calmos, 2 educados, 3 normais, 2 ansiosos e 2 violentos em cada lado), força 25 × 25,
4-4-2 × 4-4-2.

## Vermelho direto

A fração dos cartões que é vermelho direto no minuto m é *fração no 90 × (m / 90)^expoente*. Expoente 0 é a
regra até a rev. 130 (a mesma em todo o jogo); 1 cresce em linha reta do zero. 40000 partidas por linha.

| Fração no 90 | Expoente | Expulsões por jogo | Vermelho direto | Dois amarelos | Expulsões por dois amarelos | Até o minuto 15 | No 1º tempo | Amarelos por jogo | Gols por jogo |
|---|---|---|---|---|---|---|---|---|---|
| 5% | 0 | 0,226 | 0,109 | 0,117 | 51,6% | 9,2% | 36,5% | 2,06 | 2,43 |
| 4% | 1 | 0,169 | 0,044 | 0,124 | 73,8% | 2,7% | 24,8% | 2,13 | 2,34 |
| 3% | 1 | 0,158 | 0,033 | 0,125 | 79,3% | 2,7% | 24,8% | 2,14 | 2,33 |
| 2% | 1 | 0,149 | 0,022 | 0,127 | 85,1% | 2,6% | 24,4% | 2,15 | 2,33 |
| 4% | 2 | 0,156 | 0,030 | 0,126 | 80,7% | 2,4% | 22,5% | 2,14 | 2,33 |

## Trava das goleadas por expulso a mais

A trava (fator 0,4 por gol) começa quando a vantagem de quem tem jogador a mais passa de
*gols livres × expulsos a mais*. 1 é a regra até a rev. 130 (a partir de 2 gols, qualquer que seja o número de
expulsos); 2 faz 1 expulso começar em 3 gols e 2 expulsos em 5. 300000 partidas por linha, com o vermelho
da regra atual; o resultado é visto por quem tem o expulso, pelo número de expulsos a mais no fim.

| Cenário | Gols livres | Expulsos a mais | Partidas | Gols do adversário | Perde por 3+ | Perde por 5+ | Adversário faz 5+ |
|---|---|---|---|---|---|---|---|
| 25 × 25 | 1 | 1 | 38644 | 2,10 | 18,5% | 0,2% | 1,8% |
| 25 × 25 | 1 | 2 ou mais | 1375 | 3,14 | 53,9% | 5,1% | 13,5% |
| 25 × 25 | 2 | 1 | 38610 | 2,29 | 29,3% | 1,1% | 4,7% |
| 25 × 25 | 2 | 2 ou mais | 1401 | 3,57 | 58,0% | 22,1% | 30,7% |
| 30 × 20 | 1 | 1 | 38535 | 2,11 | 26,8% | 1,0% | 2,7% |
| 30 × 20 | 1 | 2 ou mais | 1360 | 3,09 | 53,6% | 6,9% | 13,8% |
| 30 × 20 | 2 | 1 | 38569 | 2,32 | 36,2% | 3,2% | 6,7% |
| 30 × 20 | 2 | 2 ou mais | 1341 | 3,58 | 55,0% | 28,8% | 35,3% |

## Lesões

Frequência em lesões por clube em 38 jogos, antes do limite da temporada. *Por clube na temporada*: a
conta esperada com o limite, para 38 jogos (a copa e a continental somam alguns jogos a mais). 40000 partidas por linha.

| Frequência | Lesões por jogo (os dois times) | Por clube na temporada, limite 2 | Limite 3 | Limite 4 | Clubes que batem o limite 2 | Limite 3 | Limite 4 |
|---|---|---|---|---|---|---|---|
| 1,5 | 0,077 | 1,21 | 1,38 | 1,44 | 43% | 18% | 6% |
| 2,5 | 0,132 | 1,65 | 2,11 | 2,35 | 72% | 46% | 24% |
| 3,0 | 0,157 | 1,77 | 2,35 | 2,70 | 81% | 58% | 35% |
| 4,0 | 0,209 | 1,90 | 2,68 | 3,25 | 92% | 77% | 57% |

