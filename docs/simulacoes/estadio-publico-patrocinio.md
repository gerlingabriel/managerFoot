# Simulação de estádio, público e patrocínio

Gerado por `RelatorioEstadioTest` (ferramenta de desenvolvimento). As 6 ligas do banco padrão, 3 sementes, 6 temporadas seguidas com promoção e rebaixamento, evolução de força (§6.1, rev. 56), caixa da rodada, prêmio da liga e patrocínio fixo. Sem mercado. Só máquina.

## Regras simuladas (proposta do agente, `ParametrosPublico.padrao()`)

- **Torcida** do clube: a curva de 5.000 a 80.000 pela força em campo atual (média dos 11 mais fortes).
- **Estádio inicial**: 75% da torcida pela força cadastrada; máximo de 80.000 para todos.
- **Ocupação** (fração da torcida): 75% no jogo do líder, caindo em linha reta até 15% no do último; receber o líder soma até 15%, menos quanto pior o mandante (o último não ganha nada); nas 3 últimas rodadas, 1º contra 2º soma mais 15%. Antes da rodada 5 a tabela vale menos (mistura com 45%). Sorteio de ±8%. Nunca passa do estádio.
- **Ampliação**: 5.000 lugares por R$ 1.587.000 (preço de um jogador de força 50), valem depois de 6 rodadas; uma obra por vez. A máquina amplia quando o jogo grande (líder contra o 2º) já passa do estádio com o bloco novo e sobram 1,5 limite de caixa, ou com 3 limites de caixa sobrando, até 105% da torcida.

## Ocupação do estádio por posição na tabela antes do jogo (fração do estádio)

| Divisão | Topo | Meio | Fundo | Líder × 2º | Jogos acima de 90% | Ocupação máxima com estádio acima da torcida |
|---|---|---|---|---|---|---|
| 1ª | 72% | 45% | 22% | 91% | 3% | 100% |
| 2ª | 73% | 47% | 23% | 84% | 4% | 90% |
| 3ª | 79% | 48% | 26% | 95% | 8% | 90% |

## Estádio e caixa por temporada (média por clube)

| Divisão | Temporada | Estádio | Ampliações na temporada | Bilheteria | Resultado da temporada | Saldo no fim | Clubes negativos |
|---|---|---|---|---|---|---|---|
| 1ª | 1 | 39.299 | 0,6 | R$ 16.289.327 | R$ 3.625.657 | R$ 6.204.763 | 15% |
| 1ª | 2 | 39.931 | 0,4 | R$ 15.765.726 | R$ 3.978.372 | R$ 11.070.379 | 7% |
| 1ª | 3 | 42.213 | 0,2 | R$ 16.166.682 | R$ 4.414.148 | R$ 16.188.488 | 11% |
| 1ª | 4 | 41.718 | 0,1 | R$ 16.528.359 | R$ 4.793.989 | R$ 21.982.973 | 7% |
| 1ª | 5 | 43.075 | 0,1 | R$ 16.752.712 | R$ 4.822.787 | R$ 27.436.824 | 8% |
| 1ª | 6 | 42.641 | 0,1 | R$ 16.725.020 | R$ 4.912.084 | R$ 33.290.743 | 7% |
| 2ª | 1 | 10.790 | 0,0 | R$ 4.567.718 | R$ 780.506 | R$ 1.564.739 | 21% |
| 2ª | 2 | 12.419 | 0,0 | R$ 4.791.981 | R$ 880.891 | R$ 2.295.964 | 17% |
| 2ª | 3 | 11.064 | 0,0 | R$ 4.225.053 | R$ 620.485 | R$ 2.805.600 | 17% |
| 2ª | 4 | 12.171 | 0,0 | R$ 4.466.576 | R$ 709.408 | R$ 3.111.708 | 16% |
| 2ª | 5 | 11.335 | 0,0 | R$ 4.201.391 | R$ 616.480 | R$ 3.831.271 | 12% |
| 2ª | 6 | 11.994 | 0,0 | R$ 4.380.123 | R$ 690.673 | R$ 4.062.181 | 16% |
| 3ª | 1 | 5.910 | 0,0 | R$ 2.662.634 | R$ 501.808 | R$ 948.178 | 21% |
| 3ª | 2 | 6.018 | 0,0 | R$ 2.558.143 | R$ 472.508 | R$ 998.302 | 27% |
| 3ª | 3 | 6.120 | 0,0 | R$ 2.567.371 | R$ 469.755 | R$ 1.083.041 | 39% |
| 3ª | 4 | 6.045 | 0,0 | R$ 2.535.205 | R$ 472.608 | R$ 1.263.251 | 39% |
| 3ª | 5 | 6.102 | 0,0 | R$ 2.514.775 | R$ 444.931 | R$ 1.223.142 | 40% |
| 3ª | 6 | 6.115 | 0,0 | R$ 2.532.777 | R$ 472.915 | R$ 1.526.656 | 40% |

## Patrocínio: quanto cada proposta teria pago (média por clube e temporada)

Proposta por vitória: 60% da tabela à vista e o resto dividido pelas vitórias de quem vence 37% dos jogos. Proposta por acesso: 50% à vista e 1,5 vezes a tabela se subir (na 1ª, se ficar entre os 3 primeiros). Terço pela posição final.

| Divisão | Terço final | Fixa | Por vitória | Por acesso | Melhor: fixa / vitória / acesso |
|---|---|---|---|---|---|
| 1ª | Topo | R$ 267.283 | R$ 319.711 | R$ 329.812 | 1% / 56% / 44% |
| 1ª | Meio | R$ 189.430 | R$ 189.231 | R$ 95.126 | 59% / 41% / 0% |
| 1ª | Fundo | R$ 132.586 | R$ 110.633 | R$ 66.679 | 100% / 0% / 0% |
| 2ª | Topo | R$ 65.981 | R$ 78.176 | R$ 82.522 | 1% / 55% / 44% |
| 2ª | Meio | R$ 49.875 | R$ 48.912 | R$ 25.191 | 66% / 34% / 0% |
| 2ª | Fundo | R$ 42.969 | R$ 36.058 | R$ 21.784 | 100% / 0% / 0% |
| 3ª | Topo | R$ 29.454 | R$ 37.017 | R$ 37.971 | 0% / 49% / 51% |
| 3ª | Meio | R$ 24.370 | R$ 24.576 | R$ 12.292 | 52% / 48% / 0% |
| 3ª | Fundo | R$ 18.817 | R$ 14.879 | R$ 9.635 | 100% / 0% / 0% |
