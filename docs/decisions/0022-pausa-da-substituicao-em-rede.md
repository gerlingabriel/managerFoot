# 0022 — Pausa de 60 s da substituição no jogo em rede

- **Status:** Aceita (SPEC §3.3 itens 3 e 8, rev. 90 e 92; parte 5c; modelo 18f)
- **Data:** 2026-11-04

## Contexto

- No jogo em rede, a rodada ao vivo é uma só para todos: o relógio é calculado do tempo passado
  (ADR 0010) e anda na velocidade do administrador (rev. 96).
- A substituição para o jogo de todos, com aviso de quem substitui e limite de 60 s. Ver o clube de
  outro não para (rev. 90). Ao fim dos 60 s, a janela de quem substitui fecha e o que foi trocado vale
  (rev. 92).
- No jogo individual, clicar em qualquer clube pausa, sem limite, e o quadro para de consultar o
  servidor com o jogo parado.

## Opções consideradas

1. **Gravar quem parou e até quando; o fim da pausa é calculado na leitura** (escolhida). Quando o
   instante passa de `pausa_ate`, a rodada é tratada como andando desde `pausa_ate`. A primeira
   consulta do quadro grava isso. Não há agendador.
2. Uma tarefa no servidor que solta a pausa aos 60 s. Precisa de agendador e de cuidado com reinício,
   sem ganho sobre a opção 1.
3. O navegador de quem parou manda "continuar" aos 60 s. Se ele fechar a tela ou cair, o jogo fica parado
   para todos.

## Decisão

- Migração V44: `jogo_rodada_ao_vivo.pausada_por` e `pausa_ate`, nulos no jogo individual e com o
  relógio andando.
- `pausar` recebe o jogo clicado. Em rede, só o próprio clube com a partida em andamento para, e marca
  `pausa_ate = agora + 60 s` (`managerfoot.rede.pausa-da-substituicao`). `continuar` só vale para quem
  parou. Parar e voltar a andar passam pela trava do jogo.
- O quadro dos outros segue consultando a cada 1 s e mostra quem parou e os segundos. A janela de quem
  substitui não consulta o quadro (apagaria a escolha de quem sai e entra). Só a contagem consulta
  `/rodada/pausa`, que manda voltar ao quadro quando o jogo anda.
- **Definido pelo usuário (rev. 101):** a lesão num clube de treinador também para todos por 60 s, com a
  janela aberta para o treinador dele; no fim do tempo a janela fecha e, sem troca, a máquina troca.
- **Provisório:** com o jogo parado por um, os outros podem substituir no próprio jogo, dentro do mesmo
  tempo. Não há limite de pausas.

## Consequências

- Um treinador pode parar o jogo de novo logo depois que ele volta. Se isso atrapalhar, cabe um limite
  por partida.
- Duas lesões de treinadores no mesmo minuto, em jogos diferentes, param uma vez só. A segunda lesão fica
  com a troca automática.
