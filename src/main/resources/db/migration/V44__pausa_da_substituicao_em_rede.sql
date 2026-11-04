-- Jogo em rede, parte 5c (SPEC §3.3 itens 3 e 8, rev. 90 e 92; modelo 18f): a substituição para o jogo
-- de todos por até 60 s. Quem parou e até quando o relógio fica parado; nulos no jogo individual e com o
-- relógio andando.
ALTER TABLE jogo_rodada_ao_vivo
    ADD COLUMN pausada_por BIGINT REFERENCES usuario (id),
    ADD COLUMN pausa_ate   TIMESTAMPTZ,
    ADD CONSTRAINT ck_jogo_rodada_ao_vivo_pausa
        CHECK ((pausada_por IS NULL) = (pausa_ate IS NULL) AND (pausa_ate IS NULL OR andando_desde IS NULL));
