# CLAUDE.md --- Diretrizes do projeto

## 1. Objetivo do projeto

Desenvolver um simulador de gestão e partidas de futebol inspirado nas
mecânicas clássicas do Elifoot 98, preservando sua simplicidade e
identidade visual como referência, mas com melhorias de interface, novas
regras configuráveis e suporte a partidas compartilhadas em rede.

O repositório https://github.com/trsthales/desvendando-elifoot-98 é uma
referência para investigação das mecânicas do Elifoot 98. Não presumir
que toda regra descrita neste projeto existe no código de referência:
inspecionar e documentar o comportamento encontrado antes de
reutilizá-lo.

## 2. Fonte de verdade e prioridade

Ao trabalhar no projeto, respeite esta ordem:

1.  Instruções explícitas do usuário na conversa atual.
2.  Requisitos aprovados em `SPEC.md` e em especificações
    complementares.
3.  Decisões de arquitetura documentadas no repositório
    (`docs/decisions/`).
4.  Código e testes existentes.
5.  Hipóteses técnicas do agente, que devem ser identificadas como
    hipóteses.

Não transforme sugestões, exemplos numéricos ou ideias ainda em
avaliação em regras definitivas. A `SPEC.md` deve distinguir: -
**Definido:** requisito aprovado. - **Provisório:** proposta que pode
ser ajustada. - **Em aberto:** decisão que exige discussão, pesquisa ou
simulação.

Quando documentos entrarem em conflito, não escolha silenciosamente.
Informe o conflito e proponha uma resolução.

## 3. Forma de trabalho

-   Antes de implementar uma funcionalidade, inspecione a estrutura do
    projeto e os padrões já existentes.
-   Para mudanças relevantes, apresente um plano curto com
    arquivos/sistemas afetados, riscos e testes previstos.
-   Trabalhe em incrementos pequenos e verificáveis. Evite reescrever
    grandes partes do projeto sem necessidade.
-   Não implemente sistemas não solicitados apenas porque parecem úteis.
-   Não invente requisitos para preencher lacunas. Faça perguntas quando
    uma decisão alterar arquitetura, regras centrais, dados persistidos
    ou experiência multiplayer.
-   Para detalhes de baixo risco, escolha a solução mais simples,
    registre a decisão quando ela afetar o futuro e mantenha-a fácil de
    alterar.
-   Ao concluir uma tarefa, informe o que mudou, quais testes foram
    executados, seus resultados e quaisquer limitações conhecidas.
-   Se não for possível executar testes, diga isso claramente. Nunca
    afirme que algo foi testado sem ter sido executado.

## 4. Arquitetura e separação de responsabilidades

Manter, sempre que compatível com a tecnologia escolhida, separação
entre:

-   **Domínio do jogo:** clubes, treinadores, jogadores, contratos,
    finanças, ligas, copas e regras.
-   **Motor de partidas:** simulação determinística ou reproduzível,
    táticas, eventos, gols, cartões, lesões e resultados.
-   **Gestão da temporada:** calendário, classificação, promoção,
    rebaixamento, copas e competições continentais.
-   **Economia:** receitas, despesas, estádio, salários, empréstimos,
    juros, patrocínios e mercado.
-   **Multiplayer:** estado da sessão, participantes, sincronização,
    fases/turnos, pausas e reconexão.
-   **Persistência:** salvar/carregar partidas e validar dados.
-   **Interface:** telas, animações, sons e configurações visuais.

A interface não deve conter regras essenciais de simulação. O motor de
partidas não deve depender de componentes visuais. O multiplayer deve
coordenar o estado compartilhado sem permitir que cada cliente calcule
resultados incompatíveis.

Não adote uma arquitetura excessivamente complexa antes de conhecer a
escala real do projeto. Registre decisões arquiteturais relevantes e
seus motivos.

## 5. Regras para o motor de partidas

-   Separar regras de futebol, parâmetros de balanceamento e
    apresentação visual.
-   Manter pesos, probabilidades e limites importantes configuráveis, em
    vez de espalhá-los pelo código.
-   Evitar aleatoriedade não controlada. Sempre que apropriado, permitir
    usar uma semente aleatória para reproduzir partidas em testes e
    investigar problemas.
-   Criar testes para casos comuns, extremos e regras especiais:
    diferença de força, mando de campo, tática, jogadores com asterisco,
    expulsões, lesões e fim da partida.
-   Usar simulações em lote para avaliar a distribuição dos placares, a
    vantagem de jogar em casa, a influência das táticas e a frequência
    de eventos.
-   Não ajustar probabilidades apenas por intuição quando uma simulação
    puder fornecer evidências.
-   Não exigir que cada partida siga um roteiro fixo. O objetivo é
    produzir distribuições plausíveis, incluindo surpresas raras.
-   Não permitir que uma tática ou atributo torne a vitória garantida.

## 6. Multiplayer e estado compartilhado

-   O estado oficial da temporada e os resultados devem ser consistentes
    para todos os participantes.
-   Distinguir ações privadas de cada treinador (por exemplo, abrir sua
    escalação ou escolher um batedor) dos eventos públicos (por exemplo,
    gol, placar, expulsão e resultado).
-   Definir explicitamente quais ações pausam o relógio compartilhado e
    quais afetam apenas a interface local.
-   Evitar que dois clientes avancem a mesma fase de turno ou processem
    o mesmo resultado mais de uma vez.
-   Tratar desconexões, reconexões, participantes atrasados e abandono
    de sessão como requisitos a especificar antes de considerar o
    multiplayer completo.
-   Não presumir que pausar uma janela local deve pausar o jogo de
    todos: aplicar somente a regra aprovada na especificação.
-   Não expor a outros jogadores escolhas privadas que devam permanecer
    ocultas.

## 7. Dados, compatibilidade e configuração

-   Validar os dados de ligas, clubes, jogadores, regras de competição e
    patrocínios antes de iniciar uma temporada.
-   Fornecer mensagens compreensíveis quando uma configuração for
    inválida, incluindo os dados que faltam.
-   Evitar valores mágicos no código. Colocar limites e parâmetros de
    balanceamento em configuração ou módulos de domínio adequados.
-   Não exigir imagens para dados que podem ser representados por texto,
    como nomes de patrocinadores.
-   Preservar partidas salvas e compatibilidade de dados quando viável.
    Se uma alteração quebrar o formato salvo, documentar e implementar
    uma migração ou explicar a limitação.

## 8. Qualidade e testes

-   Escrever testes unitários para regras de domínio e testes de
    integração para fluxos que atravessem sistemas.
-   Testar cálculos financeiros com valores positivos, negativos,
    limites e falta de saldo.
-   Testar transições de temporada: fim de campeonato,
    promoção/rebaixamento, sorteio de copa e início de nova temporada.
-   Testar que o multiplayer só avance quando as condições definidas
    para a fase forem satisfeitas.
-   Para o motor de partidas, manter testes estatísticos reproduzíveis e
    relatórios de simulação. Evitar testes probabilísticos frágeis que
    falhem aleatoriamente sem uma semente fixa.
-   Não tratar compilação bem-sucedida como prova de que a mecânica está
    correta.
-   Corrigir ou reportar regressões causadas pela mudança.

## 9. Interface e experiência

-   Usar o Elifoot 98 como referência de simplicidade, não como
    obrigação de copiar cada detalhe.
-   Modernizar hierarquia visual, legibilidade, feedback de eventos e
    acessibilidade sem perder a leitura rápida de informações.
-   Permitir configurar a velocidade das partidas e o som dos gols.
-   Mostrar com clareza placar, estádio, público, autores dos gols,
    expulsões e lesões.
-   Não bloquear a implementação de regras de domínio por causa de
    decisões visuais ainda não aprovadas.
-   Imagens de fundo devem ser opcionais e ter requisitos de
    formato/dimensões definidos pela aplicação; não assumir dimensões
    finais antes da decisão de design.

## 10. Segurança e confiabilidade

-   Validar entradas recebidas de outros participantes e arquivos de
    dados.
-   Não confiar em valores enviados por um cliente como fonte definitiva
    de dinheiro, elenco, resultado ou estado da temporada.
-   Evitar duplicação de transações financeiras, eventos de partida e
    avanços de turno.
-   Não registrar segredos ou informações sensíveis em logs.
-   Seguir as práticas de segurança adequadas à tecnologia e ao modelo
    de rede escolhidos.

## 11. Documentação e decisões pendentes

-   Atualizar a documentação quando uma regra aprovada mudar.
-   Manter as decisões de balanceamento separadas da descrição funcional
    quando isso facilitar simulações.
-   Registrar decisões importantes com contexto, opções consideradas e
    consequência.
-   Antes de implementar uma regra marcada como **Em aberto**, pedir
    decisão do usuário se ela tiver impacto relevante no equilíbrio,
    arquitetura, persistência ou multiplayer.
-   Se o usuário pedir para implementar uma funcionalidade que depende
    de uma regra em aberto, explicar a dependência e oferecer uma
    proposta explícita para aprovação.

## 12. Primeiro passo recomendado

Antes de desenvolver o jogo completo:

1.  Inspecionar o repositório e a referência do Elifoot 98.
2.  Definir tecnologia, plataforma e modelo de multiplayer.
3.  Validar um protótipo do motor de partidas com simulações
    automatizadas.
4.  Validar a estrutura de uma liga e a transição de turnos.
5.  Só então expandir economia, mercado, competições e interface.

A ordem exata deve ser ajustada à estrutura real do projeto e às
decisões do usuário. A ordem vigente está na `SPEC.md` §15
(`docs/decisions/0001-ordem-de-implementacao.md`), e o que foi confirmado
na referência está em `docs/referencia-elifoot98.md`.


## 13. Stack técnica definida

A stack inicial do projeto é:

- **Backend:** Java 21 + Spring Boot.
- **Banco de dados:** PostgreSQL.
- **Migrações de banco:** Flyway. Alterações estruturais devem ser versionadas por migrações; não depender de criação automática de schema em produção.
- **Testes:** JUnit 5; Testcontainers para testes de integração que precisem de PostgreSQL real.
- **Interface inicial:** Thymeleaf + HTMX, salvo decisão posterior fundamentada de usar React.
- **Atualização de estado/rodada:** começar com polling simples. Considerar SSE quando houver necessidade real de notificações mais imediatas. WebSocket não é requisito inicial.
- **MCP:** opcional. Um servidor MCP para PostgreSQL ou GitHub pode ser útil, mas não é pré-requisito para o desenvolvimento. Só configurar ferramentas com escopo de acesso necessário e revisar permissões antes de permitir alterações.

### Diretrizes para essa stack

- Não introduzir Supabase como serviço obrigatório: PostgreSQL será gerenciado pela própria aplicação/infraestrutura escolhida.
- Não adicionar React, WebSocket, Redis, mensageria ou outros serviços sem uma necessidade concreta documentada.
- Preferir Thymeleaf + HTMX para as telas de gestão e navegação inicialmente. Reavaliar React apenas se a interface da partida exigir interações complexas no cliente que se tornem difíceis de manter com renderização no servidor.
- Para polling, definir intervalo razoável, evitar consultas pesadas e não usar o polling para decidir resultados oficiais. O backend continua sendo a fonte de verdade.
- Usar SSE se for necessário enviar eventos do servidor ao navegador com menor latência e sem a complexidade bidirecional de WebSocket. SSE não substitui os endpoints de comando do usuário.
- Utilizar Flyway desde o início. Migrações aplicadas não devem ser editadas retroativamente em ambientes compartilhados; criar uma nova migração corretiva.
- Os testes que validam SQL, constraints, índices, consultas ou migrações devem usar PostgreSQL real por meio de Testcontainers, quando aplicável, em vez de depender exclusivamente de H2.
- Separar testes unitários rápidos dos testes de integração que iniciam containers. Documentar como executar ambos.
- Se o repositório já tiver uma ferramenta de build, respeitá-la. Em um repositório novo, Maven pode ser usado como padrão simples, mas essa escolha deve ser registrada.

## 14. Skills de projeto

Skills são procedimentos reutilizáveis e focados, não substitutos de requisitos nem permissões adicionais. Skills específicas do projeto devem ficar em `.claude/skills/<nome-da-skill>/SKILL.md`.

Skills recomendadas para este projeto:
- `implement-feature`: transformar um requisito aprovado em plano, implementação incremental e testes.
- `spring-boot-feature`: criar ou alterar funcionalidades Spring Boot seguindo as camadas e padrões do projeto.
- `flyway-migration`: planejar, criar e verificar migrações versionadas do PostgreSQL.
- `integration-tests`: escrever testes de integração com JUnit 5 e Testcontainers.
- `match-engine-simulation`: executar simulações reproduzíveis do motor de partidas e analisar distribuição de placares/eventos.
- `review-changes`: revisar alterações em busca de bugs, regressões, problemas de segurança e testes ausentes.

Não criar dezenas de skills no início. Começar com as que resolvem tarefas recorrentes, avaliar se são úteis e ajustar suas instruções com base na experiência real. Skills de terceiros devem ser revisadas antes de uso; não executar scripts ou conceder acesso a dados sem entender o que fazem.

## 15. Critérios técnicos mínimos

- Cada regra de domínio importante deve ser testável sem precisar iniciar a interface web.
- Controllers devem coordenar requisições e respostas; regras de negócio devem residir em serviços/domínio, não em templates ou handlers HTMX.
- Persistência deve ser acessada por componentes dedicados, com transações explícitas nas operações que atualizam a temporada ou as finanças.
- Operações como avançar uma rodada, concluir uma partida, pagar salários e vender um jogador devem ser protegidas contra processamento duplicado.
- Aplicar validação de entrada no backend; validação de interface não é suficiente.
- Usar logs estruturados e mensagens de erro úteis, sem registrar senhas, tokens ou outros segredos.
- Manter segredos e credenciais fora do repositório, usando variáveis de ambiente ou mecanismo apropriado ao ambiente.
- Documentar comandos para executar a aplicação, testes unitários, testes de integração e migrações.
