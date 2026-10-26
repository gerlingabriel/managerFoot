# 0004 — Servidor como autoridade do estado e do relógio

- **Status:** Aceita
- **Data:** 2026-10-26

## Contexto

- O jogo é somente web (SPEC §14, item 1).
- No multiplayer, só o administrador controla a velocidade das partidas
  (SPEC §10.4).
- Se um jogador cair durante a partida, ela continua até o fim (SPEC §11.4).
- O CLAUDE.md §6 e §10 exige resultados consistentes e proíbe confiar em
  valores enviados pelo cliente.

## Opções consideradas

1. **Servidor autoritativo:** o backend sorteia, avança o relógio e
   persiste os eventos. O navegador só exibe e envia comandos.
2. **Host de sessão:** o navegador do administrador simula e repassa aos
   demais.
3. **Simulação no cliente com semente compartilhada:** cada navegador
   calcula o mesmo resultado.

## Decisão

Opção 1.
- O relógio de cada partida e os eventos (gol, cartão, lesão) são gerados no
  servidor e gravados.
- Os navegadores consultam por polling a partir do último evento recebido,
  conforme o CLAUDE.md §13.
- No modo individual, o mesmo motor roda no servidor e a velocidade é
  escolhida pelo usuário.

## Consequências

- **Opção 2:** a queda do host derrubaria a sessão, e o host poderia
  manipular resultados.
- **Opção 3:** exige motor idêntico em JavaScript e ainda permite trapaça.
- **Opção 1:**
  - funciona com a regra de desconexão aprovada: a partida segue no
    servidor;
  - o servidor carrega o custo de simular as partidas exibidas minuto a
    minuto;
  - os comandos de avanço de fase precisam ser idempotentes.
- Salvar o jogo "na máquina pessoal" fica limitado a exportar/importar o
  estado (SPEC §3.5). Ver as opções em aberto lá.
