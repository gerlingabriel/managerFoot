# Simulação de lesões

Gerado por `RelatorioLesoesTest` (ferramenta de desenvolvimento). Uma temporada de cada país do banco padrão (6 países, 2 divisões) por semente, sementes 0 a 9; escalação da máquina para todos, evolução de força ligada, limite de 2 lesões por clube na temporada (SPEC §7.5).

*Frequência* é a taxa por minuto expressa em lesões por clube numa temporada de 38 rodadas, sem o limite; a 2ª divisão tem menos rodadas.

| Frequência | Lesões por clube | Clubes com 0 / 1 / 2 | Leve / média / grave | Jogos fora por lesão | Jogos sem o lesionado por clube | Força perdida por clube | Jogos com time incompleto |
|---|---|---|---|---|---|---|---|
| 1,0 | 0,83 | 43% / 32% / 26% | 60% / 29% / 11% | 3,3 | 2,5 | 2,7 | 0 |
| 1,5 | 1,12 | 30% / 28% / 42% | 60% / 30% / 10% | 3,2 | 3,4 | 3,6 | 0 |
| 2,0 | 1,35 | 21% / 23% / 56% | 60% / 29% / 11% | 3,3 | 4,2 | 4,4 | 0 |
| 3,0 | 1,59 | 13% / 14% / 72% | 62% / 29% / 10% | 3,2 | 4,9 | 5,1 | 0 |

*Jogos com time incompleto*: partidas em que um clube tinha menos de 11 disponíveis (lesionados e suspensos) e jogou com um a menos (SPEC §9.3).

## Quem se lesiona (frequência 1,5)

| Temperamento | No plantel | Nas lesões |
|---|---|---|
| Calmo | 20% | 30% |
| Educado | 20% | 25% |
| Normal | 39% | 34% |
| Ansioso | 13% | 9% |
| Violento | 7% | 2% |
