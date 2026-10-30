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
| 2ª | 70% | 46% | 22% | 79% | 4% | 100% |

## Estádio e caixa por temporada (média por clube)

| Divisão | Temporada | Estádio | Ampliações na temporada | Bilheteria | Resultado da temporada | Saldo no fim | Clubes negativos |
|---|---|---|---|---|---|---|---|
| 1ª | 1 | 39.256 | 0,6 | R$ 16.517.196 | R$ 3.805.590 | R$ 6.438.541 | 10% |
| 1ª | 2 | 40.400 | 0,5 | R$ 16.227.954 | R$ 4.444.315 | R$ 11.772.819 | 3% |
| 1ª | 3 | 42.281 | 0,2 | R$ 16.484.808 | R$ 4.939.377 | R$ 17.328.415 | 6% |
| 1ª | 4 | 41.883 | 0,1 | R$ 16.603.087 | R$ 5.196.175 | R$ 23.389.261 | 5% |
| 1ª | 5 | 42.931 | 0,1 | R$ 16.823.303 | R$ 5.203.651 | R$ 29.343.914 | 4% |
| 1ª | 6 | 42.832 | 0,1 | R$ 16.993.620 | R$ 5.370.603 | R$ 35.559.499 | 3% |
| 2ª | 1 | 11.388 | 0,1 | R$ 4.758.400 | R$ 1.254.074 | R$ 1.960.671 | 10% |
| 2ª | 2 | 12.847 | 0,0 | R$ 4.789.705 | R$ 1.383.429 | R$ 2.762.403 | 11% |
| 2ª | 3 | 11.924 | 0,0 | R$ 4.293.376 | R$ 1.174.106 | R$ 3.538.050 | 15% |
| 2ª | 4 | 12.761 | 0,0 | R$ 4.455.429 | R$ 1.235.724 | R$ 4.190.248 | 20% |
| 2ª | 5 | 12.134 | 0,0 | R$ 4.253.889 | R$ 1.142.590 | R$ 4.867.488 | 21% |
| 2ª | 6 | 12.527 | 0,0 | R$ 4.389.085 | R$ 1.222.135 | R$ 5.498.452 | 22% |

## Patrocínio: quanto cada proposta teria pago (média por clube e temporada)

Proposta por vitória: 60% da tabela à vista e o resto dividido pelas vitórias de quem vence 37% dos jogos. Proposta por acesso: 50% à vista e 1,5 vezes a tabela se subir (na 1ª, se ficar entre os 3 primeiros). Terço pela posição final.

| Divisão | Terço final | Fixa | Por vitória | Por acesso | Melhor: fixa / vitória / acesso |
|---|---|---|---|---|---|
| 1ª | Topo | R$ 269.612 | R$ 318.807 | R$ 328.764 | 1% / 55% / 44% |
| 1ª | Meio | R$ 186.706 | R$ 187.384 | R$ 93.759 | 57% / 43% / 0% |
| 1ª | Fundo | R$ 133.035 | R$ 111.906 | R$ 66.912 | 100% / 0% / 0% |
| 2ª | Topo | R$ 65.679 | R$ 77.389 | R$ 81.194 | 2% / 54% / 44% |
| 2ª | Meio | R$ 47.019 | R$ 45.969 | R$ 23.730 | 69% / 31% / 0% |
| 2ª | Fundo | R$ 37.377 | R$ 31.395 | R$ 18.951 | 100% / 0% / 0% |

## O que cada faixa de força alcança na temporada (para as metas do patrocínio)

Faixa pela força em campo no início da temporada, dentro da divisão. Vitórias só na liga.

| Divisão | Faixa | Vitórias: mediana | 80% ficam até | 90% ficam até | Campeão | Até 3º | Até 6º | Metade de cima |
|---|---|---|---|---|---|---|---|---|
| 1ª | 4 mais fortes | 20 | 23 | 25 | 21% | 56% | 88% | 97% |
| 1ª | 5º ao 10º | 16 | 19 | 21 | 3% | 12% | 37% | 74% |
| 1ª | 11º em diante | 10 | 14 | 15 | 0% | 0% | 3% | 15% |
| 2ª | 4 mais fortes | 20 | 24 | 26 | 22% | 55% | 79% | 94% |
| 2ª | 5º ao 10º | 15 | 18 | 20 | 2% | 12% | 41% | 73% |
| 2ª | 11º em diante | 10 | 13 | 15 | 0% | 1% | 4% | 18% |
