# 0017 — Mundial de Clubes na temporada

- **Status:** Aceita (SPEC §8.3.4, rev. 88)
- **Data:** 2026-10-30

## Contexto

- O Mundial é disputado depois da final da continental: duas semifinais e uma final, em jogo único.
- O jogo só tinha clubes da região do país jogado. O Mundial precisa de clubes das outras regiões, inclusive de
  regiões sem liga nem continental no banco padrão.
- Partidas, eventos, caixa e rodada ao vivo já tratam os confrontos da copa e da continental. O extrato agrupa os
  lançamentos por rodada e competição, e a semifinal e a final de um mesmo clube não podem cair na mesma linha.

## Opções consideradas

1. **Tabelas próprias para o Mundial:** isolamento, mas duplica partidas, eventos e tela ao vivo.
2. **Reusar `jogo_copa_confronto` com `competicao = 'MUNDIAL'`**, com partidas de perna `JOGO_UNICO`.
3. **Simular a continental das outras regiões:** dá campeões de verdade, a um custo alto, em jogos que ninguém vê.

## Decisão

Opção 2, sem simular as outras regiões: o representante delas sai por força com sorteio (V39 e V40).

- O domínio é o `Mundial`, à parte da `Copa`, porque a semifinal da copa é de ida e volta.
- A rodada das etapas é a última da liga + 1 e + 2: separa as linhas do extrato sem tocar `rodadas_processadas`.
- `jogo_clube.regiao_mundial` marca os clubes de fora que concorrem. Na criação, copiam-se os 8 mais fortes de cada
  região fora a do país jogado.

## Consequências

- Jogos criados antes do banco padrão versão 5 não têm esses clubes e ficam sem Mundial; a temporada deles segue.
- O campeão continental de fora do país jogado também vai ao Mundial, e o usuário só joga se o clube dele chegar lá.
- Os clubes copiados viram clubes de fora: entram na vitrine e na reposição da virada, como os da continental.
