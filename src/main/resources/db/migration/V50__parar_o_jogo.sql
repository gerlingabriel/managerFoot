-- Caixas "Lesões" e "Cartões vermelhos" da janela do jogo (SPEC §10.5, rev. 132): se a rodada para sozinha quando o
-- clube do treinador tem um lesionado ou um expulso. A lesão parava sempre (marcada); o vermelho nunca (desmarcada).
ALTER TABLE jogo_treinador
    ADD COLUMN parar_na_lesao    boolean NOT NULL DEFAULT true,
    ADD COLUMN parar_no_vermelho boolean NOT NULL DEFAULT false;
