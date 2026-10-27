# SPEC.md --- Especificação inicial do simulador de futebol

-   **Status:** versão inicial para revisão (rev. 14 — 2026-10-27)
-   **Objetivo:** registrar os requisitos conhecidos, separar regras
    aprovadas de hipóteses e orientar a implementação incremental.
-   **Referência de mecânicas:**
    https://github.com/trsthales/desvendando-elifoot-98 — o que foi
    confirmado nela está em `docs/referencia-elifoot98.md`.
-   **Decisões de arquitetura:** `docs/decisions/`.
-   **Princípio:** usar o Elifoot 98 como referência de experiência e
    mecânicas, verificando no código de referência o que de fato existe.
-   **Legenda:** **Definido** = aprovado pelo usuário; **Provisório** =
    proposta ajustável; **Em aberto** = exige decisão. Texto sem marcação
    descreve intenção geral ainda não detalhada.

### Histórico de revisões

-   **rev. 14 (2026-10-27):** mesmo motor em modo rápido; fluxo e ordem de processamento da rodada; referência da tela de jogos.
-   **rev. 13 (2026-10-27):** fila de espera, demissão do usuário rebaixado para fora da liga, divisões sem usuário com resultado instantâneo.
-   **rev. 12 (2026-10-27):** calendário sorteado por temporada; sorteio de desempate fixo na temporada.
-   **rev. 11 (2026-10-27):** calendário com returno espelhado e mando alternado.
-   **rev. 10 (2026-10-27):** banco de 7 reservas, substituições com mudança de formação e regra da máquina.
-   **rev. 9 (2026-10-26):** equilíbrio entre formações, efeito do temperamento reduzido, piso de zebra até 35 pontos, calibração concluída.
-   **rev. 8 (2026-10-26):** caixa de substituição na expulsão; metas de zebra com piso de 1%.
-   **rev. 7 (2026-10-26):** efeito da expulsão, goleiro expulso e escolha do batedor de pênalti.
-   **rev. 6 (2026-10-26):** pesos do finalizador para as 8 formações; finalizador escolhido com peso pela força.
-   **rev. 5 (2026-10-26):**
    -   papel dos setores (meio decide domínio);
    -   escalação presa à formação;
    -   metas de calibração do motor (§7.6);
    -   pênaltis em cerca de 0,3 por jogo.
-   **rev. 4 (2026-10-26):**
    -   pesos de cartão 1/2/4/6/8;
    -   regras de suspensão completas;
    -   limite de força 50 automático na liga principal e escolhido pelo
        usuário nas demais;
    -   clube promovido à 1ª divisão volta a poder evoluir até 50.
-   **rev. 3 (2026-10-26):**
    -   retirado o bônus de nacionalidade;
    -   regra de estrangeiros com países equiparados;
    -   suspensões (5 amarelos e expulsão de 1 a 3 jogos);
    -   divisões de 15 a 20 clubes no formato atual real;
    -   desempate para todas as posições;
    -   save local com exclusão do servidor.
-   **rev. 2 (2026-10-26):** incorporadas as respostas do usuário
    (força por divisão, moral com vitórias, pontuação e desempate,
    copas ida/volta, cartões amarelos, substituições, contas e convites,
    dados padrão, elenco de 25, moedas, desconexão, velocidade em rede,
    vagas continentais e Mundial); corrigidos erros de texto; planos de
    implementação unificados na §15.

## 1. Visão geral

O produto será um jogo de gestão e simulação de futebol inspirado no
Elifoot 98. O jogador assume o papel de treinador de um clube e
administra escalação, tática, elenco, contratos, finanças, estádio e
desempenho esportivo.

O jogo deve funcionar em modo individual e, em uma primeira versão
multiplayer, permitir que um grupo compartilhe uma mesma liga. Cada
participante controla seu próprio clube, enquanto o calendário, os
resultados e a evolução da temporada permanecem sincronizados.

A interface deve manter a simplicidade e a leitura rápida associadas ao
Elifoot 98, com modernizações visuais e novos elementos informativos.

## 2. Escopo inicial

### 2.1 Requisitos centrais

1.  Criar e validar uma estrutura de liga com uma ou mais divisões.
2.  Gerenciar clubes, treinadores, jogadores, táticas, contratos e
    finanças.
3.  Simular partidas com influência de força, mando de campo, tática e
    atributos dos jogadores.
4.  Processar turnos de temporada, calendário, classificações, promoção
    e rebaixamento.
5.  Disponibilizar modo individual e estruturar o modo multiplayer
    compartilhado.
6.  Incluir mercado de jogadores, leilões, salários, empréstimos, juros
    e patrocínios.
7.  Permitir editar parâmetros de ligas, divisões, clubes, jogadores e
    competições.
8.  Apresentar partidas e eventos com uma interface inspirada no Elifoot
    98, mas modernizada.

### 2.2 Limite da primeira versão multiplayer

Na primeira versão, todos os participantes de uma sessão escolhem clubes
de **um único campeonato/liga configurado**. Não haverá mistura livre de
clubes de campeonatos diferentes na mesma sessão.

O modo multiplayer deve ser planejado desde o início, mas suas regras de
sincronização, pausas, desconexões e autoridade do estado precisam ser
definidas antes de considerar a funcionalidade completa.

## 3. Perfis e criação de partida

### 3.1 Administrador

O administrador poderá:
- Criar ou editar a configuração da liga e das competições.
- Definir nomes, clubes, divisões, limites de força e regras de
  campeonato.
- Escolher se inicia na divisão inferior ou se seleciona um clube,
  conforme as regras da sessão.
- Criar uma sessão de rede e convidar participantes.

**Definido:**
- Somente o administrador pode criar jogo em rede.
- O administrador controla a velocidade das partidas da sessão em rede
  (§10.4).

### 3.2 Participante comum

Um participante sem privilégios de administrador:
- Só poderá iniciar ou entrar em uma configuração previamente
  disponível/carregada.
- Ao iniciar uma nova carreira individual, começará pela divisão mais
  baixa.
- Não poderá alterar regras globais da liga, salvo permissão explícita do
  administrador.
- **Definido:** pode jogar no modo individual.

**Em aberto:** definir se o administrador pode conceder permissões
adicionais e como os arquivos/configurações são compartilhados.

### 3.4 Contas, cadastro e convites

**Definido:**
- O primeiro administrador é criado por um script de administração. É um
  usuário com o papel (*role*) de administrador.
- O administrador envia um link de cadastro. Quem se cadastra por ele vira
  participante comum.
- Convites para sessões em rede podem ser enviados por e-mail.

**Em aberto:**
- Validade e uso único do link de cadastro e do convite.
- Recuperação de senha.
- Qual serviço de e-mail será usado. É uma dependência externa e precisa de
  credenciais fora do repositório.

### 3.5 Dados padrão, importação/exportação e salvamento

**Definido:**
- O jogo traz um **banco de dados padrão** com nomes gerados (sorteados)
  para clubes, jogadores, estádios e treinadores.
- O editor permite **exportar** as modificações feitas.
- O usuário pode **importar** um arquivo com suas modificações. A importação
  deve ser validada antes de uso (§7 do CLAUDE.md).

**Em aberto:**
- Formato do arquivo de importação/exportação. **Proposta:** JSON com
  versão de formato, para permitir migração futura.

**Definido — salvar na máquina pessoal:**
- Enquanto o jogo está em andamento, o estado fica no servidor, porque a
  simulação roda lá (`docs/decisions/0004`).
- O usuário pode **baixar um arquivo de save** para o computador e depois
  **apagar o jogo do servidor**.
- Para continuar, o usuário envia o arquivo de volta. O servidor
  **revalida** o arquivo antes de carregar, porque ele pode ter sido
  alterado.
- No multiplayer, o estado oficial permanece no servidor durante a sessão
  (§6 do CLAUDE.md).

**Em aberto:**
- Formato do arquivo de save. Proposta: o mesmo JSON versionado da
  importação/exportação.
- Se um save alterado manualmente é recusado ou aceito após validação.

### 3.3 Sala multiplayer

A sala deverá exibir, no mínimo: - Quantidade de participantes
conectados. - Nome do treinador de cada participante. - Clube escolhido
por cada participante. - Estado de cada participante na fase atual (por
exemplo, configurando, pronto ou desconectado).

## 4. Estrutura de ligas e divisões

### 4.1 Validação de clubes

Uma liga só poderá ser iniciada se houver clubes suficientes para
preencher as divisões e atender às regras de promoção e rebaixamento.

Exemplo ilustrativo: - Primeira divisão: 20 clubes. - Rebaixamento: 4
clubes. - A configuração exige 24 clubes disponíveis para que existam
clubes suficientes para preencher a primeira divisão e substituir os
rebaixados na temporada seguinte.

**Definido — fórmula:**

```
clubes necessários = Σ (clubes de cada divisão) + rebaixados da última divisão
```

- Exemplo com 1 divisão de 20 clubes e 4 rebaixados: 20 + 4 = 24.
- Exemplo com 2 divisões (20 e 18) e 4 rebaixados da 2ª: 20 + 18 + 4 = 42.
- As trocas entre divisões não exigem clubes extras, porque os rebaixados
  de uma divisão ocupam as vagas dos promovidos.

**Definido — fila de espera:**
- Os clubes rebaixados da última divisão ficam **sem competição de liga**
  e aguardam a temporada seguinte para subir.
- Se houver mais clubes aguardando do que vagas, os promovidos da fila são
  definidos por **sorteio**.

**Definido:**
- Cada divisão tem de **15 a 20 clubes**, configurável.
- O banco padrão inicial reproduz o **formato atual** das ligas e torneios
  reais:
  - quantidade de clubes;
  - promovidos e rebaixados;
  - estrutura das copas e competições continentais.
- Os **nomes** de clubes, jogadores e torneios são **aleatórios**.

**Pendência:** pesquisar e registrar esses formatos, com fonte e data, ao
montar o banco padrão. Não preencher de memória.

**Definido (rev. 11) — consistência exigida na validação:** estas regras
decorrem das regras acima.
- Os rebaixados de uma divisão são iguais aos promovidos da divisão
  imediatamente abaixo. Assim, as divisões mantêm o tamanho.
- A divisão principal não tem promovidos.
- Promovidos + rebaixados de uma divisão não passam do total de clubes
  dela.
- As prioridades são 1, 2, 3..., sem repetir nem pular.
- Um clube aparece uma única vez, em uma divisão ou na fila de espera.

**Definido (rev. 13) — fila de espera e fim de temporada:**
- O clube rebaixado da última divisão vai para a fila e fica **pelo menos
  uma temporada fora**. O sorteio para subir considera apenas os clubes
  que já estavam na fila antes do rebaixamento.
- **Clube de usuário rebaixado para a fila** (sem divisão para jogar): o
  usuário é **demitido na hora**.
  - **Conflito resolvido:** é uma exceção explícita à §5.4, que proíbe
    demissão automática de usuário.
  - **Em aberto:** o que acontece com o usuário demitido. Opções: escolher
    outro clube, assumir um clube que ofereça vaga, ou encerrar a
    carreira.
- **Clube da máquina rebaixado para a fila:** fica sem liga até ser
  sorteado para subir.
- **Rebaixado para outra divisão** (por exemplo, da 1ª para a 2ª): segue
  normalmente na nova divisão, seja de usuário ou da máquina.

**Em aberto:** se os clubes na fila de espera podem disputar copas
(possivelmente por sorteio) ou competições continentais.

### 4.2 Configuração de divisão

Cada divisão deve permitir configurar:
- Nome.
- Quantidade de clubes.
- Quantidade de clubes promovidos e rebaixados.
- Limite máximo de força dos clubes/jogadores, se aplicável.
- **Prioridade:** 1 = liga principal, 2 = segunda divisão e assim por
  diante (§8.3.3).
- Regras de competição relevantes.

**Definido — escala de força:** de **1 a 50** (mínimo 1, máximo 50). A
referência do Elifoot 98 confirma essa escala.

**Definido — limite de força por divisão:**
- **Primeira divisão:** os clubes usam a força cadastrada e os jogadores
  podem progredir até 50.
- **Segunda divisão:** limite de **30**.
  - Um clube cadastrado com força maior (por exemplo, 50) começa nela com
    no máximo 30.
  - Um clube rebaixado para a segunda divisão com jogadores acima de 30
    **nunca os faz subir acima de 30**. Os jogadores só podem cair ou ficar
    travados no valor atual.

**Definido — configuração dos limites:**
- A primeira liga criada (prioridade 1) tem limite **50** automaticamente.
- Nas divisões seguintes, o limite é escolhido pelo usuário. O valor
  sugerido para a 2ª divisão é 30.
- Qualquer clube promovido à primeira divisão pode voltar a evoluir até
  50, a partir da força atual dos jogadores.

**Em aberto:** como o limite da divisão interage com a âncora de ±5 do
"nível base do clube" do Elifoot 98 (`docs/referencia-elifoot98.md` §5).

### 4.3 Força do clube na criação

**Definido (simplificado):** a força do clube é obrigatória no cadastro. A
hierarquia vem da força cadastrada de cada clube, limitada pelo teto da
divisão (§4.2). O jogo **não** redistribui forças automaticamente.

A geração do elenco deve respeitar a força cadastrada:
- A força média do elenco deve corresponder aproximadamente à força
  configurada para o clube.
- Dois clubes com a mesma força não precisam ter elencos idênticos. Pode
  haver pequenas diferenças, mantendo o nível geral.
- A geração deve evitar diferenças acidentais grandes entre equipes que
  deveriam ser equivalentes.

**Em aberto:** definir se a força do clube é média simples, média
ponderada pelos titulares ou outro indicador.

## 5. Clubes, jogadores e treinadores

### 5.1 Clube

O editor de clubes deve permitir configurar: - Nome do clube. - Cores,
com paleta ampliada. - Divisão e força inicial. - Nome do estádio e
capacidade inicial. - Outros parâmetros necessários à liga.

### 5.2 Jogadores

O editor de jogadores deve permitir configurar: - Nome. - Posição. -
Nacionalidade. - Força. - Presença ou ausência do atributo especial
representado por asterisco. - Comportamento/temperamento.

Nesta versão, **não haverá idade dos jogadores**. O jogo manterá
rankings de artilheiros e o histórico de cartões de cada jogador (§7.4).

**Definido — elenco gerado:**
- Cada clube tem **25 jogadores** cadastrados.
- Nomes e nacionalidades são gerados por padrão, por sorteio.
- Cada clube tem **de 5 a 7 estrangeiros**, para não distorcer o limite de
  estrangeiros (§8.4).

**Em aberto:**
- Distribuição de posições entre os 25 jogadores (quantos goleiros,
  defensores, meias e atacantes).
- Se 25 é o máximo do elenco ou apenas o tamanho gerado (o mercado pode
  aumentar ou reduzir o elenco).

O temperamento pode assumir:
- Calmo.
- Educado.
- Normal.
- Ansioso.
- Violento.

**Provisório:** a associação de temperamento ao jogador pode ser aleatória
ou derivada de um cálculo sobre o nome.

### 5.3 Jogadores com asterisco

O asterisco indica um jogador especial, mais caro e potencialmente mais
influente na finalização: - Jogadores com asterisco têm custo maior. -
Durante a partida, um jogador escalado com asterisco pode ter maior
probabilidade de ser selecionado como finalizador e uma pequena vantagem
de conclusão. - A escalação pode ter até três jogadores com asterisco
sem penalidade. - Escalar mais de três deve aplicar uma penalização
ainda a definir. - O limite se aplica aos jogadores em campo, não ao
elenco completo ou aos reservas. - Jogadores com asterisco normalmente
não serão vendidos por leilão controlado pela máquina; alguns poderão
aparecer com preço fixo para compra direta.

A influência exata do asterisco precisa ser testada para que não se
torne uma vantagem desproporcional.

### 5.4 Treinadores e demissões

O sistema deve incluir: - Um treinador para cada clube. - Treinadores
controlados por usuários. - Treinadores livres, capazes de assumir
clubes menores. - Trocas de treinador inspiradas no Elifoot 98, com
frequência controlada para não ocorrerem excessivamente.

Regras propostas: - A moral do treinador/equipe influencia a
possibilidade de demissão. - Treinadores controlados pela máquina podem
ser substituídos automaticamente quando as condições forem atendidas. -
Um usuário não deve perder o controle do próprio clube por demissão
automática; a política para troca voluntária deve ser definida. - Deve
existir ao menos uma opção de treinador livre no início da carreira. -
Clubes de força até 10 podem, pela proposta inicial, trocar de treinador
sem exigir que outro clube tenha um treinador disponível de força
inferior. - Para clubes mais fortes, a substituição deve depender da
disponibilidade de um treinador considerado adequado e de regras de
moral.

**Em aberto:** formalizar a comparação de força, moral e
disponibilidade. As regras atuais são exemplos de intenção, não um
algoritmo completo.

## 6. Progressão de força e moral

### 6.1 Evolução dos jogadores

A progressão deve se inspirar no comportamento observado no Elifoot
98: - Jogadores reservas podem evoluir após vitórias. - Empates podem
gerar evolução em algumas situações, ou não gerar alteração. - Derrotas
não devem favorecer a evolução normal. - Jogadores que participam da
partida podem ter um comportamento de evolução diferente dos reservas. -
A carga de partidas e os resultados podem influenciar perda de força,
mas o algoritmo ainda precisa ser definido.

Proposta a testar:
- **Reservas:** maior chance de evolução após vitórias.
- **Titulares:** considerar desgaste, sequência de jogos e resultado.
  - Vitória: o desgaste demora mais a aparecer.
  - Empate: desgaste leve.
  - Derrota: desgaste.
- **Empate e derrota:** regras próprias, ajustadas por simulação.

**Confirmado na referência** (`docs/referencia-elifoot98.md` §5):
- No Elifoot 98, a perda é de **1 ponto**, com **50% de chance**, para
  jogadores com avaliação negativa.
- A força não cai abaixo de "nível base do clube" − 5 nem sobe acima de
  "nível base" + 5.
- Goleiros precisam de um sorteio extra de 50% para evoluir.
- A referência **não documenta** quais jogadores recebem avaliação positiva
  ou negativa.

**Em aberto:** definir se e quando titulares perdem força, quais
jogadores evoluem e quais são os limites por temporada.

### 6.2 Moral do time

A moral será numérica, em vez de ser representada apenas por uma barra.
A escala e os efeitos ainda precisam ser formalizados.

**Provisório — escala de 0 a 30:**

| Resultado | Em casa | Fora |
|---|---|---|
| Vitória | +2 | +3 |
| Empate | −1 | 0 |
| Derrota | −3 | −2 |

Ao assumir um time, a moral começa em **18**. Ela fica limitada entre 0 e
30.

Esses valores são uma hipótese inicial a validar por simulação. A moral deverá
influenciar: - Risco de demissão do treinador. - Público no estádio,
conforme a intenção original. - Possivelmente outros efeitos, se forem
aprovados.

Proposta de faixas ilustrativas: - Verde: 15 a 30. - Amarelo: 6 a 14. -
Vermelho: 0 a 5.

A implementação deve definir limites mínimos/máximos e o efeito de
vitórias, sequência de resultados e diferença de força.

**Simulação (rev. 14, `docs/simulacoes/temporada.md`):** com os valores
provisórios, a moral **satura nos extremos** ao longo de 38 rodadas.
- Os clubes de cima terminam perto de 30.
- Cerca de metade da liga termina na faixa vermelha (0 a 5).
- O motivo: a moral funciona como um passeio aleatório sem nada que a puxe
  de volta para o meio, e a escala é curta para 38 jogos.

Se a moral for usada para demitir treinadores, isso causaria demissões
demais.

**Em aberto:** a correção. Opções:
- reduzir as variações;
- puxar a moral de volta para o valor inicial a cada rodada;
- considerar a diferença de força (perder para um clube muito mais forte
  pesar menos).

## 7. Motor de partidas (minuto a minuto, estilo elifoot98)

### 7.1 Princípios

O motor deve aproveitar como referência as mecânicas do Elifoot 98. O que
foi confirmado no repositório está em `docs/referencia-elifoot98.md` §2–§4.

Deve acrescentar uma influência explícita dos meio-campistas e das táticas,
em vez de depender apenas de uma distinção simplificada entre defesa e
ataque. No Elifoot 98, o meio-campo apenas divide a força 50/50 e a
formação 5-0-5 domina.

**Definido — papel de cada setor (substitui a divisão 50/50 do Elifoot):**
- **Meio contra meio** decide qual time **domina**, ou seja, qual cria mais
  chances.
- **Ataque contra defesa** e o goleiro decidem se a chance vira gol.
- Formações com **mais meias dominam mais, mas finalizam menos**.

Os coeficientes são **provisórios** e serão calibrados pelas metas da
§7.6.

**Esclarecimento:** "simulação em lote" (§13, §16.5) é uma **ferramenta de
desenvolvimento e teste**. Ela simula milhares de partidas para medir
placares médios, vantagem de mando etc. Não faz parte do jogo. No jogo, as
partidas são sempre por turnos, exibidas na mesma tela (§10.2).

Objetivos de balanceamento: - Partidas entre clubes equivalentes devem
ter placares plausíveis e variados. - O mando de campo deve fornecer
vantagem, sem garantir vitória. - Clubes fracos devem ter uma chance
pequena, mas real, de vencer clubes fortes. - Táticas ofensivas devem
aumentar o potencial ofensivo e também expor mais a defesa. - Táticas
defensivas devem reduzir a probabilidade de sofrer gols, normalmente à
custa de menor produção ofensiva. - Cada gol deve, em geral, reduzir a
chance de ocorrerem muitos gols adicionais, evitando placares
exagerados. - Placares extremos devem ser possíveis, mas raros.

Exemplos de placares considerados comuns para equipes equilibradas:
1--0, 2--0, 2--1, 3--0 e 3--1, tanto com vitória do mandante quanto do
visitante em frequência coerente com a vantagem de campo. Resultados
como 4--0, 4--1, 4--2 e jogos de muitos gols devem acontecer
ocasionalmente, conforme força e tática. Um 10--10 não deve ser um
resultado normal.

Esses exemplos são referências qualitativas, não quotas rígidas de
resultados.

### 7.2 Táticas

As táticas são obrigatórias: o treinador deve selecionar uma formação
válida antes da partida. Cada opção deve mostrar um indicador de estilo
e, se aplicável, seu custo ou efeito de manutenção, cuja definição
permanece em aberto.

Classificação inicial proposta:

  Estilo              Formações
  ------------------- ---------------------
  Normal              4-4-2, 3-5-2
  Avançado/ofensivo   4-3-3, 3-4-3, 4-2-4
  Defensivo           5-4-1, 4-5-1, 3-6-1

A classificação e os nomes podem ser ajustados após avaliar a mecânica.

**Definido:**
- A escalação deve **respeitar a formação** escolhida: 1 goleiro e o número
  exato de defensores, meias e atacantes.
- Não há jogador fora de posição, nem formações fora da lista (como a
  5-0-5).

A formação deve influenciar: - Equilíbrio entre defesa, meio-campo e
ataque. - Probabilidade de seleção de um atacante ou meio-campista como
finalizador. - Chance de criar oportunidades e de sofrer gols. -
Consequências de ficar com um jogador a menos.

Exemplos iniciais de pesos para seleção do finalizador:

**Definido (rev. 6) — pesos iniciais**, configuráveis e sujeitos a ajuste
por simulação:

| Formação | Atacante | Meio-campista | Defesa | Goleiro |
|---|---|---|---|---|
| 4-4-2 | 64% | 31% | 4,99% | 0,01% |
| 4-3-3 | 75% | 21% | 3,99% | 0,01% |
| 3-5-2 | 58% | 38% | 3,99% | 0,01% |
| 3-4-3 | 74% | 23% | 2,99% | 0,01% |
| 4-2-4 | 82% | 14% | 3,99% | 0,01% |
| 5-4-1 | 45% | 47% | 7,99% | 0,01% |
| 4-5-1 | 43% | 51% | 5,99% | 0,01% |
| 3-6-1 | 40% | 55% | 4,99% | 0,01% |

**Definido — escolha do finalizador:** depois de sorteado o setor, o
jogador do setor é escolhido com **peso pela força**. Jogadores mais
fortes finalizam mais vezes.

Esses pesos são hipóteses e não devem ser fixados definitivamente sem
simulação. As probabilidades precisam variar coerentemente com a
formação e ser configuráveis.

**Definido:** o agente propõe os pesos das demais formações seguindo a
mesma lógica (mais atacantes, mais chance do atacante; mais meias, mais
participação do meio). Os valores são validados na simulação em lote e
registrados aqui depois.

### 7.3 Expulsões

-   A expulsão de um jogador deve reduzir a capacidade de criação
    ofensiva da equipe punida.
-   A equipe adversária deve ter uma chance maior de marcar.
-   O efeito deve considerar o tempo restante e, se aplicável, a posição
    do jogador expulso.
-   O efeito não deve tornar o resultado inevitável.

**Definido (rev. 7):**
- **Efeito da expulsão:** o jogador expulso **sai de campo** e o setor dele
  perde a sua força. Não há penalidade extra.
  - Se for meia, o time domina menos.
  - Se for atacante, o time finaliza pior.
  - Se for defensor, o time defende pior.
  - Quanto mais cedo a expulsão, mais tempo o time joga com um a menos.
- **Goleiro expulso** (enquanto não há substituições): o **defensor de
  menor força** vai para o gol.
  - Se não houver defensor, vai o jogador de linha de menor força.
  - **Provisório:** como goleiro, ele atua com **metade da força**.
- **Caixa de substituição na expulsão (rev. 8, como no Elifoot 98):** quando
  um jogador de um time controlado por usuário é expulso, abre-se a caixa
  com o time e as opções de substituição.
  - **Goleiro expulso:** o defensor de menor força vai automaticamente para
    o gol. Se ainda houver substituições disponíveis, o treinador pode
    trocar (por exemplo, colocar o goleiro reserva). Se não houver, ele
    confirma e o time segue assim.
  - **Jogador de linha expulso:** fica o buraco na tática. O treinador pode
    usar a caixa para outras substituições permitidas.
  - Times controlados pela máquina aplicam a regra automática.
  - **Em aberto:** em rede, se a abertura dessa caixa pausa a partida para
    todos (depende da §11.3).

### 7.4 Temperamento e cartões

Proposta de modificadores relativos para risco disciplinar: - Calmo:
−2. - Educado: −1. - Normal: 0. - Ansioso: +1. - Violento: +2.

Esses números são pontos de um modificador ainda não definido, não
probabilidades percentuais. O motor deverá converter o modificador em um
efeito pequeno e mensurável. O risco de expulsão pode aumentar
ligeiramente com mais jogadores de temperamento agressivo em campo.

**Referência:** no Elifoot 98, o infrator é escolhido por roleta ponderada
com pesos 1/2/4/7/10 (`docs/referencia-elifoot98.md` §4).

**Definido (substitui a escala −2…+2):**
- Pesos de roleta: **Calmo 1, Educado 2, Normal 4, Ansioso 6, Violento 8**.
  São menores que os do Elifoot 98 (1/2/4/7/10).
- Os pesos decidem **quem** recebe o cartão.
- A soma dos pesos dos titulares aumenta levemente a **frequência** de
  faltas do time.
- **Provisório:** quanto a soma aumenta a frequência. Os pesos são
  configuráveis e o efeito será validado por simulação.

**Definido — cartões amarelos:**
- Existem cartões amarelos.
- **Dois amarelos para o mesmo jogador na mesma partida resultam em
  expulsão.**
- Os cartões ficam registrados no histórico do jogador.

**Definido — suspensões (regra simples, sem diferenciar competições):**
- **5 amarelos acumulados na temporada** resultam em **1 jogo de
  suspensão**.
  - O acúmulo soma todas as competições.
  - O contador zera na virada da temporada.
- **Expulsão** resulta em **1 a 3 jogos** de suspensão.

**Definido:**
- **Duração da suspensão por expulsão:**
  - Vermelho direto: **sorteio ponderado** entre 1 e 3 jogos, com 1 jogo
    mais comum e 3 jogos raro.
  - Dois amarelos na mesma partida: sempre **1 jogo**.
- A suspensão é cumprida na **próxima partida do clube**, seja liga, copa
  ou competição continental.
- Depois da suspensão por 5 amarelos, o contador **volta a zero**.

**Provisório:** os pesos do sorteio de 1 a 3 jogos, configuráveis e
validados por simulação.

### 7.4.1 Substituições

**Definido:** o número de substituições é configurável **por competição**
(liga, copa, continental e Mundial). O valor padrão é **5**.

**Definido (rev. 10):**
- **Banco:** até **7 reservas** por partida, igual em todas as competições.
- **Mudança de tática:** a substituição muda a formação conforme a posição
  de quem sai e de quem entra.
  - Exemplo: no 4-4-2, sai um zagueiro e entra um atacante; a formação
    vira 3-4-3.
  - Uma troca que resultaria em formação fora das 8 da §7.2 é recusada,
    com mensagem.
- **Regras do futebol:**
  - Quem saiu não volta e não aparece mais na lista de substituições.
  - O expulso não é substituído. Fica o buraco, e a expulsão fica
    registrada nos eventos da partida.
  - Só podem sair jogadores que ainda estão em campo.
- **Goleiro reserva:** só entra no gol. Se o goleiro titular foi expulso e
  um defensor está improvisado, o goleiro reserva entra no lugar de um
  jogador de linha e o defensor volta para a defesa.
- **Máquina:** só substitui em caso de lesão ou de goleiro expulso. Não
  avalia o desempenho dos jogadores.
  - **Provisório:** com goleiro expulso, o goleiro reserva entra no lugar
    do atacante mais fraco; sem atacantes, sai o meia mais fraco.
  - Treinadores mais inteligentes ficam para depois.
- **Pendência:** a substituição por lesão entra junto com as lesões, que
  continuam **Em aberto** (§7.5).

### 7.5 Lesões

Objetivos: - Lesões não devem ser excessivamente frequentes. -
Referência inicial: em média, de uma a duas lesões por clube por
temporada. - Duração da lesão e perda de força devem ter relação
coerente: lesões mais graves podem afastar o jogador por mais tempo e
gerar maior perda. - **Definido:** o temperamento influencia a chance de
lesão. Calmos e educados têm chance **maior** que a de um jogador normal.
Ansiosos e violentos têm chance **menor**. - A
chance pode aumentar após lesões acumuladas no clube durante a
temporada, mas deve haver limite de cinco lesões por clube por
temporada, se essa regra for mantida. - O aumento sazonal de risco deve
ser reiniciado ao fim da temporada.

**Em aberto:** definir o que conta como lesão por temporada (evento ou
jogador lesionado), gravidade, duração, recuperação e se o limite de
cinco é absoluto.

### 7.6 Metas de calibração do motor

São alvos para **times equilibrados** na simulação em lote. Medem médias,
não impõem resultados (§7.1).

| Métrica | Alvo | Status |
|---|---|---|
| Gols por partida (média) | 1,75 a 2,5 | **Definido** |
| Vitória do mandante | cerca de 45% | **Definido** |
| Empate / vitória do visitante | cerca de 27% / 28% | **Provisório** (derivado) |
| Cartões amarelos por partida | cerca de 2 | **Definido** |
| Expulsões por partida | 0,1 a 0,2 | **Definido** |
| Pênaltis por partida | cerca de 0,3 | **Definido**; substitui o 1%/minuto do Elifoot, que daria cerca de 0,9 |

**Definido (rev. 8) — frequência de zebras**, com o time mais fraco jogando
em casa e formações iguais:

| Diferença de força média | O fraco vence |
|---|---|
| 10 pontos (ex.: 20 × 30) | 15% a 20% |
| 20 pontos (ex.: 20 × 40) | 5% a 10% |
| 35 pontos (ex.: 10 × 45) | 1% a 3% |

Por maior que seja a diferença, o fraco **nunca** deve ter 0%. O piso é de
cerca de **1%**.

**Definido (rev. 9):** o piso de 1% de zebra vale para diferenças de força
média de **até 35 pontos**. Diferenças maiores não têm meta. O caso 1 × 50
ficou em cerca de 0,1%, mas não é exigido.

**Resultado da calibração (Etapa 1d):** ver
`docs/simulacoes/calibracao-motor.md`. Todas as metas desta seção foram
atingidas.
- Gols: 2,18 por jogo.
- Mando (casa / empate / fora): 45,1% / 29,4% / 25,6%.
- Amarelos, expulsões e pênaltis: 2,04 / 0,20 / 0,29 por jogo.
- Zebras: 17,6% / 9,2% / 1,5%.

**Equilíbrio entre formações (rev. 9):**
- Cada setor tem um expoente de quantidade. A defesa vale mais a cada
  zagueiro, e meias e atacantes têm retorno decrescente.
- O goleiro conta só no duelo com o finalizador.
- No confronto de todas contra todas, os pontos por jogo variam de 1,27
  (3-6-1) a 1,42 (4-4-2).
- A 4-4-2 tem leve vantagem sobre todas as outras, por margens pequenas.
- Ofensivas fazem e sofrem mais gols; defensivas, menos.
- **Provisório:** reavaliar se essa leve vantagem da 4-4-2 for percebida
  em jogo.

**Temperamento (rev. 9):** um time todo violento recebe cerca de **1,7 vez**
os cartões de um time todo calmo (antes, 2,8 vezes).

## 8. Calendário, temporadas e competições

### 8.1 Ligas

-   **Definido:** o calendário é obrigatoriamente em **turno e returno**,
    contemplando todos os clubes da divisão.
-   **Definido (rev. 11):**
    -   O returno **repete a ordem do turno com o mando invertido**, como no
        Brasileirão. Isso vale para qualquer liga.
    -   Cada par de clubes joga uma vez em cada casa. Com N clubes, cada
        clube faz N−1 jogos em casa e N−1 fora (20 clubes: 19 e 19; 18
        clubes: 17 e 17).
    -   O mando alterna rodada a rodada sempre que possível.
        **Nota técnica:** a alternância perfeita para todos é
        matematicamente impossível em pontos corridos. Usa-se o método
        clássico de rodízio (tabela de Berger), que minimiza as sequências
        de dois jogos seguidos no mesmo mando.
    -   Com número ímpar de clubes (15, 17 ou 19), um clube folga a cada
        rodada.
    -   **Definido (rev. 12):** a ordem dos confrontos é **sorteada a cada
        temporada**, como no Brasileirão. O sorteio usa semente para poder
        ser reproduzido.
-   **Definido (rev. 12) — sorteio do desempate:** o último critério
    (sorteio) é **fixo durante a temporada**. Cada clube recebe um número
    sorteado a partir da semente da temporada. Assim, clubes empatados não
    trocam de posição a cada vez que a tabela é recalculada.
-   **Nota (rev. 12):** como pontos = 3 × vitórias + empates, dois
    critérios nunca chegam a decidir:
    -   "menos empates": com mesmos pontos e vitórias, os empates são
        iguais;
    -   "menos gols sofridos": com mesmo saldo e mesmos gols marcados, os
        gols sofridos são iguais.

    Ambos continuam na regra, sem efeito prático. "Menos derrotas" só
    decide quando o número de jogos difere, por exemplo por folga.
-   **Definido:** vitória vale **3 pontos**, empate **1** e derrota **0**.
-   **Definido — desempate na classificação**, nesta ordem:
    1.  Pontos.
    2.  Mais vitórias.
    3.  Menos derrotas.
    4.  Menos empates.
    5.  Melhor saldo de gols.
    6.  Mais gols marcados.
    7.  Menos gols sofridos.
    8.  Sorteio.
-   **Definido:** os critérios valem para **todas as posições** da tabela,
    inclusive promoção, rebaixamento e vagas continentais. Em empate, fica
    à frente quem tiver vantagem no primeiro critério que os diferenciar.
-   A classificação deve atualizar após as partidas.
-   Promoções e rebaixamentos devem ocorrer ao final da temporada,
    conforme configuração da divisão.
-   O avanço para a temporada seguinte só pode ocorrer depois que todas
    as partidas e competições obrigatórias forem processadas.

**Premiações:**
-   **Definido:**
    -   Quem paga as premiações é a competição (liga, copa ou
        continental).
    -   Os prêmios por colocação (1º, 2º, 3º...) são configuráveis em cada
        campeonato.
    -   Ao final da temporada também há premiação para o artilheiro, para a
        copa e para as competições continentais.
-   **Em aberto:** os valores. A referência do Elifoot 98 não documenta
    finanças (`docs/referencia-elifoot98.md` §6). Será preciso criar tabelas
    próprias que relacionem força, contrato, receita, premiação e
    patrocínio, validadas por simulação (§9).


### 8.2 Copas nacionais

A copa deve ser configurável, inclusive com alteração do nome. - Formato
eliminatório. - Pode começar com 16 equipes (oitavas de final) ou 32
equipes (fase equivalente a 16 avos), conforme o total e as regras
configuradas. - Uma proposta é selecionar os oito melhores da temporada
anterior ou por força, completando as vagas por sorteio. - A regra de
entrada das equipes deve ser formalizada para cada formato; não assumir
automaticamente que todas as equipes da primeira divisão entram em todos
os formatos. - No início da temporada, o usuário poderá escolher se
deseja acompanhar o sorteio passo a passo ou executá-lo rapidamente.

**Definido — formato dos confrontos (vale para copa, competições
continentais e Mundial):**
- Os confrontos eliminatórios são em **ida e volta**.
- A **final** é em **partida única**.
- Na final única, a **bilheteria é compartilhada** entre os dois clubes.
- **Desempate:** prorrogação e, persistindo o empate, pênaltis.
- **Número de equipes que não é potência de 2:** a equipe mais fraca fica
  de fora. Se houver mais de uma com a mesma força, a excluída é
  sorteada.

**Em aberto:**
- No ida e volta, se existe critério de gol fora de casa antes da
  prorrogação. (Não foi mencionado; hipótese: não existe.)
- Onde é jogada a final única: campo neutro, clube melhor classificado ou
  sorteio.
- A proporção da bilheteria compartilhada (hipótese: 50/50).
- Para excluir equipes, se "mais fraca" significa menor força cadastrada,
  menor força atual ou pior classificação.

Quando não houver participantes controlados por usuários em uma partida
da copa, poderá ser usado um processamento rápido, sem simulação visual
minuto a minuto.

### 8.3 Competições continentais e Mundial de Clubes

#### 8.3.1 Competições continentais

O jogo deverá permitir a criação de competições continentais para as seguintes regiões:

- **América do Sul:** competição equivalente à Copa Libertadores.
- **Europa:** competição equivalente à Liga dos Campeões da UEFA.
- **América do Norte, América Central e Caribe:** competição continental própria.
- **Ásia:** competição continental própria.

Os nomes das competições deverão ser personalizáveis pelo editor, sem depender dos nomes reais das competições utilizadas como referência.

#### 8.3.2 Distribuição de vagas por país

Cada competição continental deverá permitir configurar a quantidade de vagas destinada a cada país participante.

Exemplo de configuração para a competição sul-americana:

- Brasil: 7 vagas.
- Argentina: 5 vagas.
- Uruguai: 3 vagas.
- Outros países: quantidades definidas individualmente pelo editor.

A distribuição deverá respeitar as quantidades configuradas para cada país, considerando a disponibilidade de clubes elegíveis. O sistema seleciona clubes até completar o número de vagas de cada país, dentro dos limites das regras abaixo.

**Definido:** os valores padrão de vagas por país devem seguir a
distribuição **atual** das competições reais equivalentes. **Pendência:**
pesquisar e registrar esses números com a fonte e a data ao montar o banco
padrão. Não preencher de memória.

A classificação dos clubes deverá seguir estas regras:

1. **Países com vagas configuradas:** o sistema deverá buscar clubes elegíveis nos países que possuem vagas definidas, respeitando a quantidade de vagas de cada país.
2. **Seleção de clubes:** quando não houver uma classificação nacional previamente definida ou outro critério de classificação configurado, os clubes poderão ser selecionados aleatoriamente, dando preferência aos clubes de maior força. A seleção deverá considerar a força dos clubes como critério de prioridade, sem necessariamente tornar a escolha totalmente determinística.
3. **Países sem clubes elegíveis:** se um país possuir vagas configuradas, mas não tiver nenhum clube cadastrado e elegível, suas vagas não serão preenchidas por clubes de outros países. Essas vagas ficarão sem utilização naquela edição.
4. **Países com menos clubes do que vagas:** se um país tiver menos clubes elegíveis do que o número de vagas configuradas, somente os clubes disponíveis poderão participar. As vagas restantes não serão transferidas automaticamente para outros países.
5. **Ausência de participantes:** caso não existam clubes elegíveis suficientes para viabilizar a competição conforme suas regras mínimas, a edição não deverá ser realizada.

A força dos clubes deverá influenciar a seleção dos representantes dentro de cada país, sem alterar a quantidade de vagas atribuída a cada país.

#### 8.3.3 Relação com as ligas nacionais

Sempre que houver uma liga nacional ativa e uma classificação nacional disponível, o sistema deverá permitir que as vagas continentais sejam distribuídas com base no desempenho dos clubes nessa liga, respeitando a quantidade de vagas configurada para o respectivo país.

A classificação nacional deverá ter prioridade sobre a seleção aleatória por força, quando esse critério estiver habilitado.

Caso não exista uma classificação nacional disponível ou o editor opte por não utilizá-la, o sistema poderá selecionar os representantes entre os clubes elegíveis do país, utilizando a força dos clubes como critério de preferência e um componente aleatório.

O sistema deverá impedir que um mesmo clube ocupe mais de uma vaga na mesma competição continental. As regras para tratar vagas decorrentes de títulos continentais anteriores, duplicidade de classificação ou outras exceções deverão ser configuráveis ou definidas posteriormente.

A configuração da liga não precisa selecionar clubes, pois a força de cada um já estará definida. Ela deve, porém, indicar a **prioridade**: 1 é a liga principal, 2 é a segunda divisão e assim por diante.

**Definido — clubes de outros países:**
- Os clubes estrangeiros existem no banco de dados (com força e país), mas
  **suas ligas não são exibidas nem jogadas**.
- Para que uma competição continental funcione, é preciso ter clubes
  cadastrados dos outros países da região.
  - Exemplo: jogando no Brasil, é preciso ter clubes da Argentina, Bolívia,
    Colômbia etc. para a competição sul-americana.
  - Exemplo: jogando na Inglaterra, é preciso ter clubes da Espanha,
    Itália, França, Alemanha etc. para a competição europeia.

#### 8.3.4 Mundial de Clubes

O jogo deverá possuir um Mundial de Clubes com participação de representantes das competições continentais.

O formato inicial deverá ser inspirado em um torneio de confrontos entre representantes de diferentes regiões, incluindo Europa, Ásia, América do Norte/Central e Caribe e América do Sul, seguido de uma final.

O formato exato, o número de participantes, os critérios de classificação e a sequência dos confrontos deverão ser configuráveis ou definidos em uma etapa posterior.

**Definido:** para haver Mundial, é preciso ter pelo menos um clube cadastrado de cada região: Europa, Ásia, América do Norte/Central e Caribe e América do Sul. Exemplo: jogando na Inglaterra, é preciso ter pelo menos um clube sul-americano.

**Em aberto:**
- Se o representante de uma região é o campeão da competição continental
  dela, ou um clube escolhido por força quando a competição dessa região não
  é jogada.
- As regiões fora da liga do usuário têm competição continental própria
  simulada em segundo plano?

A participação de cada região dependerá da existência de pelo menos um clube elegível para representá-la. Regiões sem clubes elegíveis não deverão gerar participantes fictícios.

Se a ausência de representantes impedir a realização do formato previsto, o sistema deverá aplicar uma regra de elegibilidade e viabilidade previamente definida, podendo cancelar a edição caso não haja participantes suficientes. O sistema não deverá inventar clubes ou preencher vagas com clubes de regiões não previstas apenas para completar a competição.

#### 8.3.5 Regras gerais

- O editor deverá poder personalizar os nomes das competições.
- A quantidade de vagas por país deverá ser configurável individualmente em cada competição continental.

### 8.4 Regras de estrangeiros

Cada competição ou país deve permitir configurar o limite de jogadores
estrangeiros que podem atuar em uma partida. O número será definido pelo
administrador, sem ficar fixo no código.

**Definido — quem conta como estrangeiro:**
- A regra é configurável por país, com listas de nacionalidades
  equiparadas a nacionais.
- **Brasil:** jogadores de outros países são estrangeiros, **exceto
  portugueses**.
- **Países da União Europeia** (exemplo: Itália): jogadores de países da UE
  **não** são estrangeiros.

**Em aberto:** a lista padrão de países da UE e se outros casos entram no
padrão inicial (por exemplo, países fora da UE com acordos semelhantes).
Pesquisar e registrar com fonte ao montar o banco padrão.

**Definido:** **não existe bônus de nacionalidade** na força do time. O
Elifoot 98 dava +3 por titular do país do clube
(`docs/referencia-elifoot98.md` §2). A regra foi retirada porque os elencos
atuais são multinacionais.

## 9. Economia e mercado

### 9.1 Finanças do clube

A economia deve se inspirar no Elifoot 98: - Processar receitas e
despesas a cada rodada/turno conforme as regras financeiras definidas. -
Jogos fora de casa geram despesas de deslocamento ou custos
correspondentes. - Jogos em casa geram receita de bilheteria,
descontados os custos aplicáveis. - Considerar salários, contratos,
empréstimos e juros. - Permitir que o clube tenha dificuldades
financeiras e precise vender jogadores.

A fórmula exata das receitas, despesas e juros deve ser investigada no
código de referência e documentada antes de implementar o sistema final.

**Resultado da investigação (rev. 2):** a referência não documenta
finanças, mercado, leilão nem salários (`docs/referencia-elifoot98.md` §6).
O que ela confirma:
- O contrato é um **contador de jogos restantes**.
- Salário e contrato **não afetam** o rendimento em campo.

**Em aberto:** criar tabelas próprias de força × preço × salário ×
receita × premiação × patrocínio, validadas por simulação de temporadas.

### 9.1.1 Moedas

**Definido:**
- O jogo terá três moedas: **Real**, **Dólar** e **Euro**.
- As moedas seguem uma proporção fixa, configurável: **1 Dólar = 5 Reais**
  e **1 Euro = 6 Reais**.

**Em aberto:**
- Qual moeda cada clube ou liga usa: pelo país, escolhida pelo usuário ou
  outra regra.
- Qual moeda é a base interna dos cálculos. Hipótese técnica: guardar
  valores numa unidade base inteira e converter só na exibição.

### 9.2 Estádios

-   Cada clube tem nome de estádio e capacidade.
-   A capacidade pode ser ampliada mediante custo.
-   Referência inicial: capacidade de 5.000 para clubes pequenos e
    limite configurável de até 120.000.
-   Clubes grandes tendem a começar com estádios maiores.
-   A moral do time deve influenciar o público presente, conforme a
    proposta.
-   O efeito do rebaixamento sobre a capacidade do estádio precisa ser
    decidido; não reduzir automaticamente sem regra aprovada.
-   A relação entre força, capacidade, público, receita e orçamento deve
    ser simulada para evitar que a economia entre em ciclos impossíveis.

### 9.3 Insolvência e elenco mínimo

Quando o clube ficar sem dinheiro, o sistema poderá vender
automaticamente jogadores sem contrato renovado, seguindo uma regra
transparente.

Proposta de elenco mínimo para bloquear vendas automáticas: - 1
goleiro. - 4 zagueiros. - 4 meio-campistas. - 2 atacantes.

Essa regra deve evitar que o clube fique sem jogadores suficientes para
formar uma equipe. Definir como tratar jogadores lesionados, suspensos e
posições secundárias.

Quando o clube estiver no elenco mínimo e um jogador estiver suspenso ou lesionado, poderá escalar com um jogador a menos. Nesse caso, a partida é simulada como se o time já começasse com um expulso.

### 9.4 Mercado e leilões

-   A máquina compra jogadores por meio de leilões.
-   Clubes com finanças ruins podem vender jogadores para gerar receita.
-   Clubes mais fracos podem tentar reforçar o elenco, respeitando
    orçamento e salários.
-   Jogadores com asterisco geralmente não são vendidos pela máquina via
    leilão; alguns podem ter preço fixo para compra direta.
-   Jogadores estrangeiros têm preço menor, conforme a regra proposta,
    mas o desconto precisa ser balanceado.
-   Um usuário pode vender jogadores do seu elenco apenas por leilão;
    não deve existir venda direta ao clicar no próprio plantel.
-   Um usuário poderá comprar jogadores por leilão ou por compra direta
    a preço definido, quando essa opção existir.

### 9.5 Contratos, salários e empréstimos

-   Jogadores possuem contratos com prazo e salário.
-   O jogo deve emitir notificações visuais quando o contrato estiver
    próximo do vencimento.
-   Jogadores sem contrato renovado podem solicitar aumento conforme sua
    força.
-   Se o clube não aceitar a negociação, o jogador poderá ser colocado
    em leilão, de acordo com regras a definir.
-   Empréstimos e juros devem existir.
-   Definir limites, prazos, taxas e consequências de inadimplência
    antes da implementação final.

### 9.6 Patrocínios

-   Os clubes recebem propostas de patrocínio de acordo com o desempenho
    e a situação na temporada.
-   Quando um contrato termina, novas propostas podem aparecer.
-   O prazo dos contratos pode variar, com exemplos de dois, três ou
    quatro anos; contratos de até quatro anos seriam reservados, em
    princípio, a clubes campeões.
-   O editor deve permitir configurar o nome do patrocinador e os
    parâmetros financeiros.
-   Não é necessário usar imagens de marcas.
-   Os valores devem complementar o orçamento, sem eliminar a
    importância da bilheteria, do mercado e da gestão salarial.
-   Pode existir uma "força do patrocinador", usada pelo jogo para
    calcular o valor da proposta.

## 10. Interface e apresentação

### 10.1 Identidade visual

-   Usar o Elifoot 98 como referência de simplicidade e organização.
-   Modernizar tipografia, espaçamento, cores e feedback sem
    descaracterizar o formato clássico.
-   Permitir um fundo padrão em cor clara/esverdeada e, opcionalmente,
    uma imagem JPEG de fundo.
-   Dimensões, proporção, tamanho máximo e comportamento de recorte da
    imagem de fundo estão **em aberto**.

### 10.2 Tela da partida

Manter a ideia de dois times representados visualmente, com melhorias
para dar destaque a: - Nome dos clubes. - Nome do estádio. - Público
presente. - Placar. - Autor de cada gol. - Expulsões, marcadas perto do
nome do clube. - Lesões quando ocorrerem. - Linha do tempo ou indicador
visual do andamento da partida.

Gols podem ter efeitos visuais e som opcional, controlado nas
configurações.

**Referência visual (rev. 14):** tela de jogos da rodada do Elifoot 98,
enviada pelo usuário. Descrição registrada em `docs/referencia-tela-jogos.md`.
A imagem não fica no repositório.

A tela deve mostrar **todos os jogos da divisão simultaneamente**. Enquanto
a partida do usuário acontece, as outras seguem na mesma tela. A ordem das
partidas pode seguir a classificação da liga.

### 10.3 Plantel e gestão

-   A tela de elenco deve preservar a leitura simples do Elifoot 98, com
    modernizações.
-   Durante o jogo, ao clicar em um clube, o usuário vê apenas os 11
    titulares de cada time (11 x 11), sem os reservas.
-   Se o clube for controlado pelo usuário, poderá fazer substituições
    nas condições permitidas.
-   Não permitir ações de gestão que pertençam a outro participante.

### 10.4 Configurações

**Definido — velocidade das partidas:** no modo individual, o próprio
usuário escolhe. No jogo em rede, somente o administrador define, e ela
vale para todos.

O jogo deve oferecer: - Velocidade das partidas. - Som dos gols
ligado/desligado. - Modo de acompanhamento de partidas sem usuários:
visual minuto a minuto ou resultado/processamento rápido. - Sorteio de
copa passo a passo ou rápido. - Configuração do estádio. - Opções de
gestão e exibição compatíveis com o modo individual ou multiplayer.

## 11. Relógio da partida e sincronização multiplayer

### 11.1 Preparação por turnos

O fluxo base deve ser por turnos: 1. O jogo apresenta a fase de gestão.
2. Cada treinador define tática, escalação e demais ações permitidas. 3.
Cada participante confirma que está pronto. 4. Quando todos os
participantes obrigatórios estiverem prontos, a sessão avança para a
fase de partidas. 5. Os resultados são processados e a temporada avança
de forma consistente.

O estado da sessão deve mostrar quem já confirmou e quem ainda está
configurando.

### 11.2 Partidas simultâneas e em segundo plano

-   As partidas relevantes à divisão dos usuários devem ser exibidas na
    velocidade configurada.
-   Se todos os usuários estiverem na mesma divisão, as partidas de
    outras divisões poderão ser processadas em segundo plano.
-   Se os usuários estiverem em divisões diferentes, o processamento
    deve respeitar etapas que mantenham a consistência do turno.
-   Partidas sem usuários, inclusive copas em que os usuários já foram
    eliminados, podem usar resultados rápidos.

**Em aberto:** definir a ordem exata de processamento quando
participantes estão em divisões diferentes e quando existem competições
sobrepostas.

**Definido (rev. 13):** divisões sem usuário não usam a exibição minuto a
minuto. O resultado sai em instantes.

**Definido (rev. 14):**
- Usa-se o **mesmo motor em modo rápido** (cerca de 0,1 ms por partida;
  uma temporada de 380 jogos leva cerca de 40 ms). Não há motor separado.
- **Fluxo da rodada:**
  1. Todos os treinadores definem a tática e confirmam.
  2. Abre-se a tela de jogos da rodada, com todas as divisões (§10.2).
  3. Ordem de processamento: começa pela divisão de **maior número de
     prioridade** (a mais inferior) e sobe até a 1ª.
     - Divisões **sem usuário** saem instantaneamente, com todos os jogos
       simultâneos.
     - Divisões **com usuário** rodam minuto a minuto, na velocidade
       configurada.
  4. Se o país tiver uma única divisão, todos os jogos da rodada (do
     usuário e da máquina) aparecem nessa tela.

**Em aberto:**
- Quando há usuários em mais de uma divisão (por exemplo, na 1ª e na 3ª),
  se essas divisões rodam ao mesmo tempo, como no Elifoot 98, ou uma depois
  da outra.
- Se as divisões sem usuário aparecem já com o placar final na mesma tela,
  ou são mostradas antes.

**Definido:** o processamento segue a prioridade em ordem **decrescente**.
As divisões de número maior vêm primeiro (por exemplo, 3, depois 2) e a
liga principal (1) por último. Com uma única liga, basta a prioridade 1.

### 11.3 Ações privadas e pausas

-   Ações privadas, como escolher substituições ou selecionar o cobrador
    de um pênalti, devem permanecer ocultas dos demais até o momento
    apropriado.
-   Os demais participantes podem ver eventos públicos, como quem vai
    bater o pênalti e o resultado, sem ver a escolha privada
    antecipadamente.
-   Se um participante abrir uma janela de gestão durante a partida, a
    política de pausa precisa ser definida. A intenção original é que o
    relógio compartilhado pare quando alguém abrir certas telas e só
    continue quando a tela for fechada; isso pode prejudicar a
    experiência dos demais e deve ser validado antes de se tornar regra.
-   O relógio e os eventos precisam permanecer consistentes para todos
    os participantes.
-   Cada usuário deve conseguir acompanhar sua própria tela, sem que
    ações privadas de outro usuário sejam expostas.

### 11.4 Desconexão em rede

**Definido:**
- **Se o participante cair durante a partida** (tela minuto a minuto): a
  partida **continua até o fim**. O servidor é a autoridade do relógio
  (`docs/decisions/0004`).
- **Se cair na fase de gestão** (tela de plantel, como no Elifoot 98): a
  sessão **não avança** para a próxima fase enquanto ele estiver
  desconectado. O jogo fica parado aguardando a reconexão.

**Em aberto:**
- Tempo limite de espera e o que fazer com quem nunca volta: o
  administrador remove o participante, o clube passa à máquina ou a sessão
  fica parada indefinidamente.
- Se o próprio administrador desconectar.
- Como saber que um navegador "caiu" usando polling. Hipótese: sem
  consulta por N segundos.

## 12. Regras de penalidades, lesões e eventos

-   Pênaltis só precisam ser apresentados como interação especial quando
    ocorrerem em uma partida com pelo menos um usuário participante,
    seja contra ou a favor do clube do usuário.
-   Para participantes, a escolha do cobrador deve ser privada; o
    resultado e os eventos públicos serão compartilhados.
-   Para partidas sem usuários, o motor pode processar o evento sem
    interação visual detalhada.
-   Gols, expulsões e lesões devem gerar eventos consistentes para todos
    os clientes que acompanham a partida, no mesmo estilo elifoot98

**Definido (rev. 7) — batedor de pênalti:**
- O usuário escolhe o batedor na tela do plantel. A escolha é privada.
- Se não escolheu, ou se o escolhido não está em campo, bate o jogador
  **de maior força em campo**.
  - **Provisório:** o goleiro fica fora desse critério.
- Empate de força: tem prioridade quem tem **asterisco**.
- Persistindo o empate: **sorteio**.

**Provisório:** conversão de pênalti de **75%** (média do futebol real).
Meta de frequência: cerca de 0,3 pênalti por jogo (§7.6).

**Em aberto:** com a escolha prévia no plantel, se ainda deve existir a
interação especial no momento do pênalti, descrita no início desta seção.

## 13. Critérios iniciais de aceitação

A primeira implementação de cada sistema deve atender aos critérios
aplicáveis:

### Configuração da liga

-   Uma liga inválida por falta de clubes não pode iniciar.
-   A mensagem de validação deve explicar o que falta.
-   A quantidade mínima de clubes segue a fórmula da §4.1 (soma das
    divisões + rebaixados da última divisão).

### Motor de partidas

-   A mesma semente e as mesmas entradas devem permitir reproduzir a
    simulação, quando o motor for projetado para isso.
-   Táticas diferentes devem produzir diferenças mensuráveis, sem tornar
    um resultado garantido.
-   A vantagem do mando de campo deve ser observável em simulações em
    lote.
-   Placares extremos devem ser raros em equipes equivalentes.
-   Expulsões e lesões devem respeitar limites e regras de duração.

### Economia

-   As receitas e despesas devem ser rastreáveis por rodada.
-   Um clube sem saldo deve seguir uma política previsível, sem vendas
    duplicadas.
-   O sistema deve respeitar o elenco mínimo definido.
-   A compra por leilão deve respeitar o orçamento disponível.

### Multiplayer

-   Uma fase não avança antes de todos os participantes obrigatórios
    confirmarem, conforme a regra aprovada.
-   Na fase de gestão, a sessão não avança enquanto houver participante
    desconectado. Uma partida em andamento termina mesmo com participantes
    desconectados (§11.4).
-   Uma ação não pode ser processada duas vezes por causa de reconexão
    ou mensagens repetidas.
-   As escolhas privadas não devem aparecer para os demais
    participantes.
-   Todos os participantes devem receber o mesmo resultado oficial da
    partida.

### Interface

-   As configurações de velocidade e som devem ser respeitadas.
-   Eventos importantes devem ser identificáveis visualmente.
-   Uma partida sem usuários pode ser processada sem exigir a
    apresentação minuto a minuto.

## 14. Questões em aberto prioritárias

Situação após a rev. 2 (2026-10-26). As respostas foram incorporadas nas
seções indicadas.

| # | Questão | Situação |
|---|---|---|
| 1 | Plataforma e tecnologia | **Definido:** somente web; stack na §16 |
| 2 | Modelo multiplayer e desconexões | **Definido** em parte: servidor autoritativo (`docs/decisions/0004`) e regra de desconexão (§11.4). **Em aberto:** tempo limite e abandono |
| 3 | Força do clube | **Definido:** força cadastrada obrigatória, com teto por divisão (§4.2–§4.3). **Em aberto:** média simples ou ponderada |
| 4 | Fórmulas do Elifoot 98 mantidas/alteradas | **Em aberto:** a referência foi inspecionada (`docs/referencia-elifoot98.md` §7) e as divergências estão listadas para decisão |
| 5 | Progressão de titulares/reservas | **Em aberto** (§6.1); a referência confirma −1 com 50% e âncora ±5 |
| 6 | Moral | **Provisório:** escala 0–30, início 18, valores por resultado (§6.2) |
| 7 | Demissões | **Em aberto** (§5.4) |
| 8 | Lesões: limite e definição | **Em aberto** (§7.5); influência do temperamento **Definida** |
| 9 | Fórmulas financeiras | **Em aberto:** a referência não cobre; precisam de tabelas próprias (§9.1) |
| 10 | Estádio e rebaixamento | **Em aberto** (§9.2) |
| 11 | Copas: classificação, sorteio, não potência de 2 | **Definido:** ida e volta, final única, prorrogação e pênaltis, exclusão do mais fraco (§8.2). Restam detalhes em aberto |
| 12 | Mundial de clubes | **Definido:** exige clube de cada região (§8.3.4). **Em aberto:** formato exato |
| 13 | Pausa ao abrir tela de gestão durante a partida | **Em aberto** (§11.3) |
| 14 | Imagem de fundo | **Em aberto** (§10.1) |
| 15 | Escopo do primeiro protótipo | **Definido:** etapas 0 a 2 da §15 |

Novas questões abertas na rev. 2:
- Formato de importação/exportação e de save (§3.5).
- Serviço de e-mail (§3.4).
- Moeda por clube ou liga (§9.1.1).
- Detalhes das finais únicas (§8.2).
- Distribuição de posições no elenco (§5.2).
- Lista de países equiparados a nacionais (§8.4).

## 15. Plano de implementação

**Definido** em `docs/decisions/0001-ordem-de-implementacao.md`. Este plano
substitui os dois planos anteriores (antiga §15 e antiga §18). Cada etapa só
começa depois que a anterior estiver testada.

### Etapa 0 — Base técnica mínima
- Projeto Java 21 + Spring Boot com Maven Wrapper (`docs/decisions/0002`).
- PostgreSQL local, Flyway com a primeira migração e Testcontainers.
- Health check e README com comandos para rodar a aplicação, os testes
  unitários, os testes de integração e as migrações.
- Estrutura de pacotes separando domínio, motor, temporada, economia,
  multiplayer, persistência e web (CLAUDE.md §4).

### Etapa 1 — Motor de partidas (domínio puro)
- Clube, jogador, formação e escalação mínimos, sem banco.
- Motor minuto a minuto com semente, inspirado em
  `docs/referencia-elifoot98.md`, com parâmetros configuráveis.
- Gols, finalização, mando de campo, táticas da §7.2, cartões
  amarelo/vermelho e expulsão.
- Simulador em lote (ferramenta de desenvolvimento) com relatório de
  placares, mando, táticas e cartões.
- Lesões e asterisco só depois que as decisões em aberto forem tomadas.

### Etapa 2 — Liga e turno (domínio puro)
- Validação de clubes (§4.1).
- Calendário em turno e returno, pontuação e desempate (§8.1).
- Promoção, rebaixamento, fila de espera e teto de força por divisão.
- Moral (§6.2) e transição de temporada.
- Simulação de temporadas completas em lote.

### Etapa 3 — Persistência, contas e modo individual jogável
- Migrações e repositórios para o estado da temporada.
- Avanço de rodada idempotente.
- Script de administrador, link de cadastro e papéis (§3.4).
- Banco padrão gerado: 25 jogadores por clube, 5 a 7 estrangeiros (§5.2).
- Telas Thymeleaf + HTMX:
  - plantel e tática;
  - partidas simultâneas da divisão (§10.2);
  - classificação;
  - velocidade e som.

### Etapa 4 — Economia
- Depende das tabelas a definir (§9.1). Antes de implementar, propor as
  tabelas e validá-las por simulação de temporadas.
- Contratos, salários, bilheteria, estádio, moedas, empréstimos, juros,
  mercado, leilões e patrocínios.

### Etapa 5 — Multiplayer
- Sala, convites por e-mail, confirmação de turno, polling e velocidade
  definida pelo administrador.
- Desconexão (§11.4), privacidade e processamento único.

### Etapa 6 — Competições avançadas e balanceamento
- Copa nacional (§8.2), competições continentais e Mundial (§8.3).
- Importação/exportação de dados e saves (§3.5).
- Balanceamento por simulação de temporadas completas.
- Avaliar SSE apenas se o polling for insuficiente. Reavaliar React apenas
  se a interface da partida exigir.

## 16. Stack técnica e arquitetura inicial

### 16.1 Stack definida

| Área | Tecnologia | Diretriz |
|---|---|---|
| Linguagem | Java 21 | Versão de referência do backend |
| Backend | Spring Boot | Aplicação web e regras do jogo |
| Banco de dados | PostgreSQL | Fonte persistente oficial do estado da temporada |
| Migrações | Flyway | Histórico versionado de alterações no schema |
| Testes unitários | JUnit 5 | Regras de domínio, cálculos e serviços |
| Testes de integração | Testcontainers | PostgreSQL real para testes que dependem de SQL, constraints e migrações |
| Interface inicial | Thymeleaf + HTMX | Renderização no servidor e atualizações parciais |
| Avisos de rodada/eventos | Polling simples inicialmente; SSE como evolução possível | WebSocket não é necessário na primeira etapa |

**Definido:** Maven com Maven Wrapper (`docs/decisions/0002-maven-como-build.md`). Testes unitários rodam com `mvn test` (Surefire). Testes de integração (`*IT.java`) rodam com `mvn verify` (Failsafe).

### 16.2 Interface: Thymeleaf + HTMX ou React

A escolha inicial recomendada é **Thymeleaf + HTMX**. O jogo é predominantemente de gestão, formulários, tabelas, elenco, classificação, finanças e avanço por turnos; renderizar páginas no servidor reduz a complexidade inicial e integra-se naturalmente ao Spring Boot.

React não está proibido. Deve ser reconsiderado se um protótipo demonstrar que a tela de partida precisa de muitas interações locais, animações ou atualizações complexas que sejam difíceis de manter com Thymeleaf + HTMX. Não manter duas arquiteturas de frontend sem uma razão clara.

### 16.3 Atualização de rodada e partidas

A primeira versão pode usar polling para consultar se há nova rodada, eventos ou alterações relevantes:
- Definir intervalos de consulta razoáveis e ajustáveis.
- Consultar somente o necessário; evitar recarregar todo o estado do jogo sem necessidade.
- O backend é a fonte oficial dos resultados e do estado da sessão.
- Polling não pode causar avanço duplicado da rodada ou repetir transações.
- SSE pode ser introduzido para notificações do servidor ao navegador quando houver necessidade de menor latência.
- WebSocket fica fora do escopo inicial, salvo requisito concreto que justifique comunicação bidirecional persistente.

### 16.4 Banco de dados e migrações

- PostgreSQL será o banco principal.
- Todas as mudanças estruturais do banco devem ser geridas por migrações Flyway versionadas.
- Não editar migrações já aplicadas em ambientes compartilhados; criar uma nova migração para corrigir ou evoluir o schema.
- Definir constraints e transações para proteger invariantes importantes, como identidade dos clubes, integridade do calendário, saldos e processamento único dos resultados.
- Não usar H2 como substituto único do PostgreSQL para testes de integração que dependam de comportamento específico do banco.
- Credenciais não devem ser armazenadas no código nem commitadas no repositório.

### 16.5 Estratégia de testes

- JUnit 5 para testes unitários de domínio e serviços.
- Testcontainers para subir PostgreSQL real nos testes de integração relevantes.
- Testar migrações em banco vazio e em atualizações de schema representativas.
- Testar transações e concorrência em operações sensíveis: avançar turno, concluir partida, transferir jogador, processar leilão, pagar salários e aplicar juros.
- O motor de partidas deve suportar simulações reproduzíveis por semente aleatória, quando viável, para facilitar investigação de resultados e regressões.
- Testes estatísticos devem usar conjuntos de simulações e tolerâncias justificadas, evitando falhas aleatórias causadas por uma única partida.

### 16.6 MCP e ferramentas externas

MCP não é uma dependência da aplicação. É uma integração opcional para o ambiente de desenvolvimento.

Possibilidades:
- **MCP de GitHub:** útil para consultar issues, pull requests e repositórios se o fluxo de trabalho passar por GitHub.
- **MCP de PostgreSQL:** útil para inspecionar schema e consultar dados de desenvolvimento, mas deve usar um banco de desenvolvimento e permissões mínimas. Evitar permitir escrita ampla ou acesso à produção sem necessidade explícita.

Não configurar MCP só por estar disponível. O uso deve resolver uma tarefa concreta, e as permissões e comandos acessíveis devem ser revisados antes da conexão.

## 17. Skills recomendadas para Claude Code

As skills são procedimentos reutilizáveis de desenvolvimento, armazenados no projeto em `.claude/skills/<nome>/SKILL.md`. Elas não são bibliotecas Java, plugins de runtime nem substituem esta especificação. Devem ser focadas, carregadas quando relevantes e revisadas como código.

### Skills iniciais recomendadas

1. **`implement-feature`** — ler o requisito, identificar ambiguidades, planejar os arquivos afetados, implementar em incrementos e executar testes.
2. **`spring-boot-feature`** — criar ou alterar funcionalidades seguindo as convenções de Spring Boot, validação, transações, tratamento de erros e separação de camadas.
3. **`flyway-migration`** — avaliar mudanças de modelo, escrever migrações Flyway seguras e verificar migração em banco vazio e existente.
4. **`integration-tests`** — criar testes de integração com JUnit 5 e Testcontainers, incluindo preparação de dados e limpeza.
5. **`match-engine-simulation`** — executar lotes de partidas reproduzíveis, medir distribuição de placares e eventos e propor ajustes de balanceamento com evidências.
6. **`review-changes`** — revisar diff por bugs, regressões, problemas de concorrência, segurança, migrações ausentes e testes insuficientes.

Começar com poucas skills. Skills para deploy, observabilidade, performance ou design visual podem ser acrescentadas quando o projeto realmente exigir esses fluxos.
