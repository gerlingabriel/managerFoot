# Referência: tela de jogos da rodada (Elifoot 98)

Descrição de um print enviado pelo usuário em 2026-10-27. A imagem não fica no
repositório, que é público, porque o jogo original tem direitos autorais. Serve
de base para o mockup da Etapa 3, que precisa ser aprovado antes de qualquer
implementação.

## Estrutura observada

- **Janela:** título com a jornada e o ano ("4ª Jornada - 2012"). Há um relógio
  em forma de pizza no canto superior direito, que mostra o tempo decorrido da
  partida.
- **Fundo:** verde, com faixas horizontais em tons alternados, lembrando o
  gramado.
- **Divisões empilhadas** na mesma tela ("1ª Divisão", "2ª Divisão"...). Cada
  uma tem um título e uma linha separadora.
- **Cada jogo é uma linha com:**
  1. o **público** (número) à esquerda;
  2. o **mandante**: nome em maiúsculas sobre uma faixa com as cores do clube
     (fundo e letra);
  3. o **placar**: gols do mandante e do visitante em duas caixas pequenas;
  4. o **visitante**: nome sobre a faixa com as cores do clube;
  5. o **último gol**: autor e minuto, por exemplo "Saulo 60'". Gol de pênalti
     aparece como "Jorge Couto 60' (pen.)".
  6. **Expulsão**: um ícone vermelho de cartão/cruz ao lado do time que teve
     jogador expulso.

## Pontos para modernizar (CLAUDE.md §9; a decidir no mockup)

- Legibilidade das cores dos clubes. Algumas combinações de fundo e letra têm
  pouco contraste.
- Destacar o jogo do usuário.
- Ordem dos jogos dentro da divisão. A SPEC §10.2 sugere ordenar pela
  classificação.
- Mostrar o estádio e a lista completa de gols e expulsões ao clicar em um
  jogo. Nesse caso aparecem só os 11 titulares de cada time (SPEC §10.3).
