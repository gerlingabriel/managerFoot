-- Lesões (SPEC §7.5, rev. 69): jogos fora a cumprir, como a suspensão, e o evento público da lesão.
-- As lesões do clube na temporada (limite de 2) são contadas pelos eventos, sem coluna própria.
ALTER TABLE jogo_jogador
    ADD COLUMN lesao INT NOT NULL DEFAULT 0 CHECK (lesao >= 0);

ALTER TABLE jogo_evento
    DROP CONSTRAINT jogo_evento_tipo_check,
    ADD CONSTRAINT jogo_evento_tipo_check
        CHECK (tipo IN ('GOL', 'CARTAO_AMARELO', 'SEGUNDO_AMARELO', 'CARTAO_VERMELHO', 'LESAO'));

-- Lesão no clube do usuário pausa a rodada ao vivo (rev. 69): progresso da última lesão que já
-- pausou, para não pausar de novo no mesmo lance.
ALTER TABLE jogo_rodada_ao_vivo
    ADD COLUMN lesao_ate DOUBLE PRECISION NOT NULL DEFAULT 0 CHECK (lesao_ate >= 0);
