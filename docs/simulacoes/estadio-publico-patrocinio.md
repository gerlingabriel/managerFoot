# Simulação de estádio, público e patrocínio

Gerado por `RelatorioEstadioTest` (ferramenta de desenvolvimento). As 6 ligas do banco padrão, 3 sementes, 6 temporadas seguidas com promoção e rebaixamento, evolução de força (§6.1, rev. 56), caixa da rodada, prêmio da liga e patrocínio fixo. Sem mercado. Só máquina.

## Regras simuladas (proposta do agente, `ParametrosPublico.padrao()`)

- **Torcida** do clube: a curva de 5.000 a 80.000 pela força em campo atual (média dos 11 mais fortes).
- **Estádio inicial**: 75% da torcida pela força cadastrada; máximo de 80.000 para todos.
- **Ocupação** (fração da torcida): 75% no jogo do líder, caindo em linha reta até 15% no do último; receber o líder soma até 15%, menos quanto pior o mandante (o último não ganha nada); nas 3 últimas rodadas, 1º contra 2º soma mais 15%. Antes da rodada 5 a tabela atual vale menos: mistura com a mesma conta feita pela posição final da temporada passada (na 1ª temporada, com 45%). Sorteio de ±8%. Nunca passa do estádio.
- **Ampliação**: 5.000 lugares por R$ 1.746.000 (preço de um jogador de força 50), valem depois de 6 rodadas; uma obra por vez. A máquina amplia quando o jogo grande (líder contra o 2º) já passa do estádio com o bloco novo e sobram 1,5 limite de caixa, ou com 3 limites de caixa sobrando, até 105% da torcida.

## Ocupação do estádio por posição na tabela antes do jogo (fração do estádio)

| Divisão | Topo | Meio | Fundo | Líder × 2º | Jogos acima de 90% | Ocupação máxima com estádio acima da torcida |
|---|---|---|---|---|---|---|
| 1ª | 73% | 45% | 22% | 91% | 3% | 100% |
| 2ª | 72% | 46% | 22% | 83% | 4% | 98% |

## Estádio e caixa por temporada (média por clube)

| Divisão | Temporada | Estádio | Ampliações na temporada | Bilheteria | Resultado da temporada | Saldo no fim | Clubes negativos |
|---|---|---|---|---|---|---|---|
| 1ª | 1 | 39.645 | 0,7 | R$ 16.658.721 | R$ 4.126.338 | R$ 6.603.186 | 12% |
| 1ª | 2 | 41.051 | 0,4 | R$ 16.504.279 | R$ 4.946.840 | R$ 12.375.701 | 5% |
| 1ª | 3 | 42.951 | 0,2 | R$ 17.077.898 | R$ 5.628.898 | R$ 18.734.958 | 6% |
| 1ª | 4 | 42.778 | 0,1 | R$ 17.144.199 | R$ 5.777.678 | R$ 25.382.780 | 5% |
| 1ª | 5 | 43.677 | 0,1 | R$ 17.682.988 | R$ 6.093.252 | R$ 32.204.451 | 5% |
| 1ª | 6 | 43.650 | 0,0 | R$ 17.611.610 | R$ 6.076.758 | R$ 39.370.628 | 3% |
| 2ª | 1 | 11.253 | 0,0 | R$ 4.742.836 | R$ 1.174.132 | R$ 1.888.884 | 14% |
| 2ª | 2 | 12.636 | 0,0 | R$ 4.791.616 | R$ 1.244.353 | R$ 2.667.828 | 14% |
| 2ª | 3 | 11.764 | 0,0 | R$ 4.324.587 | R$ 1.016.231 | R$ 3.178.532 | 24% |
| 2ª | 4 | 12.525 | 0,0 | R$ 4.484.299 | R$ 1.099.524 | R$ 3.754.211 | 23% |
| 2ª | 5 | 12.071 | 0,0 | R$ 4.343.738 | R$ 1.045.680 | R$ 4.444.927 | 22% |
| 2ª | 6 | 12.438 | 0,0 | R$ 4.412.005 | R$ 1.072.710 | R$ 4.688.298 | 25% |

## Patrocínio: quanto cada proposta teria pago (média por clube e temporada)

Proposta por vitória: 60% da tabela à vista e o resto dividido pelas vitórias de quem vence 37% dos jogos. Proposta por acesso: 50% à vista e 1,5 vezes a tabela se subir (na 1ª, se ficar entre os 3 primeiros). Terço pela posição final.

| Divisão | Terço final | Fixa | Por vitória | Por acesso | Melhor: fixa / vitória / acesso |
|---|---|---|---|---|---|
| 1ª | Topo | R$ 294.347 | R$ 350.351 | R$ 360.038 | 1% / 55% / 44% |
| 1ª | Meio | R$ 209.431 | R$ 209.384 | R$ 105.054 | 55% / 45% / 0% |
| 1ª | Fundo | R$ 140.318 | R$ 117.443 | R$ 70.489 | 100% / 0% / 0% |
| 2ª | Topo | R$ 67.924 | R$ 79.830 | R$ 84.870 | 1% / 55% / 44% |
| 2ª | Meio | R$ 48.324 | R$ 47.392 | R$ 24.392 | 69% / 31% / 0% |
| 2ª | Fundo | R$ 36.944 | R$ 31.121 | R$ 18.722 | 100% / 0% / 0% |
