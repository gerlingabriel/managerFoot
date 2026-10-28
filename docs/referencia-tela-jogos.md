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

## Janela ao clicar num jogo (captura vista em 2026-10-28)

Captura pública do Elifoot 98 na página de download do baixesoft.com (7ª
imagem), vista em 2026-10-28. A imagem não fica no repositório.

- Janela pequena sobre o quadro da rodada, com título "MANDANTE, gols -
  VISITANTE, gols".
- Lista de lances com placar parcial, autor e minuto (ex.: "1:0 Ney Santos
  38'"), e o árbitro ao lado.
- Duas caixas de marcar: "Cartões vermelhos" e "Lesões". A função delas não
  aparece na imagem.
- Botão "Continuar".
- No clube do usuário, um painel com os titulares (posição, nome, força) e
  o banco, e o botão "Substituir".

A adaptação aprovada está na SPEC §10.5 (rev. 33).
