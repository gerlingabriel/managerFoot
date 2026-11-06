# Desgaste da força: por que o clube da 2ª afunda (SPEC rev. 126)

Estudo de 2026-11-06, a pedido do usuário. Ele assumiu um clube da 2ª divisão. O bônus do treinador (rev. 121) deixou o time mais forte, mas, mesmo com uma campanha razoável, a força geral caiu bastante. Ele queria uma queda menor, ou quase manter.

## O que se viu no jogo do usuário

No jogo salvo dele (França, 2ª temporada, rodada 7), os dois clubes que ele treinou na 1ª temporada estavam com **quase todo o plantel no piso** (força do clube − 5): Dijon (força 23) com 16 dos 17 jogadores em 18 e Annecy (25) com 17 dos 20 em 20. Na Ligue 2, a maioria dos clubes tinha 2/3 ou mais do plantel no piso. A exceção era o clube que veio da fila, onde não se joga.

## Por quê

Pela rev. 56, quem joga só perde força: em cada partida, 1, 3 ou 5 dos que jogaram perdem 1 ponto (vitória, empate, derrota). Quem não joga cresce (4, 2 ou 0,5). Num time que perde tanto quanto ganha, o saldo por partida é negativo: com 35% de vitórias, 30% de empates e 35% de derrotas, cerca de 3 pontos perdidos contra 2 ganhos. Em uma temporada, os titulares encostam no piso e os reservas sobem até virar titulares e se desgastar também.

## Variantes simuladas

Jogo completo (`RelatorioJogoCompletoIT`), 6 países × 1 semente × 10 temporadas, só máquina, base da rev. 124 (a rev. 125 só muda a tela do usuário).

| | Atual (1/3/5, piso − 5) | V2: 1/2/3, piso − 5 | **V3: 1/2/3, piso − 3 (aprovada)** |
|---|---|---|---|
| Força em campo 1ª, temporada 1 → 10 | 42,5 → 39,0 | 42,5 → 41,9 | 42,5 → 42,3 |
| Força em campo 2ª, temporada 1 → 10 | 26,6 → 22,7 | 26,6 → 25,4 | 26,6 → 26,3 |
| Caixa da 1ª na 10ª temporada (rodadas da folha, mediana) | 74 | 59 | 48 |
| Clubes da 2ª negativos na 10ª temporada | 14% | 22% | 14% |
| Vendas por temporada (média; na 10ª) | 16,5 (7) | 20 (14) | 21 (11) |
| Campeão da 1ª entre os 3 mais fortes | 82% | 63% | 52% |
| Mesmo campeão do ano anterior | 28% | 17% | 13% |
| Promovidos que caem de volta | 62% | 53% | 65% |
| Gols por jogo | 2,8 | 3,0 | 2,9 |

- **V3** mantém a força inicial das duas divisões. Como a força fica mais alta, a folha também fica, e a 1ª acumula menos caixa.
- **V2** também mantém a força, mas a folha mais alta com caixa curto deixa mais clubes da 2ª negativos.
- Nas duas, o campeonato fica menos previsível.

Variante descartada no mesmo estudo, para o caixa da 1ª: **V1**, o clube rico (caixa acima de 30 rodadas da folha e plantel acima de 23) põe a reserva mais cara em leilão. As vendas sobem de 16,5 para 49 por temporada (65 na 10ª), mas o caixa da 1ª fica igual (73), porque a venda dentro da divisão só troca o dinheiro de mãos.

## Um clube só, sem o resto da liga

Simulação direta da regra de evolução (`EvolucaoForca`), 91 clubes da 2ª do banco padrão (força 20 a 27, plantel médio de 20), 40 jogos, 20 sementes, com os resultados sorteados pela campanha. "Máquina" escala sempre os 11 mais fortes; "time fixo", os mesmos 11 do início. Variação da força em campo (11 mais fortes) em uma temporada:

| Campanha (V/E/D) | Atual: máquina | Atual: time fixo | V3: máquina | V3: time fixo |
|---|---|---|---|---|
| Boa (50/25/25) | −1,0 | +1,1 | +0,2 | +1,4 |
| Razoável (35/30/35) | −3,2 | −1,1 | −1,4 | −0,5 |
| Ruim (25/25/50) | −4,6 | −3,0 | −2,5 | −1,7 |

No jogo completo a queda é maior que aqui, porque entram vendas, lesões e jogos de copa.
