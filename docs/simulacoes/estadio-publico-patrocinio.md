# Simulação de estádio, público e patrocínio

Gerado por `RelatorioEstadioTest` (ferramenta de desenvolvimento). As 6 ligas do banco padrão, 3 sementes, 6 temporadas seguidas com promoção e rebaixamento, evolução de força (§6.1, rev. 56), caixa da rodada, prêmio da liga e patrocínio fixo. Sem mercado. Só máquina.

## Regras simuladas (proposta do agente, `ParametrosPublico.padrao()`)

- **Torcida** do clube: a curva de 5.000 a 80.000 pela força em campo atual (média dos 11 mais fortes).
- **Estádio inicial**: 75% da torcida pela força cadastrada; máximo de 80.000 para todos.
- **Ocupação** (fração da torcida): 75% no jogo do líder, caindo em linha reta até 15% no do último; receber o líder soma até 15%, menos quanto pior o mandante (o último não ganha nada); nas 3 últimas rodadas, 1º contra 2º soma mais 15%. Antes da rodada 5 a tabela atual vale menos: mistura com a mesma conta feita pela posição final da temporada passada (na 1ª temporada, com 45%). Sorteio de ±8%. Nunca passa do estádio.
- **Ampliação**: 5.000 lugares por R$ 1.587.000 (preço de um jogador de força 50), valem depois de 6 rodadas; uma obra por vez. A máquina amplia quando o jogo grande (líder contra o 2º) já passa do estádio com o bloco novo e sobram 1,5 limite de caixa, ou com 3 limites de caixa sobrando, até 105% da torcida.

## Ocupação do estádio por posição na tabela antes do jogo (fração do estádio)

| Divisão | Topo | Meio | Fundo | Líder × 2º | Jogos acima de 90% | Ocupação máxima com estádio acima da torcida |
|---|---|---|---|---|---|---|
| 1ª | 72% | 45% | 22% | 91% | 3% | 100% |
| 2ª | 71% | 46% | 22% | 83% | 4% | 99% |

## Estádio e caixa por temporada (média por clube)

| Divisão | Temporada | Estádio | Ampliações na temporada | Bilheteria | Resultado da temporada | Saldo no fim | Clubes negativos |
|---|---|---|---|---|---|---|---|
| 1ª | 1 | 38.968 | 0,6 | R$ 16.348.196 | R$ 3.765.131 | R$ 6.395.618 | 11% |
| 1ª | 2 | 40.270 | 0,5 | R$ 16.192.548 | R$ 4.439.692 | R$ 11.721.623 | 3% |
| 1ª | 3 | 42.405 | 0,1 | R$ 16.435.876 | R$ 4.927.628 | R$ 17.241.647 | 5% |
| 1ª | 4 | 41.931 | 0,1 | R$ 16.559.055 | R$ 5.126.246 | R$ 23.288.369 | 3% |
| 1ª | 5 | 42.996 | 0,1 | R$ 16.873.517 | R$ 5.189.546 | R$ 29.155.796 | 2% |
| 1ª | 6 | 43.136 | 0,1 | R$ 17.187.542 | R$ 5.430.669 | R$ 35.380.429 | 2% |
| 2ª | 1 | 11.352 | 0,0 | R$ 4.732.076 | R$ 1.264.194 | R$ 1.968.811 | 11% |
| 2ª | 2 | 12.677 | 0,0 | R$ 4.798.181 | R$ 1.404.319 | R$ 2.809.002 | 8% |
| 2ª | 3 | 11.701 | 0,0 | R$ 4.227.419 | R$ 1.130.155 | R$ 3.552.181 | 15% |
| 2ª | 4 | 12.631 | 0,0 | R$ 4.479.971 | R$ 1.230.937 | R$ 4.119.368 | 20% |
| 2ª | 5 | 12.120 | 0,0 | R$ 4.269.310 | R$ 1.152.713 | R$ 4.905.572 | 19% |
| 2ª | 6 | 12.384 | 0,0 | R$ 4.342.762 | R$ 1.195.195 | R$ 5.524.797 | 23% |

## Patrocínio: quanto cada proposta teria pago (média por clube e temporada)

Proposta por vitória: 60% da tabela à vista e o resto dividido pelas vitórias de quem vence 37% dos jogos. Proposta por acesso: 50% à vista e 1,5 vezes a tabela se subir (na 1ª, se ficar entre os 3 primeiros). Terço pela posição final.

| Divisão | Terço final | Fixa | Por vitória | Por acesso | Melhor: fixa / vitória / acesso |
|---|---|---|---|---|---|
| 1ª | Topo | R$ 265.923 | R$ 315.664 | R$ 320.304 | 0% / 56% / 44% |
| 1ª | Meio | R$ 189.108 | R$ 189.602 | R$ 94.962 | 58% / 42% / 0% |
| 1ª | Fundo | R$ 134.502 | R$ 112.855 | R$ 67.645 | 100% / 0% / 0% |
| 2ª | Topo | R$ 65.283 | R$ 76.984 | R$ 81.388 | 1% / 55% / 44% |
| 2ª | Meio | R$ 47.152 | R$ 45.944 | R$ 23.809 | 73% / 27% / 0% |
| 2ª | Fundo | R$ 37.676 | R$ 31.687 | R$ 19.086 | 100% / 0% / 0% |

## O que cada faixa de força alcança na temporada (para as metas do patrocínio)

Faixa pela força em campo no início da temporada, dentro da divisão. Vitórias só na liga.

| Divisão | Faixa | Vitórias: mediana | 80% ficam até | 90% ficam até | Campeão | Até 3º | Até 6º | Metade de cima |
|---|---|---|---|---|---|---|---|---|
| 1ª | 4 mais fortes | 20 | 23 | 25 | 19% | 53% | 84% | 98% |
| 1ª | 5º ao 10º | 16 | 19 | 21 | 4% | 14% | 40% | 75% |
| 1ª | 11º em diante | 10 | 13 | 15 | 0% | 0% | 2% | 15% |
| 2ª | 4 mais fortes | 20 | 24 | 26 | 22% | 55% | 83% | 96% |
| 2ª | 5º ao 10º | 15 | 19 | 20 | 2% | 12% | 38% | 71% |
| 2ª | 11º em diante | 10 | 13 | 15 | 0% | 1% | 4% | 18% |
