# SPEC.md --- Especificação inicial do simulador de futebol

-   **Status:** versão inicial para revisão (rev. 55 — 2026-10-28)
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

-   **rev. 55 (2026-10-28):** evolução corrigida pelo usuário: cada jogador perde no máximo 1 ponto por partida e o resultado decide quantos perdem (1, 3 ou 5); simulação refeita, nova proposta do agente (crescem 4, 2 ou 0,5 dos que não jogaram, com piso); modelos 6c (estádio) e 9a (patrocínio) aprovados.
-   **rev. 54 (2026-10-28):** modelo 8a (histórico) aprovado; um treinador por clube, obrigatório ao criar ou editar clube; patrocínio volta à caixa de escolha da rev. 40; estádio até 120.000 lugares para todos, com janela nas finanças (capacidade, ingresso fixo por clube e divisão, ampliação de 5.000 lugares pelo preço de um jogador de força 50); regra de evolução de força descrita pelo usuário e simulada (`docs/simulacoes/evolucao-forca.md`), proposta do agente aguardando aprovação. Gravados os treinadores da máquina e a aba Histórico (V23).
-   **rev. 53 (2026-10-28):** venda do usuário por leilão: o usuário define o lance mínimo na ficha do jogador e o resultado aparece na tela do 7b; com caixa negativo o jogo escolhe um jogador sem contrato renovado e, se todos estiverem renovados, a venda fica para o início da temporada seguinte, antes de tudo. Gravada a venda pela ficha (V21). Caixa negativo: o jogador é sorteado entre os sem contrato renovado (decisão do usuário, para que qualquer um possa sair) e gravado (V22).
-   **rev. 52 (2026-10-28):** leilões da máquina antes da rodada gravados (V20, modelo 7b, ADR 0012): abertos ao confirmar a tática, lances do usuário e depois da máquina, resultado e "Ir para a rodada"; compra direta de jogador em leilão aberto cancela o leilão (proposta do agente).
-   **rev. 51 (2026-10-28):** janela do clube na classificação com as abas Calendário e Plantel (modelo 7a aprovado com a coluna de gols pedida pelo usuário): plantel inteiro, gols na temporada e botão Comprar nos jogadores à venda.
-   **rev. 50 (2026-10-28):** finanças pelo modelo 6b (aprovado): linha do patrocínio, coluna "Mercado e prêmios" com o jogador comprado ou vendido; usuário quer ver o plantel inteiro ao clicar no clube (modelo 7a revisado para aprovação).
-   **rev. 49 (2026-10-28):** compra direta no servidor (V19): paga o preço da tabela ao clube da máquina, recusa sem caixa, com 25 jogadores ou durante a rodada; lançamentos de compra e venda no extrato. Tela ainda não ligada.
-   **rev. 48 (2026-10-28):** patrocínio lançado no início de cada temporada (rodada 0) e prêmio da liga na última rodada (V18); a tela de finanças mostra o patrocínio dentro do saldo do início.
-   **rev. 47 (2026-10-28):** jogadores à venda da máquina gravados (V17), completados a cada temporada e rodada; salário gravado adiado até a força poder mudar (ADR 0011).
-   **rev. 46 (2026-10-28):** prêmio e patrocínio pelo teto de força da divisão; patrocínio cai 10% do 1º para o 2º e 8% a cada posição seguinte; só o título da 1ª divisão conta no ranking de treinadores.
-   **rev. 45 (2026-10-28):** usuário aprovou a proposta do mercado da máquina (rev. 44) e definiu a premiação da liga (campeão = 2 jogadores de força 50 com asterisco, 2º 30% menos, 3º 10%) e o patrocínio (cerca de 10% do prêmio); proposta de força de referência por divisão.
-   **rev. 44 (2026-10-28):** proposta simulada do mercado da máquina (lance 10% abaixo mais até 5% pela força, 1 venda e 1 compra por rodada, interesse até 5 pontos abaixo da força em campo, folga de 5 rodadas da folha na compra), aguardando aprovação.
-   **rev. 43 (2026-10-28):** estrangeiro 25% mais barato e asterisco 25% mais caro; a máquina só compra por leilão, renova sempre e deixa 5 ou 6 jogadores sorteados à venda; compra e venda da máquina pelo saldo comparado a 10 rodadas da folha; saldo negativo põe jogador em leilão sem lance mínimo; no início da temporada todos ficam sem contrato e só quem ganha abaixo da força pede aumento (1 por rodada); patrocínio entra de uma vez no início, para todos os clubes.
-   **rev. 42 (2026-10-28):** leilão: empate por sorteio, sem lance não vende até o fim da rodada, máquina dá lance depois do OK dos usuários, usuário sem caixa não precisa ser esperado; estrangeiro 10% mais barato; empréstimo retirado; todo clube tem treinador e o ranking inclui todos; patrocínio proporcional à posição anterior (igual no 1º ano) e por temporada; contrato só até o fim da temporada.
-   **rev. 41 (2026-10-28):** tabela de preço proposta (80 rodadas de salário, provisória); leilão resolvido na hora: os da máquina depois do OK da tática e antes do minuto a minuto; o do usuário quando ele põe o jogador à venda.
-   **rev. 40 (2026-10-28):** ano da temporada (começa em 2026, configurável, +1 por temporada); histórico só de campeões e ranking de títulos dos treinadores; regras do usuário para mercado (compra direta pelo preço da força, leilão 20% mais barato com lance de cada um), contrato em temporadas, pedido de aumento, empréstimo em blocos de 500 mil a 1% por rodada e patrocínio escolhido a cada início de ano.
-   **rev. 39 (2026-10-28):** modelo da aba Finanças aprovado (só o clube do usuário); saldo do caixa no plantel; público no quadro da rodada e na janela; estádio com nome geral, sem nome próprio por clube; jogadores à venda no clique do clube ficam para o mercado.
-   **rev. 38 (2026-10-28):** valores do caixa da rodada aprovados como ponto de partida, ajustáveis por simulação; o caixa é lançado no avanço da rodada; clubes da fila de espera não têm lançamentos.
-   **rev. 37 (2026-10-28):** moeda exibida pelo país da liga e valores guardados em centavos de Real; a economia começa pelo caixa da rodada; as tabelas de valores são propostas pelo agente e validadas por simulação antes do código.
-   **rev. 36 (2026-10-28):** usuário com clube rebaixado para a fila é demitido e assume um clube sorteado entre os mais fracos da última divisão (provisório até os convites); modelo do calendário do clube aprovado.
-   **rev. 35 (2026-10-28):** rodada ao vivo com relógio no servidor (ADR 0010); velocidades lenta, normal e rápida; divisão sem usuário em cerca de 5 s; sair da tela não pausa a partida.
-   **rev. 34 (2026-10-28):** no plantel, botões Titular/Banco/Fora em cada linha; suspenso sai sozinho para fora do banco e o treinador completa o time; escalação inicial da máquina (provisório); até o minuto a minuto, "Confirmar e jogar rodada" joga a rodada inteira e mostra o placar.
-   **rev. 33 (2026-10-28):** quadro da rodada mostra o último lance com ícone; clicar num clube abre uma janela pequena sobre o quadro, como no Elifoot 98, com lances em ordem e, no próprio clube, substituição; clube da máquina mostra a prévia dos 11 de cada lado em vez do 11 x 11 com detalhes; modelos aprovados.
-   **rev. 32 (2026-10-27):** no banco padrão todos os jogadores são do país do clube; estrangeiros entram pelo editor; adaptação dos formatos aprovada; pesos do temperamento.
-   **rev. 31 (2026-10-27):** países e formato do banco padrão (até 3 divisões, até 20 clubes, pontos corridos, acesso direto); força do clube pela média em campo; faixas de força por divisão; asteriscos de 0 a 3 por clube; temperamento sorteado com peso; plantel com gols antes dos amarelos e sigla do país do estrangeiro.
-   **rev. 30 (2026-10-27):** banco padrão com formatos reais pesquisados e nomes fictícios (compostos, só nome ou só sobrenome); clicar num clube durante o minuto a minuto pausa a partida e mostra as escalações, e no próprio clube abre as substituições.
-   **rev. 29 (2026-10-27):** tela inicial só com novo jogo e jogo gravado; vários jogos gravados por usuário; dois caminhos do administrador no novo jogo; clube com usuário não aceita outro usuário; sorteio em rede com grupo maior a cada usuário; entrada em rede pela sala antes da rodada 1 (provisório); jogo individual testado antes da rede.
-   **rev. 28 (2026-10-27):** modo de início do jogo (sorteio ou escolha) definido pelo administrador e válido para todos; jogo individual do participante sempre por sorteio; treinadores em rede no plantel; modelos de entrada, novo jogo e quadro de escolha aprovados; molde pessoal salvo no servidor como possibilidade futura.
-   **rev. 27 (2026-10-27):** clube pertence ao país, não à divisão; a liga só define o formato das divisões; clubes distribuídos pela força ao criar o jogo, com empate sorteado; prioridade automática; promovidos calculados; excluir divisão sobe as de baixo; quadro de escolha do administrador.
-   **rev. 26 (2026-10-27):** divisões precisam estar completas para o jogo começar e a estrutura não muda durante o jogo; clube começa com a força cadastrada em qualquer divisão e perde força aos poucos até o teto; treinador da máquina com clube rebaixado para a fila segue a demissão por moral.
-   **rev. 25 (2026-10-27):** todo jogo tem dono; empatados com o 4º mais fraco também entram no sorteio do clube inicial; o administrador pode escolher o clube ou pedir o sorteio; clube só de divisão, nunca da fila; divisão sem clubes suficientes fica em aberto.
-   **rev. 24 (2026-10-27):** link de cadastro de uso único válido por 7 dias; login por e-mail e senha de 8 ou mais caracteres; recuperação de senha pelo administrador por enquanto.
-   **rev. 23 (2026-10-27):** o segundo amarelo na mesma partida também conta no acumulado de amarelos.
-   **rev. 22 (2026-10-27):** escalação e tática da máquina pelo plantel; pesos 70/25/5 do vermelho direto; improvisação com elenco curto; o que a rodada grava.
-   **rev. 21 (2026-10-27):** nacionalidade do jogador pode ser qualquer país do mundo; bandeira no plantel para estrangeiros, atenuada quando o jogador não conta como estrangeiro.
-   **rev. 20 (2026-10-27):** save em JSON versionado; banco com ligas de vários países, só a do país escolhido é jogada; molde editável copiado para cada jogo.
-   **rev. 19 (2026-10-27):** números do asterisco; bônus de artilharia soma com o asterisco e vale para qualquer defensor; conflito resolvido.
-   **rev. 18 (2026-10-27):** sorte/azar por divisão e oculto; bônus de artilharia como asterisco temporário, 15 por divisão; regras do asterisco; calendário por ordem e finais depois da liga.
-   **rev. 17 (2026-10-27):** prêmio de artilharia dividido no empate; calendário do clube; elenco padrão de 23; sorte/azar da temporada; bônus de artilharia; efeito do asterisco (detalhes em aberto).
-   **rev. 16 (2026-10-27):** histórico de gols do jogador e artilheiros; divisão sem usuário sempre rápida; modelos de escudo; telas aprovadas no modelo (§10.5).
-   **rev. 15 (2026-10-27):** moral com retorno leve; clube inicial sorteado entre os 4 mais fracos da última divisão; divisões exibidas em sequência.
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
- **Definido (rev. 15):** o clube do participante comum é **sorteado entre
  os 4 clubes de menor força da última divisão** do país escolhido. Exemplo:
  Inglaterra com uma só divisão leva ao sorteio entre os 4 mais fracos dela.
  - Só num jogo no **modo escolha** (abaixo, rev. 28) o clube é escolhido.
  - **Provisório:** o número 4 fica configurável.
  - A essência do Elifoot 98 é começar por baixo, assumir outros times e
    ser demitido.
- **Definido (rev. 25):**
  - Todo jogo tem um **dono** (quem o criou), que é também o seu treinador
    humano.
  - **Empate no grupo:** se outros clubes empatam com o último do grupo dos
    mais fracos, todos entram no sorteio. Uma divisão com menos clubes que
    o grupo sorteia entre todos.
  - O administrador define o **modo** do jogo (abaixo, rev. 28): escolha
    ou sorteio.
  - O clube inicial, escolhido ou sorteado, é sempre de uma **divisão**;
    clube da fila de espera não serve.
  - O sorteio usa uma semente gravada no jogo, para poder ser reproduzido.
- **Definido (rev. 28) — modo de início do jogo:** ao criar o jogo, o
  administrador define o **modo**, que vale para **todos** os treinadores
  daquele jogo:
  - **Sorteio:** todos, inclusive o administrador, recebem um clube
    sorteado entre os mais fracos da última divisão (regras acima).
  - **Escolha:** cada treinador escolhe qualquer clube de divisão. Num jogo
    em rede, todos os participantes escolhem, não só o administrador.
  - Só o administrador define o modo. O jogo individual do participante
    comum é sempre no modo sorteio.
  - **Definido (rev. 29):** o administrador define o modo pelo caminho que
    escolhe no novo jogo: **"Sortear clube e começar"** é o modo sorteio e
    **"Escolher clube"** é o modo escolha, que abre o quadro de escolha de
    clube.
  - **Provisório:** o modo fica gravado no jogo, para valer para quem
    entrar depois no jogo em rede (Etapa 5).
  - **Definido (rev. 29):** um clube que já tem treinador usuário **não
    aceita outro usuário**. No modo escolha, ele aparece bloqueado no quadro
    dos outros; se dois confirmarem ao mesmo tempo, fica com quem o servidor
    processar primeiro.
  - **Definido (rev. 29) — sorteio em rede:** o grupo de candidatos cresce
    **um clube por usuário**: 1 usuário sorteia entre os 4 mais fracos, 2
    entre os 5, 3 entre os 6, e assim por diante (grupo = 4 + usuários − 1;
    o 4 continua configurável). Clubes que já têm usuário saem do sorteio, e
    os empatados com o último do grupo continuam entrando.
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

**Definido (rev. 24):**
- O link de cadastro é de **uso único** (um link por pessoa) e expira em
  **7 dias** se não for usado. O prazo é configurável.
- A pessoa entra com **e-mail e senha**. O e-mail é único, sem diferença
  entre maiúsculas e minúsculas.
- A senha tem **8 a 72 caracteres** (72 bytes é o limite do BCrypt) e é
  guardada só como hash.
- Por enquanto não há recuperação de senha pelo próprio usuário: o
  administrador pode redefinir a senha de um participante.

**Em aberto:**
- Validade e uso do **convite para sessão em rede** (Etapa 5).
- Recuperação de senha pelo próprio usuário (depende do serviço de e-mail).
- Qual serviço de e-mail será usado. É uma dependência externa e precisa de
  credenciais fora do repositório.

### 3.5 Dados padrão, importação/exportação e salvamento

**Definido:**
- O jogo traz um **banco de dados padrão** com nomes gerados (sorteados)
  para clubes, jogadores, estádios e treinadores.
- O editor permite **exportar** as modificações feitas.
- O usuário pode **importar** um arquivo com suas modificações. A importação
  deve ser validada antes de uso (§7 do CLAUDE.md).

**Definido (rev. 20) — vários países:**
- O banco padrão traz as ligas dos **principais países** (Brasil, Argentina,
  Colômbia e outros), com nomes aleatórios. A lista de países e seus
  formatos seguem a pendência da §4.1.
- A pessoa pode **criar ou alterar** países, ligas, divisões e clubes no
  editor.
- Quem joga escolhe **um país**. As ligas dos outros países **não são
  jogadas**; seus clubes servem para copas continentais e Mundial
  (§8.3.3).
- O banco editável é um **molde**: ao iniciar um jogo, o servidor faz uma
  cópia, e o jogo evolui sem alterar o molde (`docs/decisions/0006`).

**Em aberto (rev. 28) — molde pessoal no servidor:** o editor pode entrar
mais à frente. Hoje vale o arquivo exportado na máquina da pessoa, para o
servidor não guardar esses dados. No futuro, a pessoa poderá salvar as
edições num **registro em seu nome** no servidor e jogar sobre elas; os
outros jogadores criam os seus ou jogam o banco padrão. Decidir na etapa
do editor.

**Em aberto:**
- Formato do arquivo de importação/exportação. **Proposta:** JSON com
  versão de formato, para permitir migração futura.

**Definido — salvar na máquina pessoal:**
- Enquanto o jogo está em andamento, o estado fica no servidor, porque a
  simulação roda lá (`docs/decisions/0004`).
- **Definido (rev. 29):** cada usuário pode ter **vários jogos gravados** no
  servidor e voltar a qualquer um deles.
- O usuário pode **baixar um arquivo de save** para o computador e depois
  **apagar o jogo do servidor**.
- Para continuar, o usuário envia o arquivo de volta. O servidor
  **revalida** o arquivo antes de carregar, porque ele pode ter sido
  alterado.
- No multiplayer, o estado oficial permanece no servidor durante a sessão
  (§6 do CLAUDE.md).

**Definido (rev. 20):** o arquivo de save é **JSON com número de versão do
formato**, para permitir migração de saves antigos.

**Em aberto:**
- Se um save alterado manualmente é recusado ou aceito após validação.

### 3.3 Sala multiplayer

A sala deverá exibir, no mínimo: - Quantidade de participantes
conectados. - Nome do treinador de cada participante. - Clube escolhido
por cada participante. - Estado de cada participante na fase atual (por
exemplo, configurando, pronto ou desconectado).

**Provisório (rev. 29) — entrada no jogo em rede:**
- O administrador cria o jogo (campeonato e modo). O jogo fica na **sala**,
  sem rodada jogada, e ele envia os convites.
- Quem entra pelo convite não escolhe campeonato nem modo: vai direto para
  "Sortear clube e começar" (modo sorteio) ou para o quadro de escolha de
  clube (modo escolha), com os clubes já ocupados bloqueados.
- O administrador inicia a temporada quando todos estiverem prontos.

**Em aberto (Etapa 5):** entrar num jogo em rede depois da rodada 1.
**Proposta:** não permitir na primeira versão.

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

**Definido (rev. 30) — banco padrão e nomes:**
- O banco padrão é feito com os **formatos reais** das ligas, pesquisados e
  registrados com fonte e data, antes do jogo individual jogável.
- Clubes e jogadores têm **nomes fictícios**. Como são só nomes, podem
  coincidir com nomes que já existem no futebol.
- Nome do jogador em três formas: **nome composto** (nome e sobrenome),
  **só o nome** ou **só um sobrenome**, como nos apelidos de camisa.
- **Provisório:** os nomes são montados combinando listas de nomes e
  sobrenomes comuns; não se copia lista de jogadores reais.

**Definido (rev. 31) — países e formato do banco padrão:**
- Países: **Brasil, Argentina, Inglaterra, Espanha, Itália e Portugal**.
  Outros podem entrar depois.
- Até **3 divisões** por país.
- O jogo não precisa ser fiel: o formato real é **adaptado** à regra de 15
  a 20 clubes por divisão, em **pontos corridos**, com **acesso e
  rebaixamento diretos**, sem grupos nem playoffs. Ex.: uma divisão real de
  24 clubes vira 20; um acesso decidido em playoff vira acesso direto.
- A pesquisa registra o formato real e a adaptação feita, com fonte e data.

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
  - ~~Em aberto: o que acontece com o usuário demitido.~~ **Provisório
    (rev. 36, decidido pelo usuário):** no fim da temporada ele assume um
    clube **sorteado entre os mais fracos da última divisão**, como no
    início do jogo; clube de outro treinador não entra no sorteio. O clube
    novo começa com a escalação da máquina. Vale até existirem os convites
    de clubes (depois da regra de moral, §6.2, e de demissão, §5.4).
- **Clube da máquina rebaixado para a fila:** fica sem liga até ser
  sorteado para subir.
  - **Definido (rev. 26):** só o usuário é demitido na hora. O treinador
    da máquina segue as regras de demissão por moral (§5.4); se for
    demitido, outro treinador do jogo assume o clube.
- **Rebaixado para outra divisão** (por exemplo, da 1ª para a 2ª): segue
  normalmente na nova divisão, seja de usuário ou da máquina.

**Definido (rev. 27) — formato da liga e clubes do país:**
- O clube é cadastrado no **país**, sem divisão. A liga do país só define o
  **formato**: nome e, para cada divisão, nome, quantidade de clubes,
  rebaixados e limite de força.
- **Prioridade automática:** cada divisão nova recebe o próximo número (1,
  2, 3...); não há como repetir nem pular. Ao **excluir** uma divisão, as de
  baixo sobem um número e mantêm nome e limite. Exemplo: excluindo a Série B
  de A, B e C, a Série C passa a ser a prioridade 2.
- **Promovidos calculados:** os promovidos de uma divisão são os rebaixados
  da divisão de cima; a principal não tem promovidos.
- **Campeonato disponível:** um país só aparece para escolha se tiver liga
  com formato válido e clubes suficientes. Exemplos: Premier League de 18
  com 3 rebaixados precisa de 21 ingleses; 100 clubes italianos sem liga
  cadastrada não formam campeonato; Itália com 1ª de 19 (2 rebaixados) e 2ª
  de 20 (4 rebaixados) precisa de 43.
- **Distribuição ao criar o jogo:** os clubes do país são ordenados pela
  força, do mais forte ao mais fraco. Os primeiros preenchem a 1ª divisão,
  os seguintes as de baixo, e o resto vai para a fila. **Empate de força é
  sorteado**: com 22 clubes de força 50 para 20 vagas, 20 são sorteados para
  a 1ª e 2 vão para a 2ª. A semente do jogo decide o sorteio.
- **Escolha do campeonato (tela depois do login):** o usuário escolhe o
  campeonato e o modo. O participante comum só tem o sorteio do clube; o
  administrador pode também escolher o clube. Nesse caso, a tela seguinte
  mostra um quadro com todos os clubes do país já nas suas divisões, pela
  distribuição sorteada, e clicar num clube mostra o plantel. O jogo é
  criado com essa mesma distribuição.

**Definido (rev. 26) — liga completa e fixa:**
- Todas as divisões configuradas precisam ter clubes suficientes. Se uma
  delas não tiver, a liga não pode iniciar e o editor diz o que falta.
- Depois que o jogo começa, nada é criado: não surgem clubes, jogadores
  nem divisões novas. Só mudam força, posição nas divisões e fila.

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

**Definido — limite de força por divisão (rev. 26):**
- **Em qualquer divisão, o clube começa com a força cadastrada**, sem
  adequação ao teto. Exemplo: cadastrados 15 clubes de força 50 e 5 de
  força 41 na primeira divisão, eles começam com 50 e 41.
- **Primeira divisão:** os jogadores podem progredir até 50.
- **Segunda divisão:** limite de **30**.
  - Um clube cadastrado com força maior (por exemplo, 35) começa nela com
    a força cadastrada. O teto impede que os jogadores cresçam acima de 30;
    os que estão acima vão **perdendo força** até chegar a 30.
  - O mesmo vale para um clube rebaixado com força 50: perde força jogo a
    jogo e pode voltar à primeira divisão antes de todos chegarem a 30.
  - **Conflito resolvido (rev. 26):** substitui a regra anterior de que o
    clube começava na divisão com no máximo o teto.
  - **Em aberto:** o ritmo dessa perda faz parte da mecânica de evolução e
    perda de força (§6.1), em que vitórias, empates e derrotas influem.

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

**Definido (rev. 31):** a força do clube é a **média dos 11 em campo**, na
escalação que a máquina escolheria (§7.4.1), não a média dos 23.

**Definido (rev. 31) — faixas de força do banco padrão:** 1ª divisão de
**35 a 50**, 2ª de **22 a 30** e 3ª de **12 a 22**. O limite (teto) das
divisões do banco padrão segue o topo da faixa: 50, 30 e 22.

## 5. Clubes, jogadores e treinadores

### 5.1 Clube

O editor de clubes deve permitir configurar: - Nome do clube. - Cores,
com paleta ampliada. - Divisão e força inicial. - Nome do estádio e
capacidade inicial. - Outros parâmetros necessários à liga.

### 5.2 Jogadores

O editor de jogadores deve permitir configurar: - Nome. - Posição. -
Nacionalidade. - Força. - Presença ou ausência do atributo especial
representado por asterisco. - Comportamento/temperamento.

**Definido (rev. 21) — nacionalidade:**
- Ao cadastrar um jogador, é possível escolher **qualquer país do mundo**,
  inclusive países sem liga no molde.
- **Definido (rev. 21):** a lista e os códigos são os dos **211 membros da
  FIFA** (ex.: POR, GER, NED; Inglaterra, Escócia, País de Gales e Irlanda
  do Norte separadas). Os países do molde usam os mesmos códigos.

Nesta versão, **não haverá idade dos jogadores**. O jogo manterá
rankings de artilheiros e o histórico de cartões de cada jogador (§7.4).

**Definido (rev. 16) — gols do jogador:**
- Cada jogador guarda os **gols na temporada** e os **gols na carreira**.
- Contam os gols de **todas as competições** (liga, copas, continentais,
  Mundial), inclusive gols de pênalti durante a partida.
- **Não contam** os gols da disputa de pênaltis.
- Todos os jogadores começam com **zero** gols na carreira; o histórico
  cresce durante o jogo salvo.
- Os gols na temporada zeram no início de cada temporada; os da carreira,
  nunca.
- **Artilheiros da temporada:** ranking pela soma dos gols de todas as
  competições da temporada. Cada temporada tem sua lista, usada para a
  premiação do artilheiro (§8.1).
- **Definido (rev. 17):** o prêmio vai para o clube que tem o artilheiro no
  plantel no fim da temporada. Com empate, o prêmio é **dividido** entre os
  artilheiros empatados (e seus clubes).
- **Artilheiros de todos os tempos:** ranking pelos gols na carreira, desde
  o início do jogo salvo.

**Definido — elenco gerado:**
- Cada clube tem **23 jogadores** cadastrados (composição abaixo; máximo de 25).
- Nomes e nacionalidades são gerados por padrão, por sorteio.
- ~~Cada clube tem **de 5 a 7 estrangeiros**, para não distorcer o limite de
  estrangeiros (§8.4).~~ **Substituído na rev. 32:** no banco padrão,
  **todos os jogadores são do país do clube**. Ao cadastrar ou editar um
  jogador, o editor permite escolher qualquer país.
  - **Conflito resolvido (rev. 32):** a instrução do usuário substitui a
    regra dos 5 a 7 estrangeiros.

**Definido (rev. 17) — composição padrão:**
- O cadastro padrão gera **23 jogadores**: 3 goleiros, 7 defensores,
  7 meias e 6 atacantes.
- O elenco pode chegar a **25**; as 2 vagas livres servem para compra e
  venda e dependem das finanças. A máquina também precisa dessa folga para
  negociar.
- ~~Os 5 a 7 estrangeiros continuam valendo dentro dos 23.~~ (rev. 32: sem
  estrangeiros no banco padrão.)

**Definido (rev. 17) — bônus de artilharia da temporada:**
- No início de cada temporada, o sistema sorteia cerca de **15 jogadores**
  que recebem um bônus na finalização, válido só naquela temporada.
- Podem ser sorteados: defensores, meias e atacantes, com ou sem
  asterisco (rev. 19). Goleiros não.
- **Definido (rev. 18):**
  - São **15 jogadores por divisão**.
  - Sorteio com peso: jogador com asterisco tem **peso 2**; os demais,
    peso 1.
  - O bônus é um **asterisco temporário**: o jogador sem asterisco passa a
    ter asterisco durante a temporada e o perde no fim, se não for
    sorteado de novo.
  - O asterisco temporário tem todos os efeitos do asterisco (§5.3),
    inclusive contar no limite de 3 em campo.
- **Definido (rev. 19):** o jogador que **já tem** asterisco e é sorteado
  ganha um bônus extra de finalização na temporada, do mesmo tamanho do
  bônus do asterisco, que **soma** ao do asterisco.

O temperamento pode assumir:
- Calmo.
- Educado.
- Normal.
- Ansioso.
- Violento.

**Definido (rev. 31):** no banco padrão, o temperamento é **sorteado com
peso**: Normal é o mais comum e Violento o mais raro. **Provisório:** os
pesos exatos ficam configuráveis e são propostos junto do gerador.
**Definido (rev. 32):** Normal 40%, Calmo 20%, Educado 20%, Ansioso 13% e
Violento 7% (`ParametrosBancoPadrao`).

### 5.3 Jogadores com asterisco

**Definido (rev. 31) — banco padrão:** cada clube tem de **0 a 3**
jogadores com asterisco, com mais chance nos clubes mais fortes.

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

**Definido (rev. 17):**
- Defensor, meia ou atacante com asterisco tem **mais chance de gol nas
  finalizações**.
- Goleiro com asterisco tem **mais chance de defender pênaltis**.
- **Definido (rev. 18):**
  - Goleiro com asterisco: a conversão dos pênaltis contra ele cai de 75%
    para **65%**.
  - Jogador de linha com asterisco também tem **mais chance de ser
    escolhido finalizador**.
  - Até 3 asteriscos em campo, sem penalidade. **Mais de 3** gera uma
    penalidade que deve quase equilibrar os bônus dos asteriscos extras.
- **Definido (rev. 19), valores provisórios:**
  - Jogador de linha com asterisco tem **15% a mais** de chance de gol em
    **todas** as finalizações, inclusive no pênalti que ele bate.
  - Na escolha do finalizador, o asterisco conta como força **1,5 vez**
    maior.
  - A partir do **4º asterisco em campo**, cada asterisco a mais tira
    domínio do meio-campo, ajustado por simulação para quase equilibrar o
    bônus dos asteriscos extras. Proposta inicial: 5%. Calibrado: **1%**,
    pois 5% fazia cada asterisco extra custar cerca de 0,03 ponto por jogo
    (`docs/simulacoes/calibracao-motor.md`).
  - Os efeitos se combinam: um batedor com asterisco contra um goleiro com
    asterisco converte 75% × 1,15 × (65/75).

### 5.3.1 Sorte e azar da temporada

**Definido (rev. 17):**
- No início de cada temporada, o sistema sorteia **dois clubes**: um
  **beneficiado** e um **prejudicado**, válidos só naquela temporada.
- Representa o fator imprevisível: um clube grande que joga mal num ano e
  um pequeno que joga bem. Qualquer clube pode ser sorteado, inclusive um
  forte para o benefício.
- Beneficiado: **mais chances de gol** por partida. Prejudicado: **menos
  domínio (posse de bola)**.
- Efeito pequeno: cerca de **5 pontos percentuais a mais de vitórias**
  para o beneficiado (por exemplo, de 45% para 50% contra um adversário de
  mesma força) e **5 pontos percentuais a mais de derrotas** para o
  prejudicado, a confirmar por simulação.
- **Definido (rev. 18):**
  - O sorteio é **por divisão**: cada divisão tem um beneficiado e um
    prejudicado.
  - **Qualquer clube** pode ser sorteado, inclusive clubes de usuários.
  - O sorteio é **oculto**: ninguém vê quem foi sorteado.
- **Provisório (rev. 18, calibrado por simulação):** o beneficiado ganha
  uma chance extra de finalização por minuto com 9,5% da chance normal; o
  prejudicado tem o domínio do meio-campo multiplicado por 0,83. Medido:
  cerca de +5 p.p. de vitórias e +5 p.p. de derrotas
  (`docs/simulacoes/calibracao-motor.md`).

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

~~**Em aberto:** definir se e quando titulares perdem força, quais
jogadores evoluem e quais são os limites por temporada.~~ **Respondido na
rev. 54:**

**Definido (rev. 54), pelo usuário (lembrança do Elifoot 98, "nunca
reparei ao certo"; pediu para analisar e simular):**
- **Quem jogou** (os 11 e quem entrou) não cresce: na vitória, poucos
  perdem 1 de força (cerca de 1); no empate, uns 3; na derrota, uns 5.
- **Quem não jogou** cresce: o reserva que não entrou e quem nem foi
  escalado. Na vitória às vezes crescem (cerca de 2 pontos); no empate,
  mais devagar; na derrota, quase nada.

**Definido (rev. 55), correção do usuário:** cada jogador perde **no
máximo 1 ponto por partida**; o resultado decide **quantos** jogadores
perdem: 1 na vitória, 3 no empate e 5 na derrota. ("3" eram 3 jogadores, não
3 pontos.)

~~Proposta da rev. 54 (chances individuais de crescer de 37,5%, 20% e
3,75%).~~ **Substituída na rev. 55.**

**Provisório (rev. 55) — proposta do agente, aguardando aprovação**
(`temporada.EvolucaoForca`, valores em `temporada.ParametrosEvolucao`,
simulação em `docs/simulacoes/evolucao-forca.md`, 6 ligas × 3 sementes × 6
temporadas com promoção e rebaixamento, só máquina):
- Depois de cada partida, entre os que jogaram (os 11 e quem entrou),
  **1, 3 ou 5 sorteados** perdem 1 ponto (vitória, empate, derrota).
- Entre os que não jogaram (reserva que não entrou e quem ficou fora),
  **4, 2 ou 0,5 sorteados** ganham 1 ponto (0,5 = 50% de chance de um), sem
  passar do teto.
- **Piso:** o desgaste não leva abaixo da **força do clube − 5** (como o
  "nível base do clube" − 5 do Elifoot 98, `docs/referencia-elifoot98.md`
  §5).
- **Acima do teto** da divisão ninguém cresce, e cada um perde 1 com 25% de
  chance por partida (se não perdeu pelo resultado).
- **Por que esses números:**
  - Leitura literal (crescem 2, 1 e 0) sem piso: as divisões derretem (a 2ª
    vai de 26 a 8,6 de força em campo em 6 temporadas, a 3ª chega a 2,8).
  - Literal com piso: fica estável, mas todos descem cerca de 5 pontos
    (até o piso) e quase ninguém chega ao teto.
  - Crescendo 5, 3 e 1: a força se mantém, mas um terço dos jogadores
    encosta no teto em 6 temporadas.
  - **Proposta (4, 2 e 0,5, com piso):** cada divisão cai 3 a 4 pontos na
    1ª temporada (os titulares se desgastam e os reservas sobem) e depois
    fica estável; a distância entre o mais fraco e o mais forte se mantém e
    7 a 12% dos jogadores ficam no teto.
- **Ainda não gravado:** o salário segue a força atual; com a evolução, o
  pedido de aumento (§9.5) passa a ter efeito.

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

**Definido (rev. 15): correção leve.** O usuário pediu para melhorar, mas
não muito, porque o Elifoot 98 é sobre assumir times e ser demitido.
- A cada jogo, a moral volta (18 − atual) ÷ 12 em direção a 18, com divisão
  inteira.
- No cenário realista, a faixa vermelha no fim da temporada cai de 36,5%
  para 22%. 76% dos rebaixados continuam no vermelho.
- **Provisório:** o divisor 12 pode ser recalibrado quando a regra de
  demissão for definida (§5.4).

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
- **Definido (rev. 23):** os dois amarelos da mesma partida **contam no
  acumulado**, além da expulsão. Exemplo: com 4 amarelos, o jogador leva
  dois amarelos (vermelho) no jogo; fica com 6, cumpre 1 jogo pelo
  acumulado e o contador recomeça em **1**; cumpre mais 1 jogo pela
  expulsão.

**Definido (rev. 22), pesos provisórios:** vermelho direto suspende **1
jogo em 70%** dos casos, **2 jogos em 25%** e **3 jogos em 5%**.
Configuráveis e validados por simulação.

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
- **Definido (rev. 22) — escalação da máquina:** todo clube sem usuário é
  escalado pela máquina, com a tática de acordo com o plantel.
  - Entre as 8 formações, escolhe a de **maior soma de força** dos
    titulares, usando os melhores jogadores disponíveis de cada posição
    (suspensos ficam de fora).
  - No máximo **3 asteriscos** em campo, para não sofrer a penalidade; o
    asterisco temporário do bônus de artilharia conta.
  - Banco de 7: um goleiro reserva, se houver, e os mais fortes restantes.
  - **Elenco curto:** se nenhuma formação puder ser completada, a máquina
    improvisa. Sem goleiro, o defensor mais fraco (ou, sem defensor, o
    jogador de linha mais fraco) vai para o gol com metade da força, como
    no goleiro expulso. Faltando jogador de linha, o mais forte que sobrou
    de outra posição completa o setor.
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
-   ~~**Em aberto:** os valores.~~ **Definido (rev. 45), pelo usuário —
    prêmio da liga:**
    - **Campeão:** o preço de **2 jogadores de força 50 com asterisco**
      (R$ 3.968.000 com a tabela de preço atual).
    - **2º:** 30% menos que o campeão (70%). **3º:** 10% do prêmio do
      campeão. Do 4º em diante, nada.
    - **Definido (rev. 46), pelo usuário:** a força dos jogadores do prêmio
      é o **teto de força da divisão** (1ª 50, 2ª 30, 3ª 22 no banco
      padrão): 2ª ≈ R$ 988 mil, 3ª ≈ R$ 566 mil. O prêmio do campeão fica
      perto de um quarto da bilheteria da temporada nas três divisões. Simulado em `docs/simulacoes/mercado-maquina.md`; valores
      em `economia.ParametrosPremiacao`, regra em `economia.Premiacao`.
    - **Gravado (rev. 48):** o prêmio é lançado no extrato na última rodada
      da temporada, no fim da temporada, antes da virada.


### 8.1.1 Ano da temporada e histórico

**Definido (rev. 40):**
- Cada temporada tem um **ano**: o jogo começa em **2026** (configurável) e
  cada temporada nova soma um ano. As telas mostram "Temporada 2026".
- O jogo guarda o **histórico de campeões** por ano. Não haverá histórico
  de artilheiros por ano.
- **Ranking de treinadores:** quantos títulos cada treinador tem (só a
  quantidade).
- **Definido (rev. 42):** **todo clube tem treinador** (da máquina ou
  usuário) e o ranking inclui **todos** os treinadores que ganharam
  título. Hoje só os usuários existem como treinadores; é preciso criar
  os treinadores da máquina (§5.4) antes do ranking.
- **Definido (rev. 54), modelo 8a aprovado:** aba **Histórico** com os
  campeões por ano (uma coluna por divisão, clube e treinador) e o ranking
  de treinadores (títulos da 1ª, empatados dividem a posição; só quem já
  ganhou).
- **Definido (rev. 54), pelo usuário:** **um treinador por clube**,
  obrigatório ao criar ou editar um clube. Proposta do agente: o banco
  padrão gera um nome fictício para cada um; o treinador da máquina fica no
  clube até existir a regra de demissão (§5.4).
- **Gravado (rev. 54, V23, `docs/decisions/0013`):** treinador obrigatório
  no clube do molde (nome e sobrenome gerados no banco padrão), copiado para
  o jogo; o campeão guarda o usuário ou o treinador da máquina; aba
  Histórico pelo modelo 8a.
- **Definido (rev. 46), pelo usuário:** conta **1 título** quando o
  treinador ganha a liga da **1ª divisão**. Títulos da 2ª divisão e
  abaixo não contam. Copas: decidir quando existirem. (O histórico de
  campeões continua guardando todas as divisões.)
- **Definido (rev. 42):** a aba "Artilheiros de todos os tempos" continua.
  Se as abas ficarem muitas, agrupar assuntos parecidos.

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

**Definido (rev. 18) — ordem dos jogos:**
- O calendário é só uma **ordem** de jogos, sem datas. Não existe fadiga.
- As **finais** da copa nacional e da copa continental são disputadas
  **depois do último jogo da liga**, em partida única.
- O **Mundial de Clubes** é disputado depois da liga e depois dessas
  finais.
- **Em aberto:** se a copa começa junto com a liga e como as rodadas de
  copa se intercalam com as da liga.

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
- **Definido (rev. 21):** o plantel mostra se o jogador é estrangeiro e se
  conta no limite (§10.3).

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

**Definido (rev. 38) — valores iniciais do caixa da rodada**, propostos
pelo agente (rev. 37) e aprovados pelo usuário como ponto de partida; os
números continuam em `economia.ParametrosEconomia` e podem ser
reajustados por nova simulação (`docs/simulacoes/economia-caixa.md`):
- **Estádio inicial:** 5.000 lugares até a força 12 e 80.000 na força 50,
  crescendo na mesma proporção (cerca de 7,6%) a cada ponto de força, em
  múltiplos de 500.
- **Salário por rodada:** R$ 2.000 na força 17, crescendo 7,2% por ponto
  (cerca de R$ 1.400 na força 12 e R$ 19.800 na força 50).
- **Ingresso:** R$ 40 em todas as divisões. Com preços diferentes por
  divisão, a simulação deixava uma divisão rica e outra quebrada.
- **Público:** 45% do estádio com moral inicial e adversário de mesma
  força; a moral máxima soma 20 pontos e a moral zero tira 30; adversário
  20 pontos mais forte soma 15; sorteio de ±8; entre 10% e 100% da
  capacidade.
- **Despesas de jogo:** 15% da bilheteria no jogo em casa; deslocamento de
  R$ 60.000 (1ª), R$ 30.000 (2ª) e R$ 15.000 (3ª) no jogo fora.
- **Saldo inicial:** 10 rodadas da folha do clube.
- **Resultado na simulação (só o caixa, sem patrocínio nem premiação):**
  dentro de cada divisão os mais fortes lucram mais; os mais fracos da 2ª
  e da 3ª fecham a temporada levemente no vermelho; a 1ª é a mais
  lucrativa.
- **Lançamento (rev. 38):** o caixa é lançado no avanço da rodada, na
  mesma transação, com extrato por clube, rodada e tipo (bilheteria,
  custo do jogo, deslocamento, salários) que não aceita repetição. O
  público de cada jogo é gravado na partida e sorteado pela semente
  dela. Estádio e saldo inicial são calculados no primeiro avanço
  (também em jogos salvos antes da economia).
- **Provisório (rev. 38):** clubes da fila de espera não jogam e não têm
  lançamentos; numa divisão com menos rodadas que a maior, a rodada sem
  jogo também não tem lançamentos (como na simulação).

### 9.1.1 Moedas

**Definido:**
- O jogo terá três moedas: **Real**, **Dólar** e **Euro**.
- As moedas seguem uma proporção fixa, configurável: **1 Dólar = 5 Reais**
  e **1 Euro = 6 Reais**.

**Definido (rev. 37):**
- A moeda exibida segue o **país da liga**: Brasil em Real; Inglaterra,
  Espanha, Itália e Portugal em Euro; Argentina em Dólar (não há peso nem
  libra entre as três moedas).
- Os valores são guardados como **inteiros em centavos de Real** e
  convertidos só na exibição.

### 9.2 Estádios

-   Cada clube tem capacidade de estádio. **Definido (rev. 39):** o
    estádio não tem nome próprio por clube; as telas usam o nome geral
    "Estádio".
-   A capacidade pode ser ampliada mediante custo.
-   **Definido (rev. 54), pelo usuário:** o limite para construir é
    **120.000 lugares para todos**, mas nenhum clube começa com esse
    tamanho, nem os de força 50 (hoje o inicial vai até 80.000). Nas
    finanças, clicar no quadro do estádio abre uma janela com a capacidade
    atual, o preço do ingresso e o preço para **aumentar 5.000 lugares**,
    que vale o **preço de um jogador de força 50**. O ingresso fica **fixo
    por enquanto**, com valores por clube e por divisão definidos pelo
    agente (a simular; na rev. 37, preços diferentes por divisão deixavam
    uma divisão rica e outra quebrada).
-   **Definido (rev. 55):** modelo **6c** aprovado (janela do estádio com
    capacidade, limite de 120.000, ingresso de R$ 40 e "Ampliar 5.000
    lugares" pelo preço de um jogador de força 50; desligado sem caixa, no
    limite ou com rodada ao vivo).
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

**Definido (rev. 43), pelo usuário:** sem empréstimo, sempre que o caixa
ficar **negativo** um jogador do clube vai a **leilão sem lance mínimo**.
A máquina oferece um pouco abaixo do preço padrão (§9.4).

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

**Definido (rev. 40), pelo usuário:**
- **Preço do jogador** vem de uma tabela pela força ("um jogador de força
  X custa Y"). A tabela será proposta pelo agente e validada por simulação,
  como os salários.
- **Compra direta:** o usuário compra do clube pelo preço da tabela; é o
  caminho mais comum para o usuário.
- **Leilão:** começa no preço da tabela **com 20% de desconto**. Cada
  participante (usuário e máquina) coloca **um preço**; quem oferecer
  mais leva. A **máquina participa** e deve ser a compradora mais comum.
- **Provisório (rev. 41) — tabela de preço, proposta pelo agente e
  aguardando aprovação:** preço da compra direta = **80 rodadas do salário**
  do jogador (cerca de duas temporadas), arredondado em R$ 1.000; leilão
  começa 20% abaixo. Força 17 custa R$ 160 mil, força 40 R$ 792 mil, força
  50 R$ 1,59 milhão. Na simulação (`docs/simulacoes/economia-caixa.md`), o
  saldo inicial compra cerca de 2,4 jogadores da força do próprio clube em
  todas as divisões, e o resultado de uma temporada compra de 0 (os mais
  fracos da 2ª e da 3ª) a 5 (os mais fortes da 1ª). Valores em
  `economia.ParametrosMercado`.
- **Definido (rev. 41) — momento do leilão:** o leilão é **na hora**, sem
  esperar rodadas.
  - **Jogador posto à venda pela máquina:** os leilões acontecem depois
    que o usuário clica no OK da tática (Confirmar) e antes de começar o
    minuto a minuto.
  - **Jogador posto à venda pelo usuário** ("vender por leilão"): resolve
    no momento; a máquina pode comprar e, em rede (depois), os outros
    usuários podem ofertar.
- **Definido (rev. 42):**
  - Os usuários dão seus lances e confirmam (OK); **depois** a máquina, se
    quiser, dá o lance dela. Leva quem ofereceu mais.
  - **Empate:** decidido por sorteio.
  - **Ninguém deu lance:** o jogador não é vendido e só pode ser posto à
    venda de novo (pela máquina ou pelo usuário) depois que aquela rodada
    terminar.
  - O usuário **não pode dar lance** se o lance inicial for maior que o
    caixa dele; nesse caso não é preciso esperar por ele, e a tela já
    mostra o resultado (por exemplo, que outro treinador levou).
  - ~~**Estrangeiro:** 10% mais barato que o nacional da mesma força.~~
    **Substituído na rev. 43.**
- **Definido (rev. 43), pelo usuário:**
  - **Estrangeiro:** **25% mais barato** que o nacional da mesma força (no
    Elifoot 98, comprar estrangeiros baratos é a tática dos times fracos).
  - **Asterisco:** **25% mais caro** na compra direta. No leilão o lance
    inicial continua 20% abaixo do preço (já com o acréscimo).
  - A **máquina só compra por leilão**; a compra direta é do usuário.
  - **Definido (rev. 51), modelo 7a aprovado:** ao clicar num clube na
    classificação, a janela tem as abas **Calendário** e **Plantel**. O
    plantel mostra **todos** os jogadores, com os **gols na temporada**
    (pedido do usuário, para despertar interesse), força, temperamento e
    sigla do estrangeiro; os anunciados ficam destacados com "à venda",
    preço e botão **Comprar** (ou "Caixa insuficiente" / "Plantel
    cheio"). O caixa do usuário aparece no topo. No próprio clube não há
    botão.
  - **Gravado (rev. 49):** a compra direta confere no servidor que o
    jogador está anunciado num clube da máquina, que o plantel do
    comprador tem menos de 25, que o caixa paga o preço e que não há
    rodada ao vivo. O jogador muda de clube, sai do anúncio e entra fora
    do banco; comprador e vendedor ganham lançamentos de compra e venda
    na última rodada processada (0 antes da primeira).
  - **Finanças importam:** clube forte que perde fica com estádio mais
    vazio e precisa vender os jogadores caros; clube fraco que sobe ganha
    caixa para comprar. Com o tempo o clube tende à força média do que o
    caixa sustenta (ex.: vende um de força 44 numa fase ruim e, recuperado,
    compra um de força 20).
  - **Compra e venda da máquina pelo saldo:** o **limite** é **10 rodadas
    da folha** do clube. Saldo acima do limite: a máquina pode comprar
    (até o máximo de **25 jogadores**; o banco padrão cadastra 23). Saldo
    abaixo: precisa vender. Clubes ricos ficam quase sempre com o plantel
    cheio; uma sequência de derrotas derruba a bilheteria e o clube vende
    um, depois outro, e volta a comprar quando se recupera.
  - A máquina **sempre renova** os contratos dos seus jogadores e deixa
    sempre **5 ou 6 jogadores à venda** (compra direta pelo usuário),
    **sorteados** sem olhar a posição.
    - **Gravado (rev. 47):** os anúncios são completados no início de cada
      temporada e depois de cada rodada; quem tem menos de 5 à venda sorteia
      até ter 5 ou 6. Proposta do agente: o sorteio não anuncia jogadores
      demais de uma posição, para a venda de todos não deixar o clube abaixo
      do elenco mínimo (§9.3); clubes da fila de espera não anunciam
      (`docs/decisions/0011-mercado-no-banco.md`).
  - **Gravado (rev. 52; docs/decisions/0012):** ao confirmar a tática, os
    clubes da máquina abaixo do limite põem 1 jogador em leilão cada. Se o
    caixa do usuário alcança algum lance inicial (e o plantel tem menos de
    25), a tela "Seus lances" espera os lances dele (em branco = sem lance;
    "Não quero nenhum"); senão os leilões são resolvidos na hora. Depois a
    máquina dá os lances e a tela mostra o resultado, com "Ir para a
    rodada". Os lances do usuário que valem: pelo menos o lance inicial e
    no máximo o caixa que sobrou dos leilões que ele já levou. Cada leilão é
    aberto e resolvido uma vez só. **Proposta do agente:** se o usuário
    comprar direto um jogador que está em leilão aberto, o leilão é
    cancelado.
  - **Definido (rev. 53), pelo usuário — venda do usuário por leilão:**
    - Ao pôr um jogador à venda, **o usuário define o lance mínimo**. O
      resultado aparece na mesma tela do modelo 7b.
    - **Caixa negativo:** o **jogo escolhe** o jogador que vai a leilão sem
      lance mínimo, entre os que estão **sem contrato renovado**. Se todos
      estiverem renovados, ninguém é vendido; quando virar a temporada,
      logo no início e **antes de qualquer coisa**, um jogador é vendido.
  - **Gravado (rev. 53, V21):** "Vender por leilão" na ficha do jogador
    aberta pelo plantel (não pelos artilheiros), com o campo do lance
    mínimo (0 = sem mínimo) e o preço de tabela como referência. O leilão é
    resolvido na hora com os lances da máquina e a tela mostra o resultado
    com "Voltar ao plantel". Recusado com rodada ao vivo, temporada
    terminada, mínimo negativo ou jogador que já foi a leilão antes da
    mesma rodada. Essas vendas não contam como os leilões da rodada (não
    impedem que eles abram). **Propostas do agente:** a venda respeita o
    elenco mínimo do §9.3, como a da máquina; o clube da máquina que já
    comprou antes da rodada (num leilão ou numa venda do usuário) não dá
    lance de novo (1 compra por rodada); o jogador comprado de um usuário
    não vai a leilão de novo antes da mesma rodada.
  - **Definido (rev. 53):** com caixa negativo, o jogador é **sorteado**
    entre os sem contrato renovado, para que o clube possa perder qualquer
    um deles.
  - **Gravado (rev. 53, V22):** `jogo_jogador.contrato_temporada` guarda a
    temporada em que o contrato foi renovado; por enquanto só a compra (direta
    ou por leilão) renova. Ao confirmar a tática, o clube do usuário com
    caixa abaixo de zero põe 1 jogador sorteado em leilão sem lance mínimo,
    junto com os da máquina, na tela do 7b ("Seu jogador": ele não dá lance
    no próprio). Na virada da temporada, depois de gravar a nova e antes do
    patrocínio, o mesmo leilão acontece na rodada 0, com todos sem contrato,
    e o aviso do início da temporada diz quem foi vendido e por quanto (ou
    que ninguém deu lance). **Proposta do agente:** o sorteio respeita o
    elenco mínimo do §9.3 (asterisco pode sair).
  - **Lance da máquina:** um pouco **abaixo do preço padrão**; um clube da
    máquina com preferência pelo jogador pode dar um pouco mais, de acordo
    com a força do time.
- **Provisório (rev. 43):** a tabela de preço (80 rodadas de salário) segue
  como proposta; o usuário ainda não tem noção do valor. Será recalibrada
  pela simulação de temporadas com o mercado da máquina, para que as
  finanças produzam o ciclo acima.
- **Definido (rev. 45; proposta do agente na rev. 44, aprovada pelo usuário),**
  simulada em `docs/simulacoes/mercado-maquina.md` (valores em
  `economia.ParametrosMercadoMaquina`, regra em `economia.MercadoDaMaquina`):
  - **Lance:** 10% abaixo do preço, mais até 5% proporcional à força do
    clube (a "preferência pela força do time"); leva o maior.
  - **Quem vende:** o clube abaixo do limite põe em leilão **1 jogador por
    rodada**, o mais caro sem asterisco cuja saída não deixa a posição
    abaixo do elenco mínimo do §9.3 (1 goleiro, 4 defensores, 4 meias, 2
    atacantes).
  - **Quem compra:** no máximo **1 compra por rodada**; só dá lance em
    jogador com força de pelo menos a **força em campo do clube menos 5**
    (sem isso, os clubes ricos da 1ª compravam qualquer jogador e a 3ª
    nunca comprava); e só se, pago o lance, sobrar o limite **mais 5
    rodadas da folha** (sem essa folga, 35% das compras eram seguidas de
    venda em até 3 rodadas; com ela, 0%).
  - **Resultado:** o terço de cima da tabela compra (0,6 a 1,9 por
    temporada) e o de baixo vende (1,0 a 2,3); a força em campo do fundo
    cai cerca de 1 ponto na temporada.

### 9.5 Contratos, salários e empréstimos

-   Jogadores possuem contratos com prazo e salário.
-   O jogo deve emitir notificações visuais quando o contrato estiver
    próximo do vencimento.
-   Jogadores sem contrato renovado podem solicitar aumento conforme sua
    força.
-   Se o clube não aceitar a negociação, o jogador poderá ser colocado
    em leilão, de acordo com regras a definir.
-   Empréstimos e juros devem existir.

**Definido (rev. 40), pelo usuário:**
- **Contrato em temporadas:** ao comprar ou renovar, o contrato vale um
  número de temporadas e acaba quando começa a temporada seguinte à
  última. Comprado na última rodada com contrato de 1 temporada, o
  jogador começa a próxima já sem contrato.
- **Pedido de aumento:** quando a força do jogador sobe acima do que o
  salário paga, ele pede aumento para o salário da força atual. No
  máximo **1 pedido por rodada** no clube, mesmo que vários jogadores
  tenham crescido. Aceitar renova o contrato e o jogador não pode ser
  vendido naquela rodada; recusar põe o jogador em **leilão**, sem o
  usuário definir o preço (preço da tabela com 20% de desconto).
- **Dependência:** hoje a força dos jogadores não muda (§6.1 em aberto) e
  o salário segue a força atual; o pedido de aumento só tem efeito depois
  da evolução de força e do salário gravado no contrato.
- **Retirado (rev. 42):** o usuário decidiu **não ter empréstimo**, para
  simplificar. A regra de blocos de 500 mil a 1% por rodada (rev. 40) fica
  só como registro.
- **Definido (rev. 42):** compra e renovação valem **só até o fim da
  temporada** em curso (sem escolher número de temporadas).
- ~~**Em aberto:** o que acontece no início da temporada seguinte com os
  jogadores cujo contrato acabou e o saldo negativo sem empréstimo.~~
  **Resolvido na rev. 43:**
  - No início de cada temporada **todos ficam sem contrato**, mas isso não
    obriga a renovar: o jogador continua no clube com o salário que tem.
  - Quem tem **salário abaixo da força** pede aumento: **um jogador por
    rodada** no clube (a regra do pedido de aumento acima).
  - A máquina sempre renova (§9.4). O saldo negativo vai para o §9.3.
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

**Definido (rev. 40), pelo usuário:** a cada início de ano aparece uma
caixa com opções de patrocínio, acessível pelo plantel; o usuário escolhe
uma e o dinheiro entra no caixa do clube. As opções dependem da posição
na temporada anterior.
- **Definido (rev. 42):** um patrocínio **por temporada** (substitui os
  contratos de 2 a 4 anos; pode mudar depois). O valor é **proporcional à
  posição na liga na temporada anterior**; na **primeira temporada**, todos
  recebem o mesmo valor.
- **Definido (rev. 43):** o dinheiro entra **de uma vez, no início da
  temporada**, para **todos os clubes** (usuários e máquina).
- **Definido (rev. 54), pelo usuário:** vale a **rev. 40**: no início do
  ano aparece a caixa com opções de patrocínio para o usuário escolher,
  porque cria uma interação. O dinheiro continua entrando de uma vez (rev.
  43); a máquina recebe sem escolher. As opções ainda precisam de proposta.
- **Definido (rev. 55):** modelo **9a** aprovado: três propostas no início
  do ano (fixa à vista pelo valor da tabela; menor à vista mais um valor por
  vitória; menor à vista mais um bônus se subir de divisão). A caixa abre no
  plantel e a rodada 1 só começa depois da escolha. Os valores das propostas
  ainda serão simulados.
- **Definido (rev. 45), pelo usuário:** o patrocínio vale cerca de **10% do
  prêmio do campeão** da divisão.
- **Definido (rev. 46), pelo usuário:** o patrocínio usa o mesmo teto de
  força da divisão; o 1º colocado da temporada anterior recebe os 10%
  cheios, o **2º recebe 10% menos** e cada posição seguinte **8% menos que
  a anterior**, até o último (na 1ª com 20 clubes: R$ 397 mil, R$ 357 mil,
  R$ 329 mil… R$ 80 mil).
- **Provisório (rev. 46), proposta do agente:** na primeira temporada,
  todos recebem a média das posições (R$ 197 mil na 1ª com 20 clubes); o
  valor usa a divisão em que o clube jogou a temporada anterior; quem vem
  da fila de espera recebe como o último da divisão em que entra.
- **Definido (rev. 50), modelo 6b aprovado:** a aba Finanças mostra o
  saldo do fim da temporada anterior (ou o inicial do clube), uma linha
  "Patrocínio da temporada" e a coluna "Mercado e prêmios" (compras com
  sinal de menos, vendas e prêmio da liga) com o jogador embaixo do jogo
  da rodada. Compras antes da 1ª rodada aparecem em "Antes da 1ª rodada".
  Diferença do modelo: a linha do patrocínio ainda não diz a posição da
  temporada anterior.
- **Gravado (rev. 48):** o patrocínio é lançado de uma vez na "rodada 0"
  da temporada (ao criar o jogo e na virada). A tela de finanças ainda não
  tem linha própria para patrocínio e prêmio: eles aparecem no saldo do
  início da temporada (mostrar à parte exige novo modelo da tela). Jogos
  salvos antes da rev. 48 só recebem patrocínio a partir da próxima
  temporada.

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

A tela mostra **todos os jogos de uma divisão simultaneamente**, uma
divisão por vez (§11.2). Enquanto a partida do usuário acontece, as outras
da mesma divisão seguem na mesma tela.

**Definido (rev. 16):**
- O topo da tela lista as divisões da rodada, na ordem em que jogam, com o
  estado de cada uma (terminada, jogando, aguardando).
- O controle de velocidade só aparece quando a divisão em jogo tem clube
  de usuário. Divisão sem usuário roda sempre rápido, sem aviso na tela.
- Expulsão: marca vermelha ao lado do nome do clube. Jogo do usuário em
  destaque.
- Cada clube tem um **escudo simples**: um modelo e duas cores. Modelos:
  metade vertical, metade horizontal, listras verticais, listras
  horizontais, corte diagonal, faixa diagonal, faixa horizontal, quartos,
  cruz e liso com borda.
- **Provisório:** clube sem escudo cadastrado usa liso branco com borda
  preta.
- **Definido (rev. 17):** os indicadores de divisão no topo **não são
  clicáveis**; só mostram em que divisão a rodada está. Para ver outra
  divisão, o usuário usa a classificação no plantel (§10.3).

### 10.3 Plantel e gestão

-   A tela de elenco deve preservar a leitura simples do Elifoot 98, com
    modernizações.
-   Durante o jogo, ao clicar em um clube, o usuário vê os lances e os 11
    em campo de cada time, sem os reservas. **Exceção (rev. 33):** no
    próprio clube aparece também o banco, para a substituição (§10.5).
-   Se o clube for controlado pelo usuário, poderá fazer substituições
    nas condições permitidas.
-   **Definido (rev. 30, forma da janela na rev. 33) — pausa no minuto a
    minuto:** quando a divisão em jogo tem clube de usuário, **clicar em
    qualquer clube pausa a partida** e abre a janela do jogo (§10.5, rev.
    33). Clicar no **próprio clube** abre também as **substituições**. A partida continua quando a janela é
    fechada. Vale para o jogo individual; a pausa em rede segue a §11.3,
    ainda em aberto.
    - Consequência: o servidor não pode calcular a rodada inteira de uma
      vez. A partida avança por minuto e aceita substituições no meio; a
      forma técnica será registrada numa decisão de arquitetura antes da
      tela da rodada.
-   Não permitir ações de gestão que pertençam a outro participante.

**Definido (rev. 16) — tela do plantel:**
- Abas: Plantel, Classificação, Artilheiros da temporada e Artilheiros de
  todos os tempos.
- Colunas do elenco: posição, nome (com ★), força, temperamento, amarelos
  e gols da temporada. Elenco separado em titulares, banco e fora do banco.
- Ao clicar no jogador abre a ficha, com gols na temporada e gols na
  carreira.
- **Substituído na rev. 31 pela sigla (abaixo).** ~~Definido (rev. 21):~~ depois do nome, o plantel mostra a **bandeira**
  do país quando o jogador é estrangeiro no país do clube. Se ele é
  estrangeiro mas **não conta** no limite (ex.: português no Brasil,
  jogador da UE em país da UE), a bandeira aparece **atenuada** (mais
  fosca) ou com outro sinal equivalente. Jogador do próprio país fica sem
  bandeira. A forma exata vai no modelo da tela, para aprovação.
- Classificação por divisão, com o clube do usuário destacado e os
  rebaixados marcados.
- **Definido (rev. 17):** ao clicar num clube da classificação, abre o
  **calendário do clube** com os jogos e resultados, sem detalhes dos
  gols.
- **Definido (rev. 18):** o calendário mostra os jogos que já existem. Os
  da liga existem desde o início da temporada. Os de copa, continentais e
  Mundial aparecem conforme os confrontos são criados (§8.2).
- **Definido (rev. 28):** em jogo em rede, o plantel mostra os
  **treinadores da sessão**, com nome, clube e divisão, e o estado de cada
  um (configurando, pronto, desconectado), como na sala (§3.3).
- **Definido (rev. 34) — montar o time:**
  - Cada linha do plantel tem três botões, **Titular**, **Banco** e
    **Fora**; a linha continua abrindo a ficha do jogador.
  - **Provisório (proposta do agente):** o jogo começa com a escalação da
    máquina para o clube do treinador, e ele muda o que quiser.
  - A escalação fica gravada mesmo incompleta, e a tela diz o que falta (ex.: "faltam 1 defensor"); a rodada só sai com a
    escalação válida.
  - Depois de uma rodada, quem ficou **suspenso sai sozinho** para fora do
    banco, marcado como suspenso, e deixa de ser o batedor. O treinador
    completa o time.
  - Até existir o minuto a minuto (3f.4), "Confirmar e jogar rodada" grava
    a escalação, joga a rodada inteira de uma vez e volta ao plantel com o
    placar do jogo do treinador.
- **Definido (rev. 31) — colunas do elenco:** posição, nome (com ★),
  força, temperamento, **gols** da temporada e **amarelos**, nessa ordem:
  os gols vêm antes porque são mais importantes.
- **Definido (rev. 31) — estrangeiro:** em vez da bandeira, uma **caixa
  com a sigla** do país (ex.: ARG) logo depois do nome, como na escolha do
  campeonato. Estrangeiro que não conta no limite tem a caixa tracejada e
  mais apagada. Jogador do país do clube fica sem caixa. Substitui a
  bandeira da rev. 21 (não exige imagens dos 211 países).
- Contratos, finanças, estádio, copas e competições continentais ganham
  telas próprias nas etapas em que forem implementados, também com modelo
  para aprovação antes.

### 10.4 Configurações

**Definido — velocidade das partidas:** no modo individual, o próprio
usuário escolhe. No jogo em rede, somente o administrador define, e ela
vale para todos.

O jogo deve oferecer: - Velocidade das partidas. - Som dos gols
ligado/desligado. - Sorteio de
copa passo a passo ou rápido. - Configuração do estádio. - Opções de
gestão e exibição compatíveis com o modo individual ou multiplayer.

### 10.5 Modelos de tela aprovados

**Definido (rev. 16):** os modelos de jogos da rodada, escudos, plantel,
ficha do jogador, classificação e artilheiros foram aprovados pelo usuário
no artefato de design (privado, versão 12). Eles orientam a implementação
em Thymeleaf + HTMX.

**Definido (rev. 28):** aprovados no mesmo artefato, com os ajustes do
usuário:
- **Entrar e cadastro:** e-mail e senha; cadastro só pelo link, com nome do
  treinador, e-mail e senha repetida; erros na própria tela.
- **Novo jogo:** campeonatos numa **faixa com rolagem lateral**, com setas,
  porque podem ser muitos; depois o clube. O participante vê os candidatos
  do sorteio e, depois, o clube que recebeu.
- **Quadro de escolha:** o administrador define o modo do jogo; no modo
  escolha, os clubes do país aparecem nas divisões pela força, a fila de
  espera não pode ser escolhida e o clique no clube mostra o plantel.

**Definido (rev. 33) — clique no clube durante a rodada:**
- **Quadro da rodada:** a coluna da direita mostra o **último lance** do
  jogo, com ícone (gol, amarelo ou vermelho) e "nome minuto'". Se houve
  gol e depois amarelo, aparece o amarelo; os gols ficam na janela.
- **Janela pequena sobre o quadro**, como no Elifoot 98, e não uma tela
  cheia. O quadro fica escurecido atrás.
  - **Cabeçalho:** minuto, estádio, público e "partida pausada"; abaixo,
    mandante (nome e escudo) à esquerda, placar no centro e visitante à
    direita, alinhados com os lances. Marca de expulsão ao lado do clube.
  - **Lances** em ordem: minuto e ícone (gol, amarelo, vermelho e, quando
    houver, lesão) no **centro**; o nome do jogador do lado do clube dele
    (mandante à esquerda, visitante à direita). Pênalti como "(pên.)".
  - **Próprio clube:** abaixo dos lances, os 11 em campo e o banco; o
    usuário marca quem sai e quem entra, vê a formação resultante e clica
    em **Substituir**. Mostra as substituições usadas (ex.: 1 de 5) e as
    já feitas. Expulso não aparece para troca.
  - **Clube da máquina:** a mesma janela, sem substituição, com uma
    **prévia simples** dos 11 de cada lado (formação, posição, nome e
    força), mandante à esquerda e visitante à direita.
  - Botão **Continuar** (ou fechar) retoma a partida.
- **Em aberto:** caixas do Elifoot 98 "Cartões vermelhos" e "Lesões"
  (provavelmente pausa automática nesses lances); árbitro não existe no
  domínio e fica fora.
- Convite de outro clube ao usuário: modelo depois da regra de moral
  (§6.2) e demissão (§5.4).

**Definido (rev. 36) — calendário do clube:** janela sobre a classificação,
aberta ao clicar no clube: cabeçalho com escudo, temporada, divisão e
posição; resumo de jogos, vitórias, empates, derrotas e gols; uma linha
por rodada com local (casa ou fora), adversário com escudo e resultado com
os gols do clube primeiro e a marca V, E ou D colorida; o próximo jogo em
destaque; folgas marcadas; jogos futuros com "—".

**Definido (rev. 39) — finanças do clube (modelo 6a):** aba **Finanças**
depois dos artilheiros, só para o clube do usuário (as finanças dos clubes
da máquina e dos outros treinadores não são visíveis). Cabeçalho com
escudo, temporada, divisão, última rodada e moeda; quadros de saldo (com o
saldo do início da temporada), resultado da temporada (receitas e
despesas), estádio (lugares, público médio, ocupação e ingresso) e folha
por rodada; extrato da temporada atual com o saldo inicial, uma linha por
rodada (casa ou fora, adversário com escudo, público, bilheteria, custo do
jogo, deslocamento, salários, resultado e saldo) e o total. Saídas com
sinal de menos. O **plantel** mostra o **caixa** (saldo) no cabeçalho,
para o treinador saber se pode contratar; o valor leva à aba Finanças.
- **Público na rodada (rev. 39):** o quadro da rodada mostra o público de
  cada jogo na primeira coluna (modelo 1a) e a janela do jogo mostra
  "Estádio · público" no cabeçalho. É o mesmo público que o avanço grava:
  sorteado pela semente da partida, com a moral de antes da rodada.
- **Depois, com o mercado (§9.4):** clicar num clube na classificação
  poderá mostrar os jogadores postos à venda; sem preço definido não há
  oferta.

**Definido (rev. 29):**
- **Início**, logo depois de entrar, com **só duas opções**: "Novo jogo" e
  "Voltar a um jogo gravado" (lista com clube, país, divisão, temporada,
  rodada e último acesso).
- **Novo jogo do administrador:** depois do campeonato, dois caminhos:
  "Sortear clube e começar" ou "Escolher clube". O do participante segue
  só com o sorteio.
- O quadro passa a se chamar **quadro de escolha de clube**, sem botão para
  refazer o sorteio dos empates.

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
Na tela, a divisão sem usuário é exibida sempre em velocidade rápida
(rev. 16, §10.2); o cálculo continua instantâneo no servidor.

**Definido (rev. 14):**
- Usa-se o **mesmo motor em modo rápido** (cerca de 0,1 ms por partida;
  uma temporada de 380 jogos leva cerca de 40 ms). Não há motor separado.
- **Fluxo da rodada:**
  1. Todos os treinadores definem a tática e confirmam.
  2. Abre-se a tela de jogos da rodada, que exibe uma divisão por vez (§10.2).
  3. Ordem de processamento: começa pela divisão de **maior número de
     prioridade** (a mais inferior) e sobe até a 1ª.
     - Divisões **sem usuário** saem instantaneamente, com todos os jogos
       simultâneos.
     - Divisões **com usuário** rodam minuto a minuto, na velocidade
       configurada.
  4. Se o país tiver uma única divisão, todos os jogos da rodada (do
     usuário e da máquina) aparecem nessa tela.

**Definido (rev. 15) — exibição da rodada:**
- As divisões são exibidas **uma depois da outra**, nunca ao mesmo tempo:
  primeiro a mais inferior (por exemplo, a 3ª), depois a 2ª, e por último
  a 1ª.
- Quando uma divisão termina, a seguinte começa.
- Divisão **com usuário:** roda na velocidade configurada.
- Divisão **sem usuário:** roda **sempre rápido**, sem opção de
  velocidade (decidido no modelo de tela, rev. 16).
- Ao terminar a 1ª divisão, a tela de jogos fecha e o usuário volta à tela
  do plantel.

**Definido:** o processamento segue a prioridade em ordem **decrescente**.
As divisões de número maior vêm primeiro (por exemplo, 3, depois 2) e a
liga principal (1) por último. Com uma única liga, basta a prioridade 1.

**Definido (rev. 35) — rodada ao vivo (`docs/decisions/0010`):**
- O servidor anda o relógio pelo tempo passado e refaz cada partida pela
  semente e pelas substituições gravadas. A tela consulta a cada 1 s
  (provisório).
- **Velocidades da divisão com usuário (provisórias):** lenta (1 s por
  minuto de jogo), normal (0,5 s) e rápida (0,2 s). Começa em normal; a
  escolha fica gravada para as próximas rodadas.
- **Divisão sem usuário:** cerca de 5 s na tela (0,05 s por minuto).
- **Jogo individual:** sair da tela no meio da rodada **não pausa**. A
  partida segue no servidor, e ao voltar a tela mostra o minuto atual ou o
  resultado.
- Enquanto a rodada está ao vivo, a escalação não muda pelo plantel; as
  trocas são feitas pela substituição.

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
- Lista de países equiparados a nacionais (§8.4).

Conflito da rev. 18 (bônus de artilharia × asterisco) **resolvido na
rev. 19**: o bônus soma ao asterisco e vale para qualquer defensor (§5.2).

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
- **3a (concluída):** escudo do clube e artilharia (gols na temporada e na
  carreira, rankings, artilheiros empatados), só no domínio. Antes dela
  entraram a sorte e o azar da temporada, os efeitos do asterisco e o
  sorteio do bônus de artilharia.
- Migrações e repositórios para o estado da temporada.
  - **3b.1 (concluída):** molde com países, liga nacional, divisões e
    clubes com escudo (migração V2, `docs/decisions/0006`).
  - **3b.2 (concluída):** jogadores no molde com posição, força,
    temperamento, asterisco permanente e nacionalidade (código do país,
    sem as regras de estrangeiros da §8.4); plantel de até 25 (migração V3).
  - **Catálogo de países (rev. 21):** 211 membros da FIFA (migração V4).
- Jogo em andamento (tabelas `jogo_*`), recorte aprovado em 2026-10-27:
  - **3c.1 (concluída):** criar o jogo copiando a liga e os planteis do
    país escolhido no molde, depois de validar a liga (migração V5). Os
    outros países entram na Etapa 6; o jogo fica sem dono até as contas.
  - **3c.2 (concluída):** temporada e sorteios do início gravados (migração
    V6): calendário, sorte e azar por divisão e bônus de artilharia, todos
    de uma semente, que também é a do desempate da classificação. Por
    enquanto só a 1ª temporada é iniciada.
  - **3c.3:** avanço de rodada que nunca processa a mesma rodada duas
    vezes, com semente. **Definido (rev. 22):** a rodada grava placares e
    gols no calendário, cartões amarelos e vermelhos, suspensões e moral; a
    classificação é recalculada dos resultados. Dividida em 3c.3a
    (escalação da máquina e disciplina, só domínio) e 3c.3b (gravação e
    avanço), **ambas concluídas** (migração V7). Quem pede o avanço informa
    a rodada esperada; pedido repetido ou fora de ordem é recusado.
  - **3c.4 (concluída):** fim de temporada gravado: exige todas as rodadas
    jogadas, aplica promoção, rebaixamento e fila pela classificação final,
    zera os amarelos (suspensões continuam) e inicia a temporada seguinte.
    Moral e força dos jogadores não mudam na virada, até decisão própria.
- Avanço de rodada idempotente.
- Script de administrador, link de cadastro e papéis (§3.4).
  - **3d.1 (concluída):** contas no servidor (migração V8,
    `docs/decisions/0007`): script do administrador, link de uso único,
    cadastro de participante, verificação de senha e redefinição pelo
    administrador. Login com sessão e telas entram com o Spring Security,
    junto das telas.
  - **3e.1 (concluída):** formato da liga e clubes do país (migração V10,
    rev. 27): divisões com prioridade automática e exclusão que sobe as de
    baixo, promovidos calculados, campeonatos disponíveis e distribuição dos
    clubes pela força, com empate sorteado, ao criar o jogo. A tela de
    escolha entra com as telas (mockup antes).
  - **3d.2 (concluída):** dono do jogo e clube inicial (migração V9): o
    jogo nasce com dono e com o treinador humano no clube sorteado entre os
    mais fracos da última divisão (grupo configurável em
    `managerfoot.jogo.candidatos-clube-inicial`) ou escolhido pelo
    administrador. A demissão do usuário rebaixado para a fila (§4.1) passou
    a ser aplicada na rev. 36: ele assume um clube sorteado da última
    divisão.
- **Banco padrão (concluído, rev. 32):** 6 países com formatos adaptados
  (`docs/banco-padrao-formatos.md`), 23 jogadores nacionais por clube, força
  do clube igual à média dos 11 em campo; gerado por código e carregado com
  `--carregar-banco-padrao` (`docs/decisions/0008`).
- Telas Thymeleaf + HTMX:
  - **3f.1 (concluída):** entrar, cadastro pelo link e sair, com sessão e
    CSRF (`docs/decisions/0009`); link de cadastro gerado pelo terminal até
    existir a tela do administrador; início provisório.
  - **3f.2a (concluída):** início com "Novo jogo" e "Voltar a um jogo
    gravado" (último acesso por treinador, migração V11); novo jogo por
    sorteio para todos, já com a 1ª temporada iniciada; tela do clube
    sorteado com os candidatos. Um usuário só abre os próprios jogos.
  - **3f.2b (concluída):** novo jogo do administrador com "Sortear clube e
    começar" ou "Escolher clube"; quadro de escolha com as divisões pela
    força, fila bloqueada, candidatos do sorteio destacados e plantel do
    clube clicado; "Assumir" cria o jogo com a 1ª temporada. A semente do
    quadro fica na sessão e é consumida ao assumir (clique duplo não cria
    dois jogos). Frases sobre rede do modelo ficaram fora até a Etapa 5.
  - **3f.3a (concluída):** escalação do treinador gravada (migração V12),
    inicial pela máquina, usada na rodada; suspensos saem dela depois da
    rodada; escalação inválida impede a rodada (rev. 34).
  - **3f.3b (concluída):** tela do plantel do modelo aprovado: cabeçalho
    com divisão, posição, moral e próximo jogo; tática com as 8 formações;
    batedor; resumo; titulares, banco e fora com os botões T/B/–; e
    "Confirmar e jogar rodada" com o placar. As abas de classificação e
    artilheiros, a ficha do jogador e o bloco de treinadores em rede ficam
    para a 3f.5 e a Etapa 5.
  - **3f.4a (concluída):** rodada ao vivo no servidor (ADR 0010, migração
    V13): relógio pelo tempo, velocidades, pausa só na divisão do usuário,
    substituição com a partida pausada, partidas refeitas pela semente e
    gravação no fim uma vez só. As sementes das partidas passam a ser
    sorteadas antes das suspensões.
  - **3f.4b (concluída):** quadro da rodada (modelo 1a), atualizado a cada
    1 s: divisões na ordem em que jogam, minuto, placares, expulsões e
    último lance; velocidade lenta, normal e rápida só na divisão do
    usuário. "Confirmar e jogar rodada" abre o quadro, e no fim a tela volta
    ao plantel com o placar. Ficam para depois a coluna de público (sem
    estádio no domínio) e, na 3f.4c, o som e o clique no clube.
  - **3f.4c (concluída):** clicar num clube da divisão do usuário pausa a
    partida e abre a janela do jogo sobre o quadro (modelos 1d e 1e):
    lances em ordem e, no próprio clube, em campo e banco, a formação
    resultante da troca, as substituições feitas e "Substituir"; nos
    outros jogos, os 11 de cada lado. "Continuar" ou fechar retoma. Som
    dos gols ligado ou desligado no quadro, mantido para as próximas
    rodadas (migração V14). Fica fora o cabeçalho com estádio e público
    (sem estádio no domínio). As caixas "Cartões vermelhos" e "Lesões"
    continuam em aberto.
  - **3f.5 (concluída):** abas Classificação (por divisão, clube do
    usuário em destaque, rebaixados marcados), Artilheiros da temporada
    (por divisão) e de todos os tempos (empatados dividem a posição, 20
    linhas), ficha do jogador, calendário do clube (rev. 36) e fim de
    temporada na tela (o usuário só confirma; a tela envia o número da
    temporada e o clique repetido é recusado). Usuário com clube na fila é
    demitido e assume outro clube (rev. 36).
  - plantel e tática;
  - partidas simultâneas da divisão (§10.2);
  - classificação;
  - velocidade e som.

### Etapa 4 — Economia
- Depende das tabelas a definir (§9.1). Antes de implementar, propor as
  tabelas e validá-las por simulação de temporadas.
- **Definido (rev. 37):** começa pelo **caixa da rodada**: saldo do clube,
  estádio (nome e capacidade), público e bilheteria em casa, despesa de
  jogo fora, salários por rodada e um extrato rastreável por rodada.
  Mercado, leilões, empréstimos, contratos e patrocínios vêm depois. As
  tabelas de valores são propostas pelo agente e aprovadas pelo usuário
  depois da simulação.
- Contratos, salários, bilheteria, estádio, moedas, empréstimos, juros,
  mercado, leilões e patrocínios.

### Etapa 5 — Multiplayer
- **Definido (rev. 29):** só começa depois que o jogo individual estiver
  jogável e testado pelo usuário (mecânica e bugs).
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
