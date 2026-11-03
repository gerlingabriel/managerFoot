# 0020 — Sala do jogo em rede

- **Status:** Aceita (SPEC §3.3, rev. 92; modelos 18a a 18d aprovados)
- **Data:** 2026-11-03

## Contexto

- O jogo em rede (Etapa 5) começa pela sala: o administrador cria o jogo, convida por link e começa a
  temporada quando todos estão prontos (SPEC §3.3, rev. 90). Entrada só antes da 1ª rodada.
- Todo o jogo individual já supõe `jogo_treinador` com o clube de cada usuário; vários pontos supõem um
  usuário só (OK da rodada, leilões, decisões pendentes). Eles mudam na parte 5b.
- No modo escolha, quem entra fica na sala ainda sem clube.
- Quem pega um clube da máquina depois que o jogo foi criado pegaria um clube com patrocínio de máquina já
  pago e jogadores anunciados à venda.

## Opções consideradas

1. **O jogo em rede é um jogo comum do administrador, com a sala em tabelas próprias** (escolhida). A
   1ª temporada é sorteada na criação, como no individual; patrocínio, salários e jogadores à venda ficam
   para quando a temporada começa.
2. **Criar o jogo só ao começar a temporada.** O quadro de escolha teria de ser o do molde para todos, e a
   sala guardaria clubes do molde; quem entra não veria o plantel do clube dele antes de começar.
3. **Trocar o patrocínio da máquina pelo do usuário a cada entrada e saída.** Mexe no caixa já lançado a
   cada pedido da sala.

## Decisão

- Migração V41: `jogo_rede` (modo, quando começou), `jogo_rede_convite` (link, validade, trocado) e
  `jogo_rede_participante` (entrada e última consulta). O clube continua em `jogo_treinador`.
- `SalaRepositorio` faz abrir, convite, entrar, escolher, tirar e começar. Tudo trava a linha do jogo; a
  constraint de clube único em `jogo_treinador` segura dois pedidos ao mesmo tempo.
- O token do convite fica guardado como foi gerado (o administrador volta a vê-lo na sala para copiar),
  diferente do link de cadastro, que guarda só o hash. O link só deixa entrar numa sala antes da 1ª rodada,
  vale 7 dias e para de valer quando o administrador gera outro.
- "Sem conexão" é o navegador sem consultar a sala por 30 s (`managerfoot.rede.sem-conexao-apos`). A sala
  consulta a cada 3 s.
- Enquanto o jogo está na sala, nenhuma rodada nem leilão começa (`RodadaController` e
  `RodadaAoVivoRepositorio`). O plantel pode ser visto.
- O link aberto sem conta mostra o formulário de entrar, que leva o token num campo e volta ao convite
  depois de entrar. O token só vira endereço se tiver o formato de token.
- As tabelas da sala ficam fora do arquivo de save: jogo em rede não baixa arquivo.

## Consequências

- Antes de começar, o jogo em rede não tem caixa aberto nem patrocínio; as telas de finanças mostram o jogo
  sem lançamentos.
- Depois de começar, o jogo em rede ainda segue as regras de um usuário só até a parte 5b (OK de todos).
- Presença conta só na sala por enquanto; as outras telas passam a contar na parte 5b.
