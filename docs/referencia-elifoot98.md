# Referência: mecânicas do Elifoot 98

- **Fonte:** https://github.com/trsthales/desvendando-elifoot-98
- **Inspecionado em:** 2026-10-26 (clone do branch padrão, profundidade 1)
- **Conteúdo do repositório:** `README.md`, `ROADMAP.md`, `docs/parte2.md`,
  `src/match_engine.c`, `src/team_calc.c`, `src/player_evolution.c` e 3 imagens.

## Natureza da fonte (ler antes de usar)

- O repositório é resultado de **engenharia reversa** do `ELIFOOT.EXE`
  (Delphi 1.0, 16 bits) com Ghidra e Cheat Engine.
- Os arquivos em `src/` são **pseudocódigo em C reconstruído**, não o código
  original. Alguns valores aparecem de forma diferente entre o README e o
  pseudocódigo (ver "Inconsistências na fonte").
- Licença declarada: finalidade educacional e de preservação. O Elifoot 98
  pertence a André Elias. **Decisão:** o projeto reimplementa as mecânicas a
  partir desta descrição e não copia o código (ver
  [ADR 0003](decisions/0003-uso-da-referencia-elifoot98.md)).

Legenda usada abaixo:
- **Confirmado na fonte:** descrito no README e no pseudocódigo de forma
  coerente.
- **Parcial:** descrito, mas com lacunas ou divergência interna.
- **Ausente:** a fonte não cobre o tema.

## 1. Jogador

**Confirmado na fonte:**
- Força nominal de **1 a 50**.
- Posições: 0 = goleiro, 1 = defesa, 2 = médio, 3 = ataque.
- Nacionalidade como sigla de 3 letras (`BRA`, `ING`, `POR`...).
- Contrato representado como **contador de jogos restantes**.
- Salário/valor de mercado em um único campo.
- Não há atributos ocultos (fôlego, passe, potencial etc.).
- Contrato e salário **não** afetam o rendimento em campo.

**Parcial:** existe um campo "estado disciplinar / cartões e lesões", sem
regra documentada.

## 2. Força dos setores antes da partida

**Confirmado na fonte** (`team_calc.c`, `FUN_17fb_397d`):

```
Poder do setor = Σ titulares do setor ( força × 2
                                      + 3 se nacionalidade == país do clube
                                      + ajuste de posição )
```

- Há 4 setores: goleiro, defesa, meio e ataque.
- **Bônus de nacionalidade:** +3 por titular do mesmo país do clube.
- **Ajuste de posição:** existe ("penalidade se fora de posição"), mas o
  valor não é documentado.
- Médios contribuem **50% no ataque e 50% na defesa**, segundo o README. Por
  isso a formação 5-0-5 é a mais eficiente no jogo original. O pseudocódigo
  não mostra como essa divisão é feita.

## 3. Motor de partidas

**Confirmado na fonte** (`match_engine.c`):
- O relógio vai de 1 a 90, minuto a minuto. Não há acréscimos documentados.
- **Pênalti:** 1% de chance por minuto (`Random(1000) < 10`). O lado
  beneficiado é sorteado com 50/50. O duelo é atacante 45 contra
  defesa adversária ÷ 4.
- **Chance principal por minuto:**
  - mandante: `Random(6000) < força ofensiva`;
  - visitante: `Random(7000) < força ofensiva`.
- **Chance secundária ("anti-goleada" e sorte):**
  - divisor 90 em casa e 120 fora;
  - *lucky roll* de 1/180 em casa e 1/270 fora;
  - o limiar diminui conforme aumentam as finalizações e os gols do próprio
    time.
- **Duelo de finalização:**
  - `Random(ataque/3)` contra `Random(defesa/3)`;
  - com 25% de chance, há um desvio (poste, barra ou fora);
  - sem desvio, gol se o atacante tirar valor maior, senão defesa do goleiro.
- **Desfechos:** gol, defesa, poste, barra, fora.

**Inconsistências na fonte:**
- O limiar da chance secundária é `Limiar − (Remates + Gols)` no README, mas
  `150 − (remates + gols × 3)` no pseudocódigo.
- O README lista "Para fora" como um desfecho. A Parte 2 lista "Ao lado" e
  "Por cima" separadamente.
- O pseudocódigo do pênalti usa a força de **defesa** adversária ÷ 4 como
  "goleiro". Não está claro se é o setor goleiro ou o setor defesa.

**Ausente:** como o finalizador é escolhido, cartões durante a partida
(além da roleta), lesões, substituições, expulsões e seus efeitos, e
prorrogação ou disputa de pênaltis.

## 4. Disciplina

**Confirmado na fonte** (`player_evolution.c`, `FUN_17fb_100b`):
- O infrator de uma falta ou cartão é escolhido por **roleta ponderada**
  entre os titulares.
- Pesos por comportamento: Muito correto 1, Correto 2, Normal 4,
  Agressivo 7, Muito agressivo 10.
- O comportamento **não** dá bônus de desarme.

**Ausente:** frequência de faltas e cartões por minuto, regra de
amarelo/vermelho e suspensões.

## 5. Evolução e queda de força

**Confirmado na fonte** (`FUN_17fb_4a41`). O processamento é feito após cada
rodada, para cada jogador:
- **Avaliação positiva:** +1 de força.
  - Goleiros precisam passar por um sorteio extra de 50%.
  - O limite é 50 e a força não pode passar de "nível base do clube" + 5.
- **Avaliação negativa:** −1 de força, com 50% de chance.
  - O mínimo é 1 e a força não pode cair abaixo de "nível base do clube" − 5.

**Parcial:**
- A função que decide se a avaliação é positiva ou negativa
  (`AvaliarRendimento`) **não é documentada**. O README diz apenas que
  depende de "moral e titularidade".
- O cálculo do "nível base do clube" não é documentado.
- A frase "no Elifoot 98 se perde 1 de força" (SPEC §6.1) é compatível com a
  fonte: a perda é de 1 ponto, com 50% de chance e respeitando a âncora de −5.
  Não se sabe quais jogadores recebem avaliação negativa.

## 6. Temas não cobertos pela fonte

O próprio `ROADMAP.md` da referência lista estes temas como pendentes:
- Mercado de transferências e leilão (lances e preço da IA).
- Regras de demissão e teto de caixa negativo.
- Finanças: bilheteria, preço do ingresso, salários, juros, patrocínio,
  prêmios.
- Formato dos arquivos `.SAV` / `.EFT`.
- Jogadores com asterisco. Só é citado como "mito" a ser abordado na Parte 4,
  ainda não publicada.
- Árbitros.

Também não há nada sobre lesões, moral numérica, copas, competições
continentais, estádio ou público.

**Consequência para o projeto:** as fórmulas de economia (preço × força ×
salário × receita × prêmio × patrocínio) **não podem ser copiadas da
referência**. Precisarão de tabelas próprias, validadas por simulação
(SPEC §9).

## 7. Divergências entre a referência e a SPEC atual

São escolhas a confirmar. A SPEC prevalece sobre a referência (CLAUDE.md §2).

| Tema | Elifoot 98 (fonte) | SPEC atual |
|---|---|---|
| Meio-campo | Divide a força 50/50 entre ataque e defesa; 5-0-5 domina | **Definido:** meio contra meio decide o domínio; mais meias dominam mais e finalizam menos; 5-0-5 não existe (§7.1, §7.2) |
| Nacionalidade | +3 por titular do país do clube | **Retirado** (decisão do usuário, rev. 3); há limite de estrangeiros com países equiparados (§8.4) |
| Disciplina | Pesos 1/2/4/7/10 | **Definido:** pesos menores 1/2/4/6/8, mais um ajuste de frequência pela soma do time (§7.4) |
| Mando de campo | Divisores fixos 6000 × 7000 | Vantagem configurável, medida por simulação |
| Contrato | Contador de jogos | "Prazo" (unidade a definir) |
