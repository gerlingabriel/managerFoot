# 0010 — Rodada ao vivo: relógio, pausa e substituições

- **Status:** Aceita (aprovada pelo usuário em 2026-10-28)
- **Data:** 2026-10-28

## Contexto

- A 3f.4 troca o "jogar a rodada de uma vez" pelo minuto a minuto (SPEC
  §10.2, §11.2, rev. 13 a 16). As divisões jogam uma depois da outra, da
  mais baixa para a 1ª. A divisão com usuário anda na velocidade escolhida;
  as outras aparecem sempre rápido.
- Clicar num clube pausa a partida e abre a janela do jogo. No próprio
  clube há substituição (rev. 30 e 33). Por isso o servidor não pode
  calcular a rodada inteira de uma vez e só mostrar depois (§10.3).
- O servidor é a autoridade do relógio e dos eventos (ADR 0004). O
  navegador consulta por polling (CLAUDE.md §13).
- O motor já serve: `Partida` avança um minuto por vez, aceita
  `substituir` entre os minutos e é reproduzível pela semente.
- O jogo é individual por enquanto. A rede vem na Etapa 5, e a pausa em
  rede está em aberto (§11.3).

## Opções consideradas

**1. Quem anda o relógio**
- (a) **Relógio pelo tempo no servidor.** Ao começar, o servidor grava a
  hora e a velocidade. O minuto atual é calculado do tempo passado, menos
  as pausas. Não há tarefa rodando em segundo plano.
- (b) O navegador pede "avance um minuto" a cada tique. É simples, mas o
  relógio fica com o cliente, o que não serve para a rede.
- (c) Uma tarefa no servidor avança os minutos e grava cada um. Exige
  agendador, cuidado com reinício e mais escrita no banco.

**2. Como guardar a partida em andamento**
- (a) **Repetir a partida a partir da semente.** Grava-se só a semente de
  cada partida e as substituições feitas (minuto, quem sai, quem entra).
  Para saber o estado no minuto *m*, o servidor refaz a partida até *m*,
  aplicando as substituições no minuto em que foram feitas. Custa cerca de
  0,1 ms por partida. Sobrevive a reinício do servidor.
- (b) Manter os objetos `Partida` na memória do servidor. Somem num
  reinício ou numa segunda instância.
- (c) Gravar cada minuto de cada partida. Muita escrita, sem ganho.

**3. Atualização da tela**
- (a) **Polling com HTMX a cada 1 s (provisório)**, que pede o quadro já
  com o minuto atual.
- (b) SSE. Fica para quando o polling não bastar (CLAUDE.md §13).

## Decisão

1a, 2a e 3a, aprovadas pelo usuário, com estes valores (SPEC rev. 35):
- **Velocidades da divisão com usuário (provisórias):** lenta 1 s por
  minuto, normal 0,5 s e rápida 0,2 s. Começa em normal, e a escolha fica
  gravada para as próximas rodadas.
- **Divisão sem usuário:** 0,05 s por minuto, cerca de 5 s por divisão.
- **Sair da tela no meio:** a partida continua no servidor.

- **Estado gravado da rodada ao vivo** (nova migração):
  - rodada e temporada;
  - velocidade (milissegundos por minuto de jogo);
  - tempo de relógio já corrido;
  - momento em que o relógio voltou a andar (nulo = pausado);
  - substituições feitas.
  A semente de cada partida não é gravada: ela sai da semente da temporada
  e do número da rodada, como hoje.
- **Escalação durante a rodada:** o plantel não aceita mudanças enquanto a
  rodada está ao vivo. Assim, refazer a partida dá sempre o mesmo jogo.
- **Linha do tempo:** as divisões em ordem decrescente de prioridade. Cada
  uma dura 90 minutos de jogo: na velocidade do usuário se tem clube dele,
  e na velocidade rápida fixa se não tem. O progresso é gravado em minutos
  de jogo. Ao pausar ou trocar a velocidade, o progresso até ali é gravado
  e o relógio recomeça a contar dali.
- **Pausa:**
  - clicar num clube durante a divisão com usuário grava o tempo corrido e
    para o relógio;
  - "Continuar" volta a andar;
  - pausa fora da divisão do usuário é ignorada.
- **Substituição:** só com a partida pausada. O servidor refaz a partida
  até o minuto atual, valida a troca pelo próprio `Partida.substituir` e
  grava a troca com esse minuto.
- **Fim da rodada:**
  - quando o tempo passa do fim da 1ª divisão, o primeiro pedido grava os
    resultados pelo mesmo caminho de hoje (placares, gols, cartões,
    suspensões, moral, suspensos fora da escalação) e apaga a rodada ao
    vivo;
  - a gravação usa a trava do jogo e a checagem de rodada esperada já
    existentes, então dois pedidos simultâneos gravam uma vez só.
- **Divisões sem usuário:** também jogam pelo mesmo caminho. A semente é a
  mesma e não há substituições, então o resultado mostrado é o mesmo que
  será gravado.

## Consequência obrigatória: sementes da rodada

- **Hoje:** a semente de cada partida e o sorteio das suspensões saem do
  mesmo gerador, intercalados. Uma substituição que muda os cartões de uma
  partida mudaria a semente das partidas seguintes, e jogos já exibidos
  mudariam de placar.
- **Correção:** primeiro se sorteiam as sementes de todas as partidas da
  rodada; as suspensões usam um gerador separado.
- **Efeito:** os resultados das rodadas futuras deixam de ser iguais aos
  que a regra antiga daria com a mesma semente. Rodadas já gravadas não
  mudam. Nenhuma migração de dados é necessária.

## Outras consequências

- O "Confirmar e jogar rodada" da rev. 34 passa a abrir a rodada ao vivo.
- Sair da tela no meio da rodada não perde nada. Ao voltar, a tela mostra
  o minuto calculado. Com o relógio andando, a partida segue sem o
  usuário. Na rede isso combina com a regra de desconexão (a partida segue
  no servidor).
- Para a rede (Etapa 5), a mesma estrutura serve. Falta decidir quem pode
  pausar (§11.3).
