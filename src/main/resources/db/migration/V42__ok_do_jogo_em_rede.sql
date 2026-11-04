-- OK de todos no jogo em rede (SPEC §3.3 itens 4 a 6, rev. 90 e 92; parte 5b, docs/decisions/0021).
--   jogo_rede.momento_leiloes: em que passo estão os leilões da rodada ('t3/r6/lances' ou
--     't3/r6/leiloes'); nulo ou de outra rodada é o passo da tática.
--   jogo_rede_participante.ok_momento: o momento em que o treinador deu OK; vale até o jogo avançar,
--     quando todos os OK são apagados. Com OK dado, a escalação fica travada.
ALTER TABLE jogo_rede ADD COLUMN momento_leiloes TEXT;

ALTER TABLE jogo_rede_participante
    ADD COLUMN ok_momento TEXT,
    ADD COLUMN ok_em      TIMESTAMPTZ,
    ADD CONSTRAINT ck_jogo_rede_participante_ok CHECK ((ok_momento IS NULL) = (ok_em IS NULL));

-- Lances secretos dos treinadores, guardados até todos darem lance; os leilões são resolvidos de uma
-- vez e os lances passam para jogo_leilao_lance.
CREATE TABLE jogo_rede_lance (
    leilao_id BIGINT NOT NULL REFERENCES jogo_leilao (id) ON DELETE CASCADE,
    clube_id  BIGINT NOT NULL REFERENCES jogo_clube (id) ON DELETE CASCADE,
    valor     BIGINT NOT NULL CHECK (valor > 0),
    PRIMARY KEY (leilao_id, clube_id)
);
